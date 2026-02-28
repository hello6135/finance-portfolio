import axios from 'axios';

// 1. axios 인스턴스 생성
const axiosInstance = axios.create({
    // 환경 변수에서 기본 주소를 가져옵니다.
    baseURL: import.meta.env.VITE_API_BASE_URL,
    // 요청 타임아웃 설정 (5초)
    timeout: 5000,
    headers: {
        'Content-Type': 'application/json',
    },
});

export default axiosInstance;