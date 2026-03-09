import axiosInstance from './axios';
import { getAllPosts, createPost } from './postApi';

jest.mock('./axios', () => ({
    get: jest.fn(),
    post: jest.fn(),
    patch: jest.fn(),
    delete: jest.fn(),
}));

describe('Posts API Unit Test', () => {
    test('getAllPosts는 게시글 목록을 가져와야 한다', async () => {
        const mockData = [{ id: 1, title: '테스트 게시글' }];
        axiosInstance.get.mockResolvedValue({ data: mockData });

        const result = await getAllPosts();

        expect(axiosInstance.get).toHaveBeenCalledWith('/posts');
        expect(result).toEqual(mockData);
    });

    test('createPost는 게시글 데이터를 전송해야 한다', async () => {
        const postData = { title: '제목', content: '내용' };
        axiosInstance.post.mockResolvedValue({ data: 1 });

        const result = await createPost(postData);

        expect(axiosInstance.post).toHaveBeenCalledWith('/posts', postData);
        expect(result).toBe(1);
    });
});