import axios from 'axios';
import authStore from '../store/authStore';
import { history } from '../utils/history';

// 순수 axios 인스턴스 (인터셉터X, 현재 토큰 재발급만 담당)
const pureApi = axios.create({
    baseURL: '/api',
    timeout: 5000,
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
    },
});

// 메인 애플리케이션용 axios 인스턴스
const axiosInstance = axios.create({
    // 환경 변수에서 기본 주소를 가져옵니다.
    baseURL: '/api',
    // 요청 타임아웃 설정 (10초)
    timeout: 10000,
    // CSRF 방지용 토큰
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
    },
});

// 동시성 제어 
let pendingQueue = [];
// 전역 상태 관리
let isRefreshing = false;
let isBannedAlertShowing = false;

// 비동기 대기열 처리
const processPendingQueue = (error, token = null) => {
    pendingQueue.forEach(({ resolve, reject }) => {
        if (error) {
            reject(error);
        } else {
            resolve(token);
        }
    });
    pendingQueue = [];
};

// 로그아웃
const handleLogout = () => {
    // 토큰 파기
    authStore.clearToken();
    // 전역 플래그 초기화 
    isRefreshing = false;
    isBannedAlertShowing = false;
    // 동시성 큐 초기화
    pendingQueue = [];
    // 강제 페이지 리다이렉트
    history.push('/login');
};

// 판별함수
const isLoginRequest = (config) => !!config?.url?.includes('/member/login');
const isTokenExpiredError = (error) => {
    const { response, config } = error;
    return (
        response?.status === 401 &&
        response?.data?.error === 'ACCESS_TOKEN_EXPIRED' &&
        !config?._retry
    );
};


// 전역 에러 처리(각 코드에 맞게, 인증 만료 외 스펙 처리)
const handleGlobalError = (error) => {
    const { response, config } = error;
    if (!response || !config) return;

    const { status, data, headers } = response;
    const message = data?.message || '알 수 없는 오류가 발생했습니다.';

    if (isLoginRequest(config)) return;

    // 이미지 업로드 실패는 호출한 쪽(어댑터)에서 처리
    if (config.url?.includes('/image/upload')) return;

    switch (status) {
        case 403: {
            // 정지 유저 분기 처리
            if (data?.error === 'BANNED_USER') {
                if (!isBannedAlertShowing) {
                    isBannedAlertShowing = true;
                    alert("해당 계정은 정지되었습니다. 로그인 페이지로 이동합니다.");

                    handleLogout();
                }
                break;
            }

            // 일반 권한 부족
            if (!isBannedAlertShowing) {
                isBannedAlertShowing = true;
                alert("권한이 없습니다.");
                setTimeout(() => { isBannedAlertShowing = false; }, 1000);
            }
            break;
        }
        case 429: {
            const retryAfter = Number.parseInt(headers?.['retry-after'] || '60', 10);
            history.push('/error?status=429', {
                message: "요청 한도를 초과했습니다.",
                subMessage: `${retryAfter}초 후에 다시 시도할 수 있습니다.`,
                retryAfter
            });
            break;
        }
        case 404:
        case 500:
        case 502:
        case 503:
        case 504:
            history.push(`/error?status=${status}`, { message });
            break;
    }
};

// 토큰 재발급 핵심 로직
const handleTokenRefresh = async (originalRequest) => {
    // 재시도 플래그를 통한 루프 차단(방어)
    if (originalRequest._retry) {
        handleLogout();
        throw new Error('Token refresh looped detected. Forced logout.');
    }

    if (isRefreshing) {
        return new Promise((resolve, reject) => {
            pendingQueue.push({ resolve, reject });
        }).then((token) => {
            if (originalRequest.headers) {
                originalRequest.headers['Authorization'] = `Bearer ${token}`;
            }
            return axiosInstance(originalRequest);
        });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
        // 재인증 요청은 - 인터셉터가 없는 순수 인스턴스(**pureApi**)를 사용하여 호출 (순환 참조 차단)
        const response = await pureApi.post('/member/reissue');

        const authHeader = response.headers['authorization'] || response.headers['Authorization'];
        const newToken = authHeader?.replace('Bearer ', '');

        if (!newToken) throw new Error('No Token in response headers');

        authStore.setToken(newToken);
        processPendingQueue(null, newToken);

        // 실패했던 원본 요청 헤더 갱신 후 재요청
        if (originalRequest.headers) {
            originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
        }
        return axiosInstance(originalRequest);
    } catch (reissueError) {
        // 리프레시 토큰 자체 만료 등으로 재발급 실패 시 동시성 큐 전원 에러처리 + 로그아웃
        processPendingQueue(reissueError);
        handleLogout();
        throw reissueError;
    } finally {
        isRefreshing = false;
    }
};

// ── 요청 인터셉터(REQUEST): Access Token 자동 첨부 ──────────────────
axiosInstance.interceptors.request.use(
    (config) => {
        // reissue 요청일 때는 기존 토큰을 첨부하지 않음
        if (config.url?.includes('/member/reissue')) {
            return config;
        }

        const token = authStore.getToken();
        if (token && token !== 'null' && token !== 'undefined') {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// ── 응답 인터셉터(RESPONSE): 에러 처리, 만료 토큰 재발급 ───────────────
axiosInstance.interceptors.response.use(
    // 정상 response는 response 그대로 전달
    (response) => response,
    // 에러가 난 경우에는 토큰 재발급 절차 진행
    async (error) => {
        const { config } = error;

        if (!config) {
            throw error;
        }

        // 토큰 만료 에러인 경우 재발급 시도
        if (isTokenExpiredError(error)) {
            return handleTokenRefresh(config);
        }

        // 그 외 일반 에러 처리
        handleGlobalError(error);

        throw error;
    }
);

export default axiosInstance;