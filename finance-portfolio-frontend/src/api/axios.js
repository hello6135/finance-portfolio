import axios from 'axios';
import authStore from './authStore';

// 1. axios 인스턴스 생성
const axiosInstance = axios.create({
    // 환경 변수에서 기본 주소를 가져옵니다.
    baseURL: '/api',
    // 요청 타임아웃 설정 (5초)
    timeout: 5000,
    // CSRF 방지용 토큰
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
    },
});

// ── 요청 인터셉터: Access Token 자동 첨부 ──────────────────
axiosInstance.interceptors.request.use(
    (config) => {
        const token = authStore.getToken();
        if (token) {
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

// ── 응답 인터셉터: 토큰 만료 시 자동 재발급 ───────────────
axiosInstance.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // Silent Refresh 실패(비로그인 상태)는 조용히 넘깁니다
        if (originalRequest.url === '/auth/reissue') {
            throw Promise.reject(error);
        }

        // ACCESS_TOKEN_EXPIRED 에러이고 재시도 안 한 요청이면 재발급 시도
        const isExpired =
            error.response?.status === 401 &&
            error.response?.data?.error === 'ACCESS_TOKEN_EXPIRED' &&
            !originalRequest._retry;

        if (!isExpired) {
            return Promise.reject(error);
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

        try {
            // Refresh Token은 HttpOnly 쿠키로 자동 전송됩니다
            const response = await axiosInstance.post('/auth/reissue');
            const newToken = response.headers['authorization']?.replace('Bearer ', '');

            if (!newToken) throw new Error('재발급된 토큰이 없습니다.');

            authStore.setToken(newToken);
            processPendingQueue(null, newToken);

            originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
            return axiosInstance(originalRequest);

        } catch (reissueError) {
            // 재발급 실패 = Refresh Token도 만료 → 강제 로그아웃
            authStore.clearToken();
            processPendingQueue(reissueError);
            // 로그인 페이지로 이동 (router를 여기서 import하면 순환참조 위험 → window 사용)
            globalThis.location.href = '/login';
            throw Promise.reject(reissueError);
        } finally {
            isRefreshing = false;
        }
    }
);

export default axiosInstance;