// 엑세스 토큰 저장소(메모리)
import Cookies from 'js-cookie';

let accessToken = null;

const checkLoginFlag = () => {
    const flag = Cookies.get('isLoggedIn');
    return flag === 'true' || decodeURIComponent(flag || '') === 'true';
};

const authStore = {
    getToken: () => accessToken,
    setToken: (token) => { accessToken = token; },
    clearToken: () => { accessToken = null; },
    isLoggedIn: () => !!accessToken || checkLoginFlag(),
};

export default authStore;
