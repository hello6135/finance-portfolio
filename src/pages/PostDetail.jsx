import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

import { getPostById, deletePost } from '../api/postApi';

const PostDetail = () => {
    const { id } = useParams();
    const [post, setPost] = useState(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchPost = async () => {
            try {
                const data = await getPostById(id);
                setPost(data);
            } catch (error) {
                console.error("상세 데이터 로딩 실패:", error);
                alert("존재하지 않는 게시글입니다.");
                navigate('/posts');
            } finally {
                setLoading(false);
            }
        };
        fetchPost();
    }, [id, navigate]);

    const onDelete = async () => {
        if (window.confirm('정말 삭제하시겠습니까?')) {
            try {
                await deletePost(id);
                navigate('/posts');
            } catch (error) {
                alert("삭제에 실패했습니다.");
            }
        }
    };

    if (loading) return <div className="container mt-5">로딩 중...</div>;
    if (!post) return null;

    return (
        <div className="container mt-5">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h2>게시글 상세</h2>
                <span className="text-muted">ID: {post.id}</span>
            </div>

            <table className="table table-bordered" style={{ color: 'black', backgroundColor: 'white' }}>
                <tbody>
                    <tr>
                        <th className="table-light" style={{ width: '20%' }}>제목</th>
                        <td>{post.title}</td>
                    </tr>
                    <tr>
                        <th className="table-light">내용</th>
                        {/* dangerouslySetInnerHTML: 타임리프의 utext와 같은 기능 */}
                        <td
                            className="post-content"
                            dangerouslySetInnerHTML={{ __html: post.content }}
                            style={{ minHeight: '200px' }}
                        />
                    </tr>
                    <tr>
                        <th className="table-light">작성일</th>
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
                </tbody>
            </table>

            <div className="mt-3 d-flex gap-2">
                <button onClick={() => navigate('/posts')} className="btn btn-secondary">목록으로</button>
                <button onClick={() => navigate(`/editor/${id}`)} className="btn btn-warning">수정</button>
                <button onClick={onDelete} className="btn btn-danger">삭제</button>
            </div>
        </div>
    );
};

export default PostDetail;