import React, { useEffect, useState } from 'react';
import { Routes, Route } from 'react-router-dom';
import authStore from './store/authStore';
import './App.css';

import Layout from './components/layout/Layout';
import PostLayout from './components/layout/PostLayout';
import AdminLayout from './components/layout/AdminLayout';
import PrivateRoute from './components/auth/PrivateRoute';
import AdminRoute from './components/auth/AdminRoute';

import PostList from './pages/posts/PostList';
import PostDetail from './pages/posts/PostDetail';
import PostEditor from './pages/posts/PostEditor';
import FairValuePage from './pages/finances/FinanceFair';
import LoginPage from './pages/members/LoginPage';
import JoinPage from './pages/members/JoinPage';
import ErrorPage from './pages/common/ErrorPage';
import LoadingPage from './pages/common/LoadingPage';
import AdminDashboard from './pages/admins/AdminDashboard';
import AdminCategoryManager from './pages/admins/AdminCategoryManager';


function App() {
  const [authChecked, setAuthChecked] = useState(false);

  // 새로고침 혹은 사이트 처음 접속 시 실행
  useEffect(() => {
    // silent refresh
    authStore.initAuth().then(() => {
      setAuthChecked(true); // 전역 로딩 해제
    });
  }, []);


  if (!authChecked) {
    return <LoadingPage />;
  }

  return (
    <Routes>
      {/* 레이아웃: 네비게이션, */}
      <Route element={<Layout />}>

        {/* 공개 라우트 - 게시판 */}
        <Route element={<PostLayout />}> {/* 카테고리 네비게이션 포함 */}
          <Route path="/" element={<PostList />} />
          <Route path="/posts" element={<PostList />} />
        </Route>
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

        {/* 관리자 라우트 */}
        <Route element={<AdminRoute />}>
          <Route element={<AdminLayout />}>
            <Route path="/admin/dashboard" element={<AdminDashboard />} />
            <Route path="/admin/categoryManager" element={<AdminCategoryManager />} />
          </Route>
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