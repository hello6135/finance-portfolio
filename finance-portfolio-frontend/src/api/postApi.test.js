import { vi, describe, test, expect } from 'vitest';
import axiosInstance from './axios';
import {
    getAllPosts,
    getPostById,
    createPost,
    updatePost,
    deletePost,
    cleanUpFiles,
    imageUploadAdapter
} from './postApi';

// axiosInstance 모킹
vi.mock('./axios', () => ({
    default: {
        get: vi.fn(),
        post: vi.fn(),
        patch: vi.fn(),
        delete: vi.fn(),
    },
}));

describe('postApi 100% 커버리지 완성 테스트', () => {

    // --- 성공 케이스들 (20대 라인들 공략) ---
    test('모든 게시글 조회 성공', async () => {
        axiosInstance.get.mockResolvedValue({ data: [] });
        await getAllPosts();
        expect(axiosInstance.get).toHaveBeenCalledWith('/posts');
    });

    test('상세 게시글 조회 성공 (12번 라인)', async () => {
        axiosInstance.get.mockResolvedValue({ data: {} });
        await getPostById(1);
        expect(axiosInstance.get).toHaveBeenCalledWith('/posts/1');
    });

    test('게시글 생성 성공 (18-19번 라인)', async () => {
        axiosInstance.post.mockResolvedValue({ data: 1 });
        await createPost({ title: 'test' });
        expect(axiosInstance.post).toHaveBeenCalledWith('/posts', { title: 'test' });
    });

    test('게시글 수정 성공 (24-25번 라인)', async () => {
        axiosInstance.patch.mockResolvedValue({ data: 'ok' });
        await updatePost(1, { title: 'hi' });
        expect(axiosInstance.patch).toHaveBeenCalledWith('/posts/1', { title: 'hi' });
    });

    test('게시글 삭제 성공 (30번 라인)', async () => {
        axiosInstance.delete.mockResolvedValue({});
        await deletePost(1);
        expect(axiosInstance.delete).toHaveBeenCalledWith('/posts/1');
    });

    test('미참조 파일 정리 성공 (35번 라인)', async () => {
        axiosInstance.delete.mockResolvedValue({});
        await cleanUpFiles();
        expect(axiosInstance.delete).toHaveBeenCalledWith('/posts/cleanup');
    });

    // --- 에러 케이스들 (5-6번, 63-64번 라인 공략) ---
    test('상세 조회 실패 시 에러 처리 (5-6번 라인)', async () => {
        axiosInstance.get.mockRejectedValue(new Error('Fail'));
        await expect(getPostById(1)).rejects.toThrow();
    });

    test('이미지 업로드 성공 및 실패 (40-64번 라인)', async () => {
        const mockLoader = { file: Promise.resolve(new File([''], 't.png')) };
        const adapter = imageUploadAdapter(mockLoader);

        // 성공 테스트
        axiosInstance.post.mockResolvedValue({ data: { url: 'url' } });
        const res = await adapter.upload();
        expect(res.default).toBe('url');

        // 실패 테스트 (63-64번 라인)
        axiosInstance.post.mockRejectedValue({ response: { data: { message: 'error' } } });
        await expect(adapter.upload()).rejects.toMatch('error');
    });
});