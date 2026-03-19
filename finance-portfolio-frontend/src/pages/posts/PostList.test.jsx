import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, test, expect, beforeEach } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import PostList from './PostList';
import { getAllPosts, cleanUpFiles } from '../../api/postApi';

// API 모킹
vi.mock('../../api/postApi', () => ({
    getAllPosts: vi.fn(),
    cleanUpFiles: vi.fn(),
}));

// axios 모킹
vi.mock('../../api/axios', () => ({
    default: {
        post: vi.fn(),
    },
}));

// authStore 모킹
vi.mock('../../api/authStore', () => ({
    default: {
        clearToken: vi.fn(),
    },
}));

// navigate 모킹
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

describe('PostList 컴포넌트 테스트', () => {

    beforeEach(() => {
        vi.clearAllMocks();
    });

    // 1. 로딩 상태
    test('데이터 로딩 중에는 "로딩 중..." 메시지를 표시해야 한다', () => {
        getAllPosts.mockReturnValue(new Promise(() => { }));
        render(<BrowserRouter><PostList /></BrowserRouter>);
        expect(screen.getByText('로딩 중...')).toBeDefined();
    });

    // 2. 목록 렌더링
    test('게시글 목록을 성공적으로 불러와 화면에 표시한다', async () => {
        getAllPosts.mockResolvedValue([
            { id: 1, title: '테스트 게시글', hasImage: true, createdAt: new Date().toISOString() }
        ]);

        render(<BrowserRouter><PostList /></BrowserRouter>);

        expect(await screen.findByText(/테스트 게시글/)).toBeDefined();
        expect(screen.getByText('📷')).toBeDefined();
    });

    // 3. 빈 목록
    test('게시글이 없을 경우 "게시글이 없습니다." 문구를 표시한다', async () => {
        getAllPosts.mockResolvedValue([]);
        render(<BrowserRouter><PostList /></BrowserRouter>);

        expect(await screen.findByText('게시글이 없습니다.')).toBeDefined();
    });

    // 4. 데이터 호출 실패
    test('데이터 호출 실패 시 콘솔에 에러 로그를 남겨야 한다', async () => {
        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => { });
        getAllPosts.mockRejectedValue(new Error('API Error'));

        render(<BrowserRouter><PostList /></BrowserRouter>);

        await waitFor(() => {
            expect(consoleSpy).toHaveBeenCalledWith("데이터 호출 실패:", expect.any(Error));
        });
        consoleSpy.mockRestore();
    });

    // 5. 미참조 이미지 삭제 성공
    test('이미지 삭제 확인 시 cleanUpFiles API를 호출해야 한다', async () => {
        getAllPosts.mockResolvedValue([]);
        cleanUpFiles.mockResolvedValue({});
        vi.spyOn(globalThis, 'confirm').mockReturnValue(true);

        render(<BrowserRouter><PostList /></BrowserRouter>);

        const cleanupBtn = await screen.findByText('미참조 이미지 삭제');
        fireEvent.click(cleanupBtn);

        await waitFor(() => {
            expect(cleanUpFiles).toHaveBeenCalled();
        });
    });

    // 6. 미참조 이미지 삭제 실패
    test('이미지 삭제 실패 시 에러 로그와 alert를 띄워야 한다', async () => {
        getAllPosts.mockResolvedValue([]);
        cleanUpFiles.mockRejectedValue(new Error('Delete Fail'));
        vi.spyOn(globalThis, 'confirm').mockReturnValue(true);
        const alertSpy = vi.spyOn(globalThis, 'alert').mockImplementation(() => { });

        render(<BrowserRouter><PostList /></BrowserRouter>);

        const cleanupBtn = await screen.findByText('미참조 이미지 삭제');
        fireEvent.click(cleanupBtn);

        await waitFor(() => {
            expect(alertSpy).toHaveBeenCalledWith("삭제에 실패했습니다.");
        });
        alertSpy.mockRestore();
    });

    // 7. 새 글 버튼 이동
    test('새 글 버튼 클릭 시 에디터 페이지로 이동해야 한다', async () => {
        getAllPosts.mockResolvedValue([]);
        render(<BrowserRouter><PostList /></BrowserRouter>);

        const newPostBtn = await screen.findByText('새 글');
        fireEvent.click(newPostBtn);

        expect(mockNavigate).toHaveBeenCalledWith('/editor');
    });

    // 8. 로그아웃 성공
    test('로그아웃 클릭 시 토큰을 삭제하고 로그인 페이지로 이동한다', async () => {
        getAllPosts.mockResolvedValue([]);
        const axiosInstance = (await import('../../api/axios')).default;
        axiosInstance.post.mockResolvedValue({});

        render(<BrowserRouter><PostList /></BrowserRouter>);

        const logoutBtn = await screen.findByText('로그아웃');
        fireEvent.click(logoutBtn);

        await waitFor(() => {
            expect(mockNavigate).toHaveBeenCalledWith('/login');
        });
    });

    // 9. 로그아웃 실패해도 로그인 페이지로 이동
    test('로그아웃 API 실패해도 토큰을 삭제하고 로그인 페이지로 이동한다', async () => {
        getAllPosts.mockResolvedValue([]);
        const axiosInstance = (await import('../../api/axios')).default;
        axiosInstance.post.mockRejectedValue(new Error('logout fail'));

        render(<BrowserRouter><PostList /></BrowserRouter>);

        const logoutBtn = await screen.findByText('로그아웃');
        fireEvent.click(logoutBtn);

        await waitFor(() => {
            expect(mockNavigate).toHaveBeenCalledWith('/login');
        });
    });
});