import React, { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, useNavigate } from 'react-router-dom';
import { navRef } from './utils/history';
import './index.css';
import App from './App.jsx';

// 리액트 외부에서 라우터 사용을 하기 위한 컴포넌트
const NavigateSetter = () => {
  const navigate = useNavigate();
  navRef.navigate = navigate;
  return null;
};

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <NavigateSetter />
      <App />
    </BrowserRouter>
  </StrictMode>
);
