import axiosInstance from './axios';

// 1. 게시글 댓글 목록 조회 (GET /api/comments/post/{postId})
export const getCommentsByPost = async (postId) => {
    const response = await axiosInstance.get(`/comments/post/${postId}`);
    return response.data; // List<CommentResponseDto> 가 들어옴
};

// 2. 댓글 작성 (POST /api/comments)
export const createComment = async (commentData) => {
    // commentData: { postId, parentId, content }
    const response = await axiosInstance.post('/comments', commentData);
    return response.data; // 생성된 ID 반환
};

// 3. 댓글 수정 (PATCH /api/comments/{id})
export const updateComment = async (id, commentData) => {
    // commentData: { content }
    const response = await axiosInstance.patch(`/comments/${id}`, commentData);
    return response.data;
};

// 4. 댓글 삭제 (DELETE /api/comments/{id})
export const deleteComment = async (id) => {
    const response = await axiosInstance.delete(`/comments/${id}`);
    return response.data;
};
