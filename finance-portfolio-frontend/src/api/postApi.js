import axiosInstance from './axios';

// 1. 게시글 목록 조회 (GET /api/posts)
export const getAllPosts = async () => {
    const response = await axiosInstance.get('/posts');
    return response.data; // List<PostResponseDto> 가 들어옴
};

// 2. 게시글 상세 조회 (GET /api/posts/{id})
export const getPostById = async (id) => {
    const response = await axiosInstance.get(`/posts/${id}`);
    return response.data; // PostResponseDto 가 들어옴
};

// 3. 게시글 저장 (POST /api/posts)
export const createPost = async (postData) => {
    // postData: { title, content }
    const response = await axiosInstance.post('/posts', postData);
    return response.data; // 생성된 ID 반환
};

// 4. 게시글 수정 (PATCH /api/posts/{id})
export const updatePost = async (id, updateData) => {
    const response = await axiosInstance.patch(`/posts/${id}`, updateData);
    return response.data;
};

// 5. 게시글 삭제 (DELETE /api/posts/{id})
export const deletePost = async (id) => {
    await axiosInstance.delete(`/posts/${id}`);
};

// 6. 미참조 파일 정리 (DELETE /api/posts/cleanup)
export const cleanUpFiles = async () => {
    await axiosInstance.delete('/posts/cleanup');
};