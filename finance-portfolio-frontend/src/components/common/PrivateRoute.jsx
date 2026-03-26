import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import authStore from '../../store/authStore';

// 로그인하지 않은 사용자를 로그인 페이지로 리다이렉트합니다.
// Access Token이 메모리에 없으면 미인증 상태로 판단합니다.
const PrivateRoute = () => {
    if (!authStore.isLoggedIn()) {
        return <Navigate to="/login" replace />;
    }
    return <Outlet />;
};


export default PrivateRoute;

