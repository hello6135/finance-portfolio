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
        } catch (error) {
            // 네트워크 오류는 쿠키 삭제하지 않음
            const status = error.response?.status;

            // 서버가 명시적으로 인증 실패를 응답한 경우만 로그아웃
            if (status && status >= 400 && status < 500) {
                this.clearToken();  // 401, 400 등 → 실제 인증 실패
            }
        } finally {
            isInitialized = true;
        }
    }
};

export default authStore;
