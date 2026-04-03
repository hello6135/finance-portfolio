// 엑세스 토큰 저장소(메모리)

let accessToken = null;

const authStore = {
    getToken: () => accessToken,
    setToken: (token) => { accessToken = token; },
    clearToken: () => { accessToken = null; },
    isLoggedIn: () => !!accessToken,
};

export default authStore;
