// 엑세스 토큰 저장소(메모리)
import Cookies from 'js-cookie';

import { reissueMember } from '../api/memberApi';

let accessToken = null;
let isInitialized = false;

const checkLoginFlag = () => {
    const flag = Cookies.get('isLoggedIn');
    return flag === 'true' || decodeURIComponent(flag || '') === 'true';
};

const authStore = {
    getToken: () => accessToken,
    setToken: (token) => { accessToken = token; },

    getUserRole: () => Cookies.get('userRole') || 'USER',

    clearToken: () => {
        accessToken = null;
        Cookies.remove('isLoggedIn');
        Cookies.remove('userRole');
    },
    isLoggedIn: () => !!accessToken || checkLoginFlag(),

    getIsInitialized: () => isInitialized,

    // silent refresh
    async initAuth() {
        if (!checkLoginFlag()) {
            isInitialized = true;
            return;
        }
        try {
            const response = await reissueMember();
            if (response.token) {
                this.setToken(response.token);
            }
        } catch {
            this.clearToken();
        } finally {
            isInitialized = true;
        }
    }
};

export default authStore;
