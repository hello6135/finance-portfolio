import React from 'react';
import { Navigate } from 'react-router-dom';
import authStore from '../api/authStore';

// 로그인하지 않은 사용자를 로그인 페이지로 리다이렉트합니다.
// Access Token이 메모리에 없으면 미인증 상태로 판단합니다.
// (페이지 새로고침 시 토큰이 초기화되는 문제는 아래 주석 참고)
const PrivateRoute = ({ children }) => {
    if (!authStore.isLoggedIn()) {
        return <Navigate to="/login" replace />;
    }
    return children;
};

export default PrivateRoute;

/*
 * [새로고침 시 로그인 유지 처리]
 *
 * 메모리 저장 방식 특성상 새로고침하면 Access Token이 사라집니다.
 * 이를 해결하려면 앱 최초 마운트 시(App.jsx) /auth/reissue를 한 번 호출해서
 * Refresh Token 쿠키로 새 Access Token을 받아오는 "silent refresh" 패턴을 쓰면 됩니다.
 *
 * 예시 (App.jsx에서 사용):
 *
 * useEffect(() => {
 *     axiosInstance.post('/auth/reissue')
 *         .then(res => {
 *             const token = res.headers['authorization']?.replace('Bearer ', '');
 *             if (token) authStore.setToken(token);
 *         })
 *         .catch(() => {}) // 비로그인 상태면 무시
 *         .finally(() => setAuthChecked(true));
 * }, []);
 */