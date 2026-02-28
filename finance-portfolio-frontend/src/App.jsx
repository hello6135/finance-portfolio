import React from 'react';
import { Routes, Route } from 'react-router-dom';
import PostList from './pages/PostList';
import PostDetail from './pages/PostDetail';
import PostEditor from './pages/PostEditor';

function App() {
  return (
    <Routes>
      <Route path="/" element={<PostList />} />
      <Route path="/posts" element={<PostList />} />
      <Route path="/detail/:id" element={<PostDetail />} />
      <Route path="/editor" element={<PostEditor />} />
      <Route path="/editor/:id" element={<PostEditor />} />
    </Routes>
  );
}

export default App;