import axios from 'axios';

// 1. axios 인스턴스 생성
const axiosInstance = axios.create({
    // 환경 변수에서 기본 주소를 가져옵니다.
    baseURL: '/api',
    // 요청 타임아웃 설정 (5초)
    timeout: 5000,
    // CSRF 방지용 토큰
    withCredentials: true,
    xsrfCookieName: 'XSRF-TOKEN',
    xsrfHeaderName: 'X-XSRF-TOKEN',

    headers: {
        'Content-Type': 'application/json',
    },
});

export default axiosInstance;