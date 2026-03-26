import React, { useEffect, useState } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import axiosInstance from './api/axios';
import authStore from './store/authStore';
import './App.css';

import Layout from './components/layout/Layout';

import PostList from './pages/posts/PostList';
import PostDetail from './pages/posts/PostDetail';
import PostEditor from './pages/posts/PostEditor';
import FairValuePage from './pages/finances/FinanceFair';
import LoginPage from './pages/members/LoginPage';
import JoinPage from './pages/members/JoinPage';
import ErrorPage from './pages/common/ErrorPage';
import PrivateRoute from './components/common/PrivateRoute';


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
      {/* 레이아웃: 네비게이션, */}
      <Route element={<Layout />}>

        {/* 공개 라우트 - 게시판 */}
        <Route path="/" element={<PostList />} />
        <Route path="/posts" element={<PostList />} />
        <Route path="/detail/:id" element={<PostDetail />} />
        {/* 공개 라우트 - 금융계산기 */}
        <Route path="/finance/fair" element={<FairValuePage />} />
        {/* 공개 라우트 - 인증 인가 */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/join" element={<JoinPage />} />

        {/* 인증 필요 라우트 */}
        <Route element={<PrivateRoute />}>
          <Route path="/editor" element={<PostEditor />} />
          <Route path="/editor/:id" element={<PostEditor />} />
        </Route>

        {/* 에러 라우트 */}
        <Route path="/error" element={<ErrorPage />} />

        {/* 명시한 엔드포인트 이외엔 에러 페이지로 */}
        <Route path="*" element={<ErrorPage status="404" message="페이지를 찾을 수 없습니다." />} />
      </Route>
    </Routes>
  );
}

export default App;