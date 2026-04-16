import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode'; // 설치 필요: npm install jwt-decode

interface TokenPayload {
    role: string;
    exp: number;
}

const AdminRoute = () => {
    const token = localStorage.getItem('token');

    if (!token) return <Navigate to="/login" replace />;

    try {
        const decoded: TokenPayload = jwtDecode(token);

        // DB의 role이 'ADMIN'이므로 이에 맞춰 조건 확인
        if (decoded.role !== 'ADMIN') {
            alert('관리자만 접근 가능합니다.');
            return <Navigate to="/" replace />;
        }

        return <Outlet />;
    } catch {
        return <Navigate to="/login" replace />;
    }
};

export default AdminRoute;