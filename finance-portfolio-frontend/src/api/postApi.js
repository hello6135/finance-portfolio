import axiosInstance from './axios';

// 1. 게시글 목록 조회 (GET /api/posts/list)
export const getPostList = async (page, size) => {
    const response = await axiosInstance.get('/posts/list', {
        params: { page, size }
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

// 6. 미참조 파일 정리 (DELETE /api/posts/cleanup)
export const cleanUpFiles = async () => {
    await axiosInstance.delete('/posts/cleanup');
};

// 7. CKeditor 전용 업로드 어댑터
export const imageUploadAdapter = (loader) => {
    return {
        upload: async () => {
            try {
                // 1. 파일 로드 (await로 뎁스 제거)
                const file = await loader.file;

                // 2. FormData 준비
                const formData = new FormData();
                formData.append('upload', file);

                // 3. API 호출
                const response = await axiosInstance.post('/image/upload', formData, {
                    headers: {
                        'Content-Type': 'multipart/form-data',
                    },
                });

                // 4. 성공 시 결과 반환
                return {
                    default: response.data.url
                };
            } catch (err) {
                // 5. 에러 핸들링
                const errorMessage = err.response?.data?.message || '업로드 실패';
                throw errorMessage; // async 함수에서 에러는 throw하면 reject와 동일합니다.
            }
        }
    };
};