import axiosInstance from './axios';

// 대시보드 일람
export const getDashboardSummary = async () => {
    const response = await axiosInstance.get('/admin/summary');
    return response.data; // AdminResponseDto 가 들어옴
};