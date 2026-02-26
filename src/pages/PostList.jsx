import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

import { getAllPosts, cleanUpFiles } from '../api/postApi';

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
        if (window.confirm('미참조 이미지를 삭제하시겠습니까?')) {
            try {
                await cleanUpFiles();
            } catch (error) {
                alert("삭제에 실패했습니다.");
            }
        }
    };

    if (loading) return <div>로딩 중...</div>;

    return (
        <div className="container mt-5">
            <h2>금융 포트폴리오 게시판</h2>

            <button onClick={() => navigate(`/editor`)} className="btn btn-warning">새 글</button>
            <button onClick={onCleanUpFiles} className="btn btn-warning">미참조 이미지 삭제</button>
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
                                        hour12: false // 24시간 형식
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