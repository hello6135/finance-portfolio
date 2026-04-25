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
        if (config.url?.includes('/auth/reissue')) {
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

// ── 응답 인터셉터(RESPONSE): 에러 처리, 만료 토큰 재발급 ───────────────
axiosInstance.interceptors.response.use(
    // 정상 response는 response 그대로 전달
    (response) => response,
    // 에러가 난 경우에는 토큰 재발급 절차 진행
    async (error) => {
        const originalRequest = error.config;

        // reissue 요청 자체가 실패했을 때
        if (originalRequest.url === '/auth/reissue') {
            authStore.clearToken();
            isRefreshing = false;
            processPendingQueue(error); // 대기 중인 다른 요청들 종료

            // 리프레시 토큰도 만료된 것이므로 로그인 페이지로 이동
            history.push('/login');
            throw error;
        }

        // 일반 API 에러
        // ACCESS_TOKEN_EXPIRED 에러이고 재시도 안 한 요청이면 재발급 시도
        const isExpired =
            error.response?.status === 401 &&
            error.response?.data?.error === 'ACCESS_TOKEN_EXPIRED' &&
            !originalRequest._retry;

        // 토큰 만료 이외 에러 
        if (!isExpired) {
            const status = error.response?.status || 500;
            const message = error.response?.data?.message || '알 수 없는 오류가 발생했습니다.';

            // 개발 환경에서만 에러 상세 출력
            if (import.meta.env.DEV) {
                console.error(`[API Error] Status: ${status}, Message: ${message}`);
            }

            // 로그인 요청(`/member/login`)에서 발생한 에러는 비지니스 로직상 실패: 에러 페이지로 보내지 않음!
            const isLoginRequest = originalRequest.url.includes('/member/login');

            if (!isLoginRequest && (status === 404 || status >= 500)) {
                // 쿼리 스트링으로 데이터 전달
                history.push(`/error?status=${status}&message=${encodeURIComponent(message)}`);
            }

            throw error;
        }

        // 재발급이 이미 진행 중이면 대기열에 추가
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

        // 실제 엑세스 토큰 재발급 파트
        try {
            // Refresh Token은 HttpOnly 쿠키로 자동 전송(서버가 직접 쏨)
            // 엑세스 토큰 재발급 `axiosInstance.post('/auth/reissue')`
            // status: 200, data: "토큰이 재발급되었습니다."
            // status: 400, data: "유효하지 않은 Refresh Token입니다."
            // status: 400, data: "존재하지 않는 회원입니다."
            // status: 400, data: "로그인 상태가 아닙니다."
            // status: 400, data: "Refresh Token이 일치하지 않습니다."
            const response = await axiosInstance.post('/auth/reissue');
            const authHeader = response.headers['authorization'] || response.headers['Authorization'];
            const newToken = authHeader?.replace('Bearer ', '');


            if (!newToken) throw new Error('재발급된 토큰이 없습니다.');

            authStore.setToken(newToken);
            processPendingQueue(null, newToken);

            originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
            return axiosInstance(originalRequest);

        } catch (reissueError) {
            // 재발급 실패 = Refresh Token도 만료 → 강제 로그아웃
            authStore.clearToken();
            processPendingQueue(reissueError);
            // 로그인 페이지로 이동 
            history.push('/login');
            throw reissueError;
        } finally {
            isRefreshing = false;
        }
    }
);

export default axiosInstance;