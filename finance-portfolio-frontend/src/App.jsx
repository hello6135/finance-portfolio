import React, { useEffect, useState } from 'react';
import { Routes, Route } from 'react-router-dom';
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
import PrivateRoute from './components/auth/PrivateRoute';
import { reissueMember } from './api/memberApi';

function App() {
  const [authChecked, setAuthChecked] = useState(false);

  // 새로고침 혹은 사이트 처음 접속 시 실행
  useEffect(() => {
    // 1. async 로직을 별도 함수로 분리
    const initAuth = async () => {
      const pathname = globalThis.location.pathname;
      const hasAuthCookie = document.cookie.includes('isLoggedIn=true');

      // 로그인/회원가입 페이지에서는 silent refresh 시도 안 함
      // 로그인 흔적도 없으면 silent refresh 시도 안 함
      if (pathname === '/login' || pathname === '/join' || !hasAuthCookie) {
        setAuthChecked(true);
        return;
      }

      try {
        // Silent Refresh 시도
        const response = await reissueMember();
        // memberApi의 응답 구조에 따라 적절히 수정 (예: response.data)
        const { status, token } = response;

        if ((status === 200 || status === 201) && token) {
          authStore.setToken(token);
        }
      } catch {
        authStore.clearToken();
      } finally {
        setAuthChecked(true);
      }
    };

    initAuth();
  }, []);


  if (!authChecked) {
    return (
      <div className="loading-screen">
        인증 검사 중...
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

        {/* 명시한 엔드포인트 이외엔 404 */}
        <Route path="*" element={<ErrorPage status="404" message="페이지를 찾을 수 없습니다." />} />
      </Route>
    </Routes>
  );
}

export default App;