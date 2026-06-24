import axiosInstance from './axios';

// status, message만 반환하는 베이스 파서
const parseBaseResponse = (response) => {
    const { status, data } = response;
    return {
        status: status,
        message: data
    }
};
// 베이스에 엑세스 토큰 섞어서 반환
const parseAuthResponse = (response) => {
    const base = parseBaseResponse(response);
    const authHeader = response.headers['authorization'] || response.headers['Authorization'];
    const token = authHeader?.replace('Bearer ', '');

    return {
        ...base,
        token: token
    };
};

// 회원가입
// status: 200, data: "회원가입이 성공적으로 완료되었습니다."
// status: 400, data: "이미 존재하는 아이디입니다."
// status: 500, data: "서버 이용에 불편을 드려 죄송합니다. 관리자에게 문의하세요."
export const joinMember = async (form) => {
    const response = await axiosInstance.post('/member/join', form);
    // 토큰 없음!
    return parseBaseResponse(response);
};

// 로그인(엑세스 토큰 발급)
// status: 200, data: "로그인이 완료되었습니다."
// status: 400, data: "존재하지 않는 회원입니다."
// status: 500, data: "서버 이용에 불편을 드려 죄송합니다. 관리자에게 문의하세요."
export const loginMember = async (form) => {
    const response = await axiosInstance.post('/member/login', form);
    // 토큰 필요!
    return parseAuthResponse(response);
};

// 엑세스 토큰 재발급
// status: 200, data: "토큰이 재발급되었습니다."
// status: 400, data: "유효하지 않은 Refresh Token입니다."
// status: 400, data: "존재하지 않는 회원입니다."
// status: 400, data: "로그인 상태가 아닙니다."
// status: 400, data: "Refresh Token이 일치하지 않습니다."
export const reissueMember = async () => {
    const response = await axiosInstance.post('/member/reissue');
    // 토큰 필요!
    return parseAuthResponse(response);
};


// 로그아웃
// status: 200, data: "로그아웃이 완료되었습니다."
// status: 400, data: "존재하지 않는 회원입니다."
// status: 500, data: "서버 이용에 불편을 드려 죄송합니다. 관리자에게 문의하세요."
export const logoutMember = async () => {
    const response = await axiosInstance.post('/member/logout');
    // 토큰 없음!
    return parseBaseResponse(response);
};

