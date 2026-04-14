import axiosInstance from './axios';

// 게시글 목록 조회 (GET /api/category)
// @returns {Promise<Array>} List<CategoryResponseDto>
export const getCategories = async () => {
    const response = await axiosInstance.get('/category');
    return response.data; // List<PostResponseDto> 가 들어옴
};

// 카테고리 생성 (ADMIN 전용)
// @param {Object} data - { name: string, order: number }
// @returns {Promise<number>} 생성된 카테고리 ID
export const createCategory = async (data) => {
    const response = await axiosInstance.post('/category', data);
    return response.data;
};

// 카테고리 수정 (ADMIN 전용)
// @param {number} id - 수정할 카테고리 ID
// @param {Object} data - { name: string, order: number }
// @returns {Promise<number>} 수정된 카테고리 ID
export const updateCategory = async (id, data) => {
    const response = await axiosInstance.patch(`/category/${id}`, data);
    return response.data;
};

// 카테고리 삭제 (ADMIN 전용)
// @param {number} id - 삭제할 카테고리 ID
// @returns {Promise<number>} 삭제된 카테고리 ID
export const deleteCategory = async (id) => {
    const response = await axiosInstance.delete(`/category/${id}`);
    return response.data;
};