import axiosInstance from './axios';

// 1. 게시글 목록 조회 (GET /api/posts/list)
export const getPostList = async (page, size, categoryId) => {
    const response = await axiosInstance.get('/posts/list', {
        params: { page, size, categoryId }
    });
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

// 6. CKeditor 전용 업로드 어댑터
export const imageUploadAdapter = (loader) => {
    return {
        upload: async () => {
            try {
                // 파일 로드
                const file = await loader.file;

                // 프론트 사전 검증 (CKEditor 기본 제한 우회, 서버 제한과 맞춰야함)
                if (file.size > 1 * 1024 * 1024) {
                    throw new Error('파일 용량이 너무 큽니다. (최대 1MB)');
                }

                // FormData 준비
                const formData = new FormData();
                formData.append('upload', file);

                // API 호출
                const response = await axiosInstance.post('/image/upload', formData, {
                    headers: { 'Content-Type': 'multipart/form-data' },
                });

                // 파일 업로드 실패시 예외처리
                if (!response.data.uploaded) {
                    throw new Error(response.data.error?.message || '업로드 실패');
                }

                // 성공 시 결과 반환
                return { default: response.data.url };

            } catch (err) {
                const raw = err.response?.data?.error?.message || err.message || '이미지 업로드에 실패했습니다.';
                throw new Error(raw.replace(/^Error:\s*/, ''));
            }
        }
    };
};