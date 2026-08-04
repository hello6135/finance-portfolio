import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router';
import DOMPurify from 'dompurify';
import { jwtDecode } from 'jwt-decode';
import PropTypes from 'prop-types';

import LoadingPage from '../common/LoadingPage';
import { getPostById, deletePost } from '../../api/postApi';
import { getCommentsByPost, createComment, updateComment, deleteComment } from '../../api/commentApi';
import authStore from '../../store/authStore';

// 날짜 시간 포맷 헬퍼 함수 (예: 2026.06.11 21:34)
const formatDate = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}.${month}.${day} ${hours}:${minutes}`;
};

// 재귀형 댓글 노드 컴포넌트
const CommentNode = ({ 
    comment, 
    currentLoginId, 
    onReply, 
    onEdit, 
    onDelete, 
    replyingId, 
    setReplyingId, 
    editingId, 
    setEditingId, 
    postAuthorNickname 
}) => {
    const [replyText, setReplyText] = useState('');
    const [editText, setEditText] = useState(comment.content);

    const isAuthor = comment.authorLoginId === currentLoginId;
    const isAdmin = authStore.getUserRole() === 'ADMIN';
    const canManage = isAuthor || isAdmin;

    const isPostAuthor = comment.authorNickname === postAuthorNickname;

    const handleReplySubmit = () => {
        if (!replyText.trim()) return;
        onReply(comment.id, replyText);
        setReplyText('');
        setReplyingId(null);
    };

    const handleEditSubmit = () => {
        if (!editText.trim()) return;
        onEdit(comment.id, editText);
        setEditingId(null);
    };

    return (
        <div className="mb-3 p-3 bg-white border rounded shadow-sm text-dark">
            <div className="d-flex justify-content-between align-items-center mb-2">
                <div>
                    <span className="fw-bold me-2" style={{ fontSize: '0.95rem' }}>{comment.authorNickname}</span>
                    {isPostAuthor && <span className="badge bg-primary me-2" style={{ fontSize: '0.75rem' }}>작성자</span>}
                    <span className="text-muted small">
                        {formatDate(comment.createdAt)}
                    </span>
                </div>
                <div className="d-flex gap-2">
                    {authStore.isLoggedIn() && (
                        <button 
                            onClick={() => {
                                setReplyingId(replyingId === comment.id ? null : comment.id);
                                setEditingId(null);
                            }} 
                            className="btn btn-sm btn-outline-secondary py-1 px-2"
                            style={{ fontSize: '0.8rem' }}
                        >
                            답글
                        </button>
                    )}
                    {canManage && (
                        <>
                            <button 
                                onClick={() => {
                                    setEditingId(editingId === comment.id ? null : comment.id);
                                    setEditText(comment.content);
                                    setReplyingId(null);
                                }} 
                                className="btn btn-sm btn-outline-warning py-1 px-2"
                                style={{ fontSize: '0.8rem' }}
                            >
                                수정
                            </button>
                            <button 
                                onClick={() => onDelete(comment.id)} 
                                className="btn btn-sm btn-outline-danger py-1 px-2"
                                style={{ fontSize: '0.8rem' }}
                            >
                                삭제
                            </button>
                        </>
                    )}
                </div>
            </div>

            {editingId === comment.id ? (
                <div className="mt-2">
                    <textarea 
                        value={editText} 
                        onChange={(e) => setEditText(e.target.value)} 
                        className="form-control mb-2" 
                        rows="2"
                    />
                    <div className="d-flex gap-2 justify-content-end">
                        <button onClick={handleEditSubmit} className="btn btn-sm btn-warning">수정 완료</button>
                        <button onClick={() => setEditingId(null)} className="btn btn-sm btn-secondary">취소</button>
                    </div>
                </div>
            ) : (
                <p className="mb-0 text-dark" style={{ whiteSpace: 'pre-wrap', fontSize: '0.95rem' }}>{comment.content}</p>
            )}

            {replyingId === comment.id && (
                <div className="mt-3 p-3 bg-light border rounded">
                    <h6 className="small fw-bold mb-2">답글 작성</h6>
                    <textarea 
                        value={replyText} 
                        onChange={(e) => setReplyText(e.target.value)} 
                        placeholder="답글을 입력하세요..." 
                        className="form-control mb-2" 
                        rows="2"
                    />
                    <div className="d-flex gap-2 justify-content-end">
                        <button onClick={handleReplySubmit} className="btn btn-sm btn-primary">등록</button>
                        <button onClick={() => setReplyingId(null)} className="btn btn-sm btn-secondary">취소</button>
                    </div>
                </div>
            )}

            {/* 자식 대댓글 렌더링 */}
            {comment.children && comment.children.length > 0 && (
                <div className="ps-3 mt-3 border-start" style={{ borderColor: '#dee2e6', borderWidth: '2px' }}>
                    {comment.children.map(child => (
                        <CommentNode 
                            key={child.id} 
                            comment={child} 
                            currentLoginId={currentLoginId}
                            onReply={onReply}
                            onEdit={onEdit}
                            onDelete={onDelete}
                            replyingId={replyingId}
                            setReplyingId={setReplyingId}
                            editingId={editingId}
                            setEditingId={setEditingId}
                            postAuthorNickname={postAuthorNickname}
                        />
                    ))}
                </div>
            )}
        </div>
    );
};

// 재귀적 구조 대응을 위한 PropTypes 정의
const commentShape = {
    id: PropTypes.number.isRequired,
    content: PropTypes.string.isRequired,
    authorLoginId: PropTypes.string,
    authorNickname: PropTypes.string,
    createdAt: PropTypes.string,
};
commentShape.children = PropTypes.arrayOf(PropTypes.shape(commentShape));

CommentNode.propTypes = {
    comment: PropTypes.shape(commentShape).isRequired,
    currentLoginId: PropTypes.string,
    onReply: PropTypes.func.isRequired,
    onEdit: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
    replyingId: PropTypes.number,
    setReplyingId: PropTypes.func.isRequired,
    editingId: PropTypes.number,
    setEditingId: PropTypes.func.isRequired,
    postAuthorNickname: PropTypes.string
};

const PostDetail = () => {
    const { id } = useParams();
    const [post, setPost] = useState(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    // 댓글 관련 상태
    const [comments, setComments] = useState([]);
    const [newCommentText, setNewCommentText] = useState('');
    const [replyingCommentId, setReplyingCommentId] = useState(null);
    const [editingCommentId, setEditingCommentId] = useState(null);

    const fetchComments = useCallback(async () => {
        try {
            const data = await getCommentsByPost(id);
            setComments(data);
        } catch (error) {
            console.error("댓글 로딩 실패:", error);
        }
    }, [id]);

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
        fetchComments();
    }, [id, navigate, fetchComments]);

    // 로그인 ID 추출
    const token = authStore.getToken();
    let currentLoginId = null;
    if (token) {
        try {
            const decoded = jwtDecode(token);
            currentLoginId = decoded.sub;
        } catch (e) {
            console.error("토큰 디코드 실패:", e);
        }
    }

    // 권한 체크: 서버가 준 isOwner가 true이거나, 현재 사용자가 관리자인 경우
    const canManage = post?.isOwner || authStore.getUserRole() === 'ADMIN';

    const onDelete = async () => {
        if (globalThis.confirm('정말 삭제하시겠습니까?')) {
            try {
                await deletePost(id);
                alert("삭제되었습니다.");
                navigate('/posts');
            } catch (error) {
                console.error("삭제 중 오류 발생:", error);
                alert("삭제에 실패했습니다.");
            }
        }
    };

    // 댓글 작성 핸들러
    const handleRootCommentSubmit = async () => {
        if (!newCommentText.trim()) return;
        try {
            await createComment({
                postId: Number(id),
                parentId: null,
                content: newCommentText
            });
            setNewCommentText('');
            await fetchComments();
        } catch (error) {
            console.error("댓글 작성 실패:", error);
            alert("댓글 작성에 실패했습니다.");
        }
    };

    // 답글 작성 핸들러
    const handleReply = async (parentId, content) => {
        try {
            await createComment({
                postId: Number(id),
                parentId,
                content
            });
            await fetchComments();
        } catch (error) {
            console.error("답글 작성 실패:", error);
            alert("답글 작성에 실패했습니다.");
        }
    };

    // 댓글 수정 핸들러
    const handleEdit = async (commentId, content) => {
        try {
            await updateComment(commentId, { content });
            await fetchComments();
        } catch (error) {
            console.error("댓글 수정 실패:", error);
            alert("댓글 수정에 실패했습니다.");
        }
    };

    // 댓글 삭제 핸들러
    const handleDelete = async (commentId) => {
        if (globalThis.confirm('댓글을 정말 삭제하시겠습니까?')) {
            try {
                await deleteComment(commentId);
                await fetchComments();
            } catch (error) {
                console.error("댓글 삭제 실패:", error);
                alert("댓글 삭제에 실패했습니다.");
            }
        }
    };

    if (loading) return <LoadingPage />;
    if (!post) return null;

    return (
        <div className="container py-4">
            {/* 페이지 헤더 */}
            <div className="mb-4">
                <h2 className="mb-0 fw-bold">{post.categoryName}</h2>
            </div>

            {/* 게시글 상세 카드 */}
            <div className="p-4 bg-white border rounded shadow-sm text-dark mb-4" style={{ textAlign: 'left' }}>
                <div className="d-flex justify-content-between align-items-baseline mb-2">
                    <h3 className="mb-0 fw-bold" style={{ fontSize: '1.5rem' }}>{post.title}</h3>
                    <span className="text-muted small">{post.id}</span>
                </div>
                <div className="text-muted small mb-3">
                    <span className="fw-bold text-dark me-1">{post.author}</span>
                    <span className="text-muted mx-2" style={{ userSelect: 'none' }}>|</span>
                    <span>{formatDate(post.createdAt)}</span>
                </div>
                <hr className="my-3" />
                {/* dangerouslySetInnerHTML: 타임리프의 utext와 같은 기능
                dompurify를 통해 XSS 이중방어 */}
                <div
                    className="post-content py-2"
                    dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(post.content) }}
                    style={{ minHeight: '200px' }}
                />
            </div>

            <div className="mt-3 d-flex gap-2 justify-content-end">
                <button onClick={() => navigate('/posts')} className="btn btn-outline-secondary">목록으로</button>
                {/* 권한이 있을 때만 수정/삭제 버튼 렌더링 */}
                {canManage && (
                    <>
                        <button onClick={() => navigate(`/editor/${id}`)} className="btn btn-outline-warning">수정</button>
                        <button onClick={onDelete} className="btn btn-outline-danger">삭제</button>
                    </>
                )}
            </div>

            {/* 댓글 영역 */}
            <hr className="my-5" />
            <div className="comment-section mt-4 mb-5 text-dark" style={{ textAlign: 'left' }}>
                {/* 댓글 목록 */}
                {comments.length === 0 ? (
                    <div>
                    </div>
                ) : (
                    <div className="mb-4">
                        {comments.map(comment => (
                            <CommentNode 
                                key={comment.id} 
                                comment={comment} 
                                currentLoginId={currentLoginId}
                                onReply={handleReply}
                                onEdit={handleEdit}
                                onDelete={handleDelete}
                                replyingId={replyingCommentId}
                                setReplyingId={setReplyingCommentId}
                                editingId={editingCommentId}
                                setEditingId={setEditingCommentId}
                                postAuthorNickname={post.author}
                            />
                        ))}
                    </div>
                )}

                {/* 루트 댓글 작성창 */}
                {authStore.isLoggedIn() ? (
                    <div className="p-3 bg-light border rounded mt-4">
                        <h5 className="mb-3 small fw-bold">새 댓글 작성</h5>
                        <textarea 
                            value={newCommentText} 
                            onChange={(e) => setNewCommentText(e.target.value)} 
                            placeholder="내용을 입력하세요..." 
                            className="form-control mb-3" 
                            rows="3"
                        />
                        <div className="d-flex justify-content-end">
                            <button onClick={handleRootCommentSubmit} className="btn btn-primary px-4">댓글 등록</button>
                        </div>
                    </div>
                ) : (
                    <div className="p-4 bg-light border rounded text-center text-muted mt-4">
                        댓글을 작성하려면 <button onClick={() => navigate('/login')} className="btn btn-link p-0 pb-1 align-baseline fw-bold">로그인</button>이 필요합니다.
                    </div>
                )}
            </div>
        </div>
    );
};

export default PostDetail;
