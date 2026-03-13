import axios from 'axios';

axios.defaults.withCredentials = true; // 쿠키 공유 허용
axios.defaults.xsrfCookieName = 'XSRF-TOKEN'; // 쿠키에서 읽을 이름
axios.defaults.xsrfHeaderName = 'X-XSRF-TOKEN'; // 헤더에 넣을 이름

// 1. axios 인스턴스 생성
const axiosInstance = axios.create({
    // 환경 변수에서 기본 주소를 가져옵니다.
    baseURL: '/api',
    // 요청 타임아웃 설정 (5초)
    timeout: 5000,
    headers: {
        'Content-Type': 'application/json',
    },
});

export default axiosInstance;