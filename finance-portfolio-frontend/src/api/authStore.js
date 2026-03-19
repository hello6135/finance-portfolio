let accessToken = null;

const authStore = {
    getToken: () => accessToken,
    setToken: (token) => { accessToken = token; },
    clearToken: () => { accessToken = null; },
    isLoggedIn: () => !!accessToken,
};

export default authStore;
