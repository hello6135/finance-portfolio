import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, test, expect, beforeEach } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import LoginPage from './LoginPage';

// axios 모킹
vi.mock('../../api/axios', () => ({
    default: { post: vi.fn() },
}));

// authStore 모킹
vi.mock('../../api/authStore', () => ({
    default: { setToken: vi.fn() },
}));

// navigate 모킹
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

describe('LoginPage 컴포넌트 테스트', () => {

    beforeEach(() => {
        vi.clearAllMocks();
    });

    const fillForm = (loginId = 'testId', password = 'test1234') => {
        fireEvent.change(screen.getByLabelText('아이디'), { target: { value: loginId } });
        fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: password } });
    };

    // 1. 렌더링
    test('로그인 폼이 정상적으로 렌더링된다', () => {
        render(<BrowserRouter><LoginPage /></BrowserRouter>);

        expect(screen.getByLabelText('아이디')).toBeInTheDocument();
        expect(screen.getByLabelText('비밀번호')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: '로그인' })).toBeInTheDocument();
    });

    // 2. 입력값 변경
    test('입력값 변경 시 폼 상태가 업데이트된다', () => {
        render(<BrowserRouter><LoginPage /></BrowserRouter>);

        fireEvent.change(screen.getByLabelText('아이디'), { target: { value: 'testId' } });
        expect(screen.getByLabelText('아이디').value).toBe('testId');
    });

    // 3. 입력 시 에러 메시지 초기화
    test('입력값 변경 시 에러 메시지가 사라진다', async () => {
        const axiosInstance = (await import('../../api/axios')).default;
        axiosInstance.post.mockRejectedValue({ response: { status: 400 } });

        render(<BrowserRouter><LoginPage /></BrowserRouter>);
        fillForm();
        fireEvent.click(screen.getByRole('button', { name: '로그인' }));

        expect(await screen.findByText('아이디 또는 비밀번호가 올바르지 않습니다.')).toBeInTheDocument();

        fireEvent.change(screen.getByLabelText('아이디'), { target: { value: 'newId' } });
        expect(screen.queryByText('아이디 또는 비밀번호가 올바르지 않습니다.')).not.toBeInTheDocument();
    });

    // 4. 로그인 성공 - 토큰 저장 및 페이지 이동
    test('로그인 성공 시 토큰을 저장하고 게시판으로 이동한다', async () => {
        const axiosInstance = (await import('../../api/axios')).default;
        const authStore = (await import('../../api/authStore')).default;
        axiosInstance.post.mockResolvedValue({
            headers: { authorization: 'Bearer myAccessToken' },
        });

        render(<BrowserRouter><LoginPage /></BrowserRouter>);
        fillForm();
        fireEvent.click(screen.getByRole('button', { name: '로그인' }));

        await waitFor(() => {
            expect(authStore.setToken).toHaveBeenCalledWith('myAccessToken');
            expect(mockNavigate).toHaveBeenCalledWith('/posts');
        });
    });

    // 5. 로그인 성공 - Authorization 헤더 없는 경우
    test('Authorization 헤더가 없으면 setToken을 호출하지 않고 페이지만 이동한다', async () => {
        const axiosInstance = (await import('../../api/axios')).default;
        const authStore = (await import('../../api/authStore')).default;
        axiosInstance.post.mockResolvedValue({ headers: {} });

        render(<BrowserRouter><LoginPage /></BrowserRouter>);
        fillForm();
        fireEvent.click(screen.getByRole('button', { name: '로그인' }));

        await waitFor(() => {
            expect(authStore.setToken).not.toHaveBeenCalled();
            expect(mockNavigate).toHaveBeenCalledWith('/posts');
        });
    });

    // 6. 400 에러
    test('400 에러 시 아이디/비밀번호 오류 메시지를 표시한다', async () => {
        const axiosInstance = (await import('../../api/axios')).default;
        axiosInstance.post.mockRejectedValue({ response: { status: 400 } });

        render(<BrowserRouter><LoginPage /></BrowserRouter>);
        fillForm();
        fireEvent.click(screen.getByRole('button', { name: '로그인' }));

        expect(await screen.findByText('아이디 또는 비밀번호가 올바르지 않습니다.')).toBeInTheDocument();
    });

    // 7. 401 에러
    test('401 에러 시 인증 실패 메시지를 표시한다', async () => {
        const axiosInstance = (await import('../../api/axios')).default;
        axiosInstance.post.mockRejectedValue({ response: { status: 401 } });

        render(<BrowserRouter><LoginPage /></BrowserRouter>);
        fillForm();
        fireEvent.click(screen.getByRole('button', { name: '로그인' }));

        expect(await screen.findByText('인증에 실패했습니다. 다시 시도해주세요.')).toBeInTheDocument();
    });

    // 8. 서버 오류
    test('서버 오류 시 안내 메시지를 표시한다', async () => {
        const axiosInstance = (await import('../../api/axios')).default;
        axiosInstance.post.mockRejectedValue({ response: { status: 500 } });

        render(<BrowserRouter><LoginPage /></BrowserRouter>);
        fillForm();
        fireEvent.click(screen.getByRole('button', { name: '로그인' }));

        expect(await screen.findByText('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.')).toBeInTheDocument();
    });

    // 9. 로딩 중 버튼 비활성화
    test('API 호출 중에는 버튼이 "로그인 중..."으로 바뀌고 비활성화된다', async () => {
        const axiosInstance = (await import('../../api/axios')).default;
        axiosInstance.post.mockReturnValue(new Promise(() => { }));

        render(<BrowserRouter><LoginPage /></BrowserRouter>);
        fillForm();
        fireEvent.click(screen.getByRole('button', { name: '로그인' }));

        await waitFor(() => {
            expect(screen.getByRole('button', { name: '로그인 중...' })).toBeDisabled();
        });
    });

    // 10. 회원가입 페이지 이동 버튼
    test('회원가입 버튼 클릭 시 회원가입 페이지로 이동한다', () => {
        render(<BrowserRouter><LoginPage /></BrowserRouter>);

        fireEvent.click(screen.getByRole('button', { name: '회원가입' }));
        expect(mockNavigate).toHaveBeenCalledWith('/join');
    });
});