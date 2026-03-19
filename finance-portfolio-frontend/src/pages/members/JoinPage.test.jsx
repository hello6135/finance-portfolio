import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, test, expect, beforeEach } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import JoinPage from './JoinPage';

// axios 모킹
vi.mock('../../api/axios', () => ({
    default: { post: vi.fn() },
}));

// navigate 모킹
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

describe('JoinPage 컴포넌트 테스트', () => {

    beforeEach(() => {
        vi.clearAllMocks();
    });

    const fillForm = (loginId = 'testId', password = 'test1234', nickname = '테스터') => {
        fireEvent.change(screen.getByLabelText('아이디'), { target: { value: loginId } });
        fireEvent.change(screen.getByLabelText(/비밀번호/), { target: { value: password } });
        fireEvent.change(screen.getByLabelText('닉네임'), { target: { value: nickname } });
    };

    // 1. 렌더링
    test('회원가입 폼이 정상적으로 렌더링된다', () => {
        render(<BrowserRouter><JoinPage /></BrowserRouter>);

        expect(screen.getByLabelText('아이디')).toBeInTheDocument();
        expect(screen.getByLabelText(/비밀번호/)).toBeInTheDocument();
        expect(screen.getByLabelText('닉네임')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: '회원가입' })).toBeInTheDocument();
    });

    // 2. 입력값 변경
    test('입력값 변경 시 폼 상태가 업데이트된다', () => {
        render(<BrowserRouter><JoinPage /></BrowserRouter>);

        fireEvent.change(screen.getByLabelText('아이디'), { target: { value: 'testId' } });
        expect(screen.getByLabelText('아이디').value).toBe('testId');
    });

    // 3. 입력 시 에러 메시지 초기화
    test('입력값 변경 시 에러 메시지가 사라진다', async () => {
        render(<BrowserRouter><JoinPage /></BrowserRouter>);

        // 비밀번호 짧게 입력해서 에러 발생
        fillForm('testId', '123', '테스터');
        fireEvent.click(screen.getByRole('button', { name: '회원가입' }));

        expect(await screen.findByText('비밀번호는 최소 4자 이상이어야 합니다.')).toBeInTheDocument();

        // 다시 입력하면 에러 사라짐
        fireEvent.change(screen.getByLabelText(/비밀번호/), { target: { value: 'test1234' } });
        expect(screen.queryByText('비밀번호는 최소 4자 이상이어야 합니다.')).not.toBeInTheDocument();
    });

    // 4. 비밀번호 4자 미만 프론트 검증
    test('비밀번호가 4자 미만이면 에러 메시지를 표시하고 API를 호출하지 않는다', async () => {
        const axiosInstance = (await import('../../api/axios')).default;
        render(<BrowserRouter><JoinPage /></BrowserRouter>);

        fillForm('testId', '123', '테스터');
        fireEvent.click(screen.getByRole('button', { name: '회원가입' }));

        expect(await screen.findByText('비밀번호는 최소 4자 이상이어야 합니다.')).toBeInTheDocument();
        expect(axiosInstance.post).not.toHaveBeenCalled();
    });

    // 5. 회원가입 성공
    test('회원가입 성공 시 alert를 띄우고 로그인 페이지로 이동한다', async () => {
        const axiosInstance = (await import('../../api/axios')).default;
        axiosInstance.post.mockResolvedValue({});
        const alertSpy = vi.spyOn(globalThis, 'alert').mockImplementation(() => { });

        render(<BrowserRouter><JoinPage /></BrowserRouter>);
        fillForm();
        fireEvent.click(screen.getByRole('button', { name: '회원가입' }));

        await waitFor(() => {
            expect(alertSpy).toHaveBeenCalledWith('회원가입이 완료되었습니다. 로그인해주세요.');
            expect(mockNavigate).toHaveBeenCalledWith('/login');
        });

        alertSpy.mockRestore();
    });

    // 6. 400 에러 - 백엔드 메시지 표시 (중복 아이디 등)
    test('400 에러 시 백엔드 메시지를 화면에 표시한다', async () => {
        const axiosInstance = (await import('../../api/axios')).default;
        axiosInstance.post.mockRejectedValue({
            response: { status: 400, data: '이미 존재하는 아이디입니다.' },
        });

        render(<BrowserRouter><JoinPage /></BrowserRouter>);
        fillForm();
        fireEvent.click(screen.getByRole('button', { name: '회원가입' }));

        expect(await screen.findByText('이미 존재하는 아이디입니다.')).toBeInTheDocument();
    });

    // 7. 서버 오류
    test('서버 오류 시 안내 메시지를 표시한다', async () => {
        const axiosInstance = (await import('../../api/axios')).default;
        axiosInstance.post.mockRejectedValue({
            response: { status: 500, data: 'Internal Server Error' },
        });

        render(<BrowserRouter><JoinPage /></BrowserRouter>);
        fillForm();
        fireEvent.click(screen.getByRole('button', { name: '회원가입' }));

        expect(await screen.findByText('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.')).toBeInTheDocument();
    });

    // 8. 로딩 중 버튼 비활성화
    test('API 호출 중에는 버튼이 "처리 중..."으로 바뀌고 비활성화된다', async () => {
        const axiosInstance = (await import('../../api/axios')).default;
        axiosInstance.post.mockReturnValue(new Promise(() => { }));

        render(<BrowserRouter><JoinPage /></BrowserRouter>);
        fillForm();
        fireEvent.click(screen.getByRole('button', { name: '회원가입' }));

        await waitFor(() => {
            expect(screen.getByRole('button', { name: '처리 중...' })).toBeDisabled();
        });
    });

    // 9. 로그인 페이지 이동 버튼
    test('로그인 버튼 클릭 시 로그인 페이지로 이동한다', () => {
        render(<BrowserRouter><JoinPage /></BrowserRouter>);

        fireEvent.click(screen.getByRole('button', { name: '로그인' }));
        expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
});