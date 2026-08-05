import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router';

import { getPostList } from '../../api/postApi';
import { getCategories } from '../../api/categoryApi';
import LoadingPage from '../common/LoadingPage';

const PostList = () => {
    const [posts, setPosts] = useState([]);
    const [pageInfo, setPageInfo] = useState({
        currentPage: 0,
        totalPages: 0,
        totalElements: 0
    });
    const [loading, setLoading] = useState(true);
    const [categoryName, setCategoryName] = useState('');

    const navigate = useNavigate();
    const location = useLocation();

    // 1. URL에서 카테고리 ID 추출
    const queryParams = new URLSearchParams(location.search);
    const categoryId = queryParams.get('category');

    const fetchPostList = useCallback(async (page = 0, size = 10) => {
        setLoading(true);
        try {
            const data = await getPostList(page, size, categoryId);
            const { content, number, totalPages, totalElements } = data;

            setPosts(content);
            setPageInfo({
                currentPage: number,
                totalPages: totalPages,
                totalElements: totalElements
            });
        } catch (error) {
            console.error("데이터 호출 실패:", error);
        } finally {
            setLoading(false);
        }
    }, [categoryId]);

    useEffect(() => {
        fetchPostList(0, 10);
    }, [fetchPostList]);

    // 카테고리 이름 조회 및 세팅
    useEffect(() => {
        if (categoryId) {
            getCategories()
                .then((list) => {
                    const matched = list.find(cat => String(cat.id) === String(categoryId));
                    if (matched) {
                        setCategoryName(matched.name);
                    } else {
                        setCategoryName('카테고리');
                    }
                })
                .catch((err) => {
                    console.error("카테고리 호출 실패:", err);
                    setCategoryName('카테고리');
                });
        } else {
            setCategoryName('');
        }
    }, [categoryId]);

    if (loading) return <LoadingPage />;

    return (
        // 페이지
        <div className="container py-4">
            {/* 페이지 헤더 */}
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h2 className="mb-0">
                    {categoryId ? (categoryName || posts[0]?.categoryName || '카테고리') : '전체 글'}
                </h2>
                <button 
                    type="button"
                    onClick={() => navigate('/editor')}
                    className="btn btn-warning"
                >
                    새 글
                </button>
            </div>

            {/* 페이지 바디: 테이블 */}
            <table className="table table-hover">
                {/* 테이블 헤더 */}
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>제목</th>
                        <th>작성자</th>
                        <th>시간</th>
                    </tr>
                </thead>
                {/* 테이블 바디 */}
                <tbody>
                    {posts.length > 0 ? (
                        posts.map((post) => (
                            <tr key={post.id}>
                                <td>{post.id}</td>
                                <td onClick={() => navigate(`/detail/${post.id}`)} style={{ cursor: 'pointer' }}>
                                    {post.hasImage && <span className="badge bg-info me-1">📷</span>}
                                    {post.title}
                                </td>
                                <td>{post.author}</td>
                                <td>
                                    {new Intl.DateTimeFormat('ko-KR', {
                                        year: 'numeric', month: '2-digit', day: '2-digit',
                                    }).format(new Date(post.createdAt))}
                                </td>
                            </tr>
                        ))
                    ) : (
                        <tr><td colSpan="3" className="text-center">게시글이 없습니다.</td></tr>
                    )}
                </tbody>
            </table>
            {/* 페이징 네비게이션*/}
            <nav>
                <ul className="pagination justify-content-center">
                    {/* 이전 버튼 */}
                    <li className={`page-item ${pageInfo.currentPage === 0 ? 'disabled' : ''}`}>
                        <button 
                            type="button"
                            className="page-link"
                            onClick={() => fetchPostList(pageInfo.currentPage - 1)}
                        >
                            이전
                        </button>
                    </li>

                    {/* 페이지 번호: 현재 페이지 기준 유동적 노출 */}
                    {Array.from({ length: pageInfo.totalPages }, (_, i) => {
                        // 현재 페이지 앞뒤 2개씩만 노출
                        if (i >= pageInfo.currentPage - 2 && i <= pageInfo.currentPage + 2) {
                            return (
                                <li key={i} className={`page-item ${pageInfo.currentPage === i ? 'active' : ''}`}>
                                    <button 
                                        type="button"
                                        className="page-link"
                                        onClick={() => fetchPostList(i)}
                                    >
                                        {i + 1}
                                    </button>
                                </li>
                            );
                        }
                        return null;
                    })}

                    {/* 다음 버튼 */}
                    <li className={`page-item ${pageInfo.currentPage >= pageInfo.totalPages - 1 ? 'disabled' : ''}`}>
                        <button 
                            type="button"
                            className="page-link"
                            onClick={() => fetchPostList(pageInfo.currentPage + 1)}
                        >
                            다음
                        </button>
                    </li>
                </ul>
            </nav>
        </div>
    );
};

export default PostList;