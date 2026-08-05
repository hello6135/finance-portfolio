import React from 'react';
import { Navigate, Outlet } from 'react-router';
import authStore from '../../store/authStore';

// 인증 필요 라우트 접근시 동작
const PrivateRoute = () => {
    const isLogged = authStore.isLoggedIn();

    // 로그인이 안 되어 있다면 로그인 페이지로
    if (!isLogged) {
        return <Navigate to="/login" replace />;
    }

    // 로그인 상태라면 자식 컴포넌트(Outlet) 렌더링
    return <Outlet />;
};

export default PrivateRoute;

