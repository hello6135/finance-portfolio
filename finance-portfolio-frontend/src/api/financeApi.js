import axiosInstance from './axios';

/*
     * 고든 성장 모델 계산
     * dps: 현재 배당금
     * growthRate: 배당 성장률
     * expectedYield: 기대수익률
     */
export const getFairValue = async (requestDto) => {

    // GET 대신 POST 사용, 두 번째 인자로 DTO 전달
    const response = await axiosInstance.post('/fin/fair', requestDto);
    return response.data; // { fairValue: 123.45, message: "..." }
};