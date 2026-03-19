import React, { useEffect, useState } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import axiosInstance from './api/axios';
import authStore from './api/authStore';
import './App.css';

import PostList from './pages/posts/PostList';
import PostDetail from './pages/posts/PostDetail';
import PostEditor from './pages/posts/PostEditor';
import FairValuePage from './pages/finances/FinanceFair';
import LoginPage from './pages/members/LoginPage';
import JoinPage from './pages/members/JoinPage';
import PrivateRoute from './components/PrivateRoute';

function App() {
  const [authChecked, setAuthChecked] = useState(false);

  useEffect(() => {
    // 로그인/회원가입 페이지에서는 silent refresh 시도 안 함
    if (globalThis.location.pathname === '/login' || globalThis.location.pathname === '/join') {
      setAuthChecked(true);
      return;
    }
    // Silent Refresh: 새로고침 후에도 Refresh Token 쿠키가 살아있으면 자동 로그인 유지
    axiosInstance.post('/auth/reissue')
      .then((res) => {
        const token = res.headers['authorization']?.replace('Bearer ', '');
        if (token) {
          authStore.setToken(token);
        }
      })
      .catch(() => {
        // Refresh Token이 없거나 만료됨 → 비로그인 상태 유지 (정상 케이스)
        authStore.clearToken();
      })
      .finally(() => {
        setAuthChecked(true);
      });
  }, []);

  if (!authChecked) {
    return (
      <div className="loading-screen">
        로딩 중...
      </div>
    );
  }

  return (
    <Routes>
      {/* 공개 라우트 */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/join" element={<JoinPage />} />

      {/* 인증 필요 라우트 */}
      <Route path="/" element={
        <PrivateRoute><PostList /></PrivateRoute>
      } />
      <Route path="/posts" element={
        <PrivateRoute><PostList /></PrivateRoute>
      } />
      <Route path="/detail/:id" element={
        <PrivateRoute><PostDetail /></PrivateRoute>
      } />
      <Route path="/editor" element={
        <PrivateRoute><PostEditor /></PrivateRoute>
      } />
      <Route path="/editor/:id" element={
        <PrivateRoute><PostEditor /></PrivateRoute>
      } />
      <Route path="/finance/fair" element={
        <PrivateRoute><FairValuePage /></PrivateRoute>
      } />

      {/* 없는 경로는 로그인 페이지로 */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default App;