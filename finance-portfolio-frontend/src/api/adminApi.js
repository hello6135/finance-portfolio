import axiosInstance from './axios';

// 대시보드 일람
export const getDashboardSummary = async () => {
    const response = await axiosInstance.get('/admin/summary');
    return response.data; // AdminResponseDto 가 들어옴
};

// AWS 상태 체크
export const getAwsStatus = async () => {
    const response = await axiosInstance.get('/admin/awsHealth');
    return response.data; // s3와 ec2 상태가 담긴 Map 이 들어옴
};