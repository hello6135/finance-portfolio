import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { getPostList, cleanUpFiles } from '../../api/postApi';
import LoadingPage from '../common/LoadingPage';
import { navRef } from '../../utils/history';

const PostList = () => {
    const [posts, setPosts] = useState([]);
    const [pageInfo, setPageInfo] = useState({
        currentPage: 0,
        totalPages: 0,
        totalElements: 0
    });
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    const fetchPostList = async (page = 0, size = 10) => {
        setLoading(true);
        try {
            const data = await getPostList(page, size);
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
    };

    useEffect(() => {
        fetchPostList(0, 10);
    }, []);




    if (loading) return <LoadingPage />;

    return (
        <div className="container mt-5">
            <div className="mb-3 d-flex gap-2">
                <button onClick={() => navigate('/editor')} className="btn btn-warning">새 글</button>
            </div>
            <table className="table table-hover">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>제목</th>
                        <th>시간</th>
                    </tr>
                </thead>
                <tbody>
                    {posts.length > 0 ? (
                        posts.map((post) => (
                            <tr key={post.id}>
                                <td>{post.id}</td>
                                <td onClick={() => navigate(`/detail/${post.id}`)} style={{ cursor: 'pointer' }}>
                                    {post.hasImage && <span className="badge bg-info me-1">📷</span>}
                                    {post.title}
                                </td>
                                <td>
                                    {new Intl.DateTimeFormat('ko-KR', {
                                        year: 'numeric', month: '2-digit', day: '2-digit',
                                        hour: '2-digit', minute: '2-digit', hour12: false,
                                    }).format(new Date(post.createdAt))}
                                </td>
                            </tr>
                        ))
                    ) : (
                        <tr><td colSpan="3" className="text-center">게시글이 없습니다.</td></tr>
                    )}
                </tbody>
            </table>
            {/* 페이징 네비게이션 추가 */}
            <nav>
                <ul className="pagination justify-content-center">
                    {/* 이전 버튼 */}
                    <li className={`page-item ${pageInfo.currentPage === 0 ? 'disabled' : ''}`}>
                        <button className="page-link" onClick={() => fetchPostList(pageInfo.currentPage - 1)}>이전</button>
                    </li>

                    {/* 페이지 번호: 현재 페이지 기준 유동적 노출 */}
                    {[...Array(pageInfo.totalPages)].map((_, i) => {
                        // 현재 페이지 앞뒤 2개씩만 노출 (예: 1 2 [3] 4 5)
                        if (i >= pageInfo.currentPage - 2 && i <= pageInfo.currentPage + 2) {
                            return (
                                <li key={i} className={`page-item ${pageInfo.currentPage === i ? 'active' : ''}`}>
                                    <button className="page-link" onClick={() => fetchPostList(i)}>{i + 1}</button>
                                </li>
                            );
                        }
                        return null;
                    })}

                    {/* 다음 버튼 */}
                    <li className={`page-item ${pageInfo.currentPage >= pageInfo.totalPages - 1 ? 'disabled' : ''}`}>
                        <button className="page-link" onClick={() => fetchPostList(pageInfo.currentPage + 1)}>다음</button>
                    </li>
                </ul>
            </nav>
        </div>
    );
};

export default PostList;