import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { getAllPosts, cleanUpFiles } from '../../api/postApi';
import axiosInstance from '../../api/axios';
import authStore from '../../api/authStore';

const PostList = () => {
    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchPosts = async () => {
            try {
                const data = await getAllPosts();
                setPosts(data);
            } catch (error) {
                console.error("데이터 호출 실패:", error);
            } finally {
                setLoading(false);
            }
        };
        fetchPosts();
    }, []);

    const onCleanUpFiles = async () => {
        if (globalThis.confirm('미참조 이미지를 삭제하시겠습니까?')) {
            try {
                await cleanUpFiles();
            } catch (error) {
                console.error("삭제 중 오류 발생:", error);
                alert("삭제에 실패했습니다.");
            }
        }
    };

    if (loading) return <div>로딩 중...</div>;

    return (
        <div className="container mt-5">
            <div className="d-flex justify-content-between align-items-center mb-3">
                <h2>금융 포트폴리오 게시판</h2>
            </div>

            <div className="mb-3 d-flex gap-2">
                <button onClick={() => navigate('/editor')} className="btn btn-warning">새 글</button>
                <button onClick={onCleanUpFiles} className="btn btn-warning">미참조 이미지 삭제</button>
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
                                <td onClick={() => navigate(`/detail/${post.id}`)}>
                                    {post.hasImage && <span className="badge bg-info me-1">📷</span>}
                                    {post.title}
                                </td>
                                <td>
                                    {new Intl.DateTimeFormat('ko-KR', {
                                        year: 'numeric',
                                        month: '2-digit',
                                        day: '2-digit',
                                        hour: '2-digit',
                                        minute: '2-digit',
                                        hour12: false,
                                    }).format(new Date(post.createdAt))}
                                </td>
                            </tr>
                        ))
                    ) : (
                        <tr><td colSpan="3">게시글이 없습니다.</td></tr>
                    )}
                </tbody>
            </table>
        </div>
    );
};

export default PostList;