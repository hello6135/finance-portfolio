import axios from 'axios';
import authStore from '../store/authStore';
import { history } from '../utils/history';

// 1. axios 인스턴스 생성
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

// ── 재발급 중복 호출 방지 플래그 ───────────────────────────
let isRefreshing = false;
let pendingQueue = []; // 재발급 중 들어온 요청들을 대기

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
    authStore.clearToken();
    isRefreshing = false;
    history.push('/login');
};

// 판별함수
const isReissueRequest = (config) => config.url === '/member/reissue';
const isLoginRequest = (config) => config.url.includes('/member/login');
const isTokenExpiredError = (error) =>
    error.response?.status === 401 &&
    error.response?.data?.error === 'ACCESS_TOKEN_EXPIRED' &&
    !error.config._retry;

// 전역 에러 처리(각 코드에 맞게)
const handleGlobalError = (error) => {
    const { status, data, headers } = error.response || {};
    const message = data?.message || '알 수 없는 오류가 발생했습니다.';

    if (isLoginRequest(error.config)) return;

    // 이미지 업로드 실패는 호출한 쪽(어댑터)에서 처리
    if (error.config?.url?.includes('/image/upload')) return;

    switch (status) {
        case 403:
            alert("권한이 없습니다.");
            break;
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
    if (isRefreshing) {
        return new Promise((resolve, reject) => {
            pendingQueue.push({ resolve, reject });
        }).then((token) => {
            originalRequest.headers['Authorization'] = `Bearer ${token}`;
            return axiosInstance(originalRequest);
        });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
        const response = await axiosInstance.post('/member/reissue');
        const authHeader = response.headers['authorization'] || response.headers['Authorization'];
        const newToken = authHeader?.replace('Bearer ', '');

        if (!newToken) throw new Error('No Token');

        authStore.setToken(newToken);
        processPendingQueue(null, newToken);

        originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
        return axiosInstance(originalRequest);
    } catch (reissueError) {
        processPendingQueue(reissueError);
        handleLogout();
        throw reissueError;
    } finally {
        isRefreshing = false;
    }
};

// ── 응답 인터셉터(RESPONSE): 에러 처리, 만료 토큰 재발급 ───────────────
axiosInstance.interceptors.response.use(
    // 정상 response는 response 그대로 전달
    (response) => response,
    // 에러가 난 경우에는 토큰 재발급 절차 진행
    async (error) => {
        const { config } = error;

        // reissue 요청 자체가 실패했을 때
        if (isReissueRequest(config)) {
            handleLogout();
            // 대기 중인 다른 요청들 종료
            processPendingQueue(error);
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