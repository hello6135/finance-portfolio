import axiosInstance from './axios';

// 회원가입
// status: 200, data: "회원가입이 성공적으로 완료되었습니다."
// status: 400, data: "이미 존재하는 아이디입니다."
// status: 500, data: "서버 이용에 불편을 드려 죄송합니다. 관리자에게 문의하세요."
export const joinMember = async (form) => {
    const response = await axiosInstance.post('/member/join', form);
    return {
        status: response.status,
        message: response.data
    }
};

// 로그인
// status: 200, data: "로그인이 완료되었습니다."
// status: 400, data: "존재하지 않는 회원입니다."
// status: 500, data: "서버 이용에 불편을 드려 죄송합니다. 관리자에게 문의하세요."
export const loginMember = async (form) => {
    const response = await axiosInstance.post('/member/login', form);
    // response에서 엑세스 토큰 추출
    const token = response.headers['authorization']?.replace('Bearer ', '');
    return {
        status: response.status,
        message: response.data,
        token: token
    };
};

// 로그아웃
// status: 200, data: "로그아웃이 완료되었습니다."
// status: 400, data: "존재하지 않는 회원입니다."
// status: 500, data: "서버 이용에 불편을 드려 죄송합니다. 관리자에게 문의하세요."
export const logoutMember = async () => {
    const response = await axiosInstance.post('/member/logout');
    return {
        status: response.status,
        message: response.data
    }
};

// 토큰 재발급
// status: 200, data: "토큰이 재발급되었습니다."
// status: 400, data: "유효하지 않은 Refresh Token입니다."
// status: 400, data: "존재하지 않는 회원입니다."
// status: 400, data: "로그인 상태가 아닙니다."
// status: 400, data: "Refresh Token이 일치하지 않습니다."
export const reissueMember = async () => {
    const response = await axiosInstance.post('/auth/reissue');
    return {
        status: response.status,
        message: response.data,
        token: token
    }
};
