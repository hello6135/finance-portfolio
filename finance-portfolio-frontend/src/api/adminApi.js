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

// 전체 회원 페이징 조회
export const getAdminMembers = async (page = 0, size = 10) => {
    const response = await axiosInstance.get('/admin/members', {
        params: { page, size }
    });
    return response.data; // Page<MemberAdminResponseDto>가 들어옴
};

// 회원 정지 및 해제 처리
export const toggleMemberBan = async (memberId, shouldBan) => {
    const response = await axiosInstance.patch(`/admin/members/${memberId}/ban`, null, {
        params: { status: shouldBan }
    });
    return response.status; // 244 No Content 가 들어옴
};

// 회원 임시 잠금 수동 해제
export const unlockMemberAccount = async (memberId) => {
    const response = await axiosInstance.post(`/admin/members/${memberId}/unlock`);
    return response.status; // 244 No Content 가 들어옴
};