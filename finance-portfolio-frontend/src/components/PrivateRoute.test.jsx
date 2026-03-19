import { vi, describe, test, expect, beforeEach } from 'vitest';
import axiosInstance from '../api/axios';
import authStore from '../api/authStore';

vi.mock('../api/authStore', () => ({
    default: {
        getToken: vi.fn(),
        setToken: vi.fn(),
        clearToken: vi.fn(),
    },
}));

const mockAdapter = vi.fn();
axiosInstance.defaults.adapter = mockAdapter;

describe('axios 인터셉터 테스트', () => {

    beforeEach(() => {
        vi.clearAllMocks();
        authStore.getToken.mockReturnValue(null);
    });

    // ── 요청 인터셉터 ──────────────────────────────────────────

    test('Access Token이 있으면 Authorization 헤더에 Bearer 토큰을 첨부한다', async () => {
        authStore.getToken.mockReturnValue('myAccessToken');
        mockAdapter.mockResolvedValue({ status: 200, data: {}, headers: {} });

        await axiosInstance.get('/posts');

        const sentConfig = mockAdapter.mock.calls[0][0];
        expect(sentConfig.headers['Authorization']).toBe('Bearer myAccessToken');
    });

    test('Access Token이 없으면 Authorization 헤더를 첨부하지 않는다', async () => {
        mockAdapter.mockResolvedValue({ status: 200, data: {}, headers: {} });

        await axiosInstance.get('/posts');

        const sentConfig = mockAdapter.mock.calls[0][0];
        expect(sentConfig.headers['Authorization']).toBeUndefined();
    });

    // ── 응답 인터셉터: /auth/reissue 예외 처리 ────────────────

    test('/auth/reissue 실패 시 재발급 시도 없이 에러를 throw한다', async () => {
        const reissueError = {
            config: { url: '/auth/reissue' },
            response: { status: 401, data: {} },
        };
        mockAdapter.mockRejectedValue(reissueError);

        await expect(axiosInstance.post('/auth/reissue')).rejects.toEqual(reissueError);
        expect(mockAdapter).toHaveBeenCalledTimes(1);
    });

    // ── 응답 인터셉터: ACCESS_TOKEN_EXPIRED 재발급 ────────────

    test('ACCESS_TOKEN_EXPIRED 에러 시 /auth/reissue를 호출하고 원래 요청을 재시도한다', async () => {
        authStore.getToken.mockReturnValue('expiredToken');

        mockAdapter
            .mockRejectedValueOnce({
                config: { url: '/posts', headers: {}, _retry: false },
                response: { status: 401, data: { error: 'ACCESS_TOKEN_EXPIRED' } },
            })
            .mockResolvedValueOnce({
                status: 200,
                data: {},
                headers: { authorization: 'Bearer newAccessToken' },
            })
            .mockResolvedValueOnce({
                status: 200,
                data: { result: 'ok' },
                headers: {},
            });

        const response = await axiosInstance.get('/posts');

        expect(authStore.setToken).toHaveBeenCalledWith('newAccessToken');
        expect(response.data).toEqual({ result: 'ok' });
        expect(mockAdapter).toHaveBeenCalledTimes(3);
    });

    test('ACCESS_TOKEN_EXPIRED가 아닌 401은 재발급 시도 없이 에러를 throw한다', async () => {
        const error = {
            config: { url: '/posts', headers: {} },
            response: { status: 401, data: { error: 'UNAUTHORIZED' } },
        };
        mockAdapter.mockRejectedValue(error);

        await expect(axiosInstance.get('/posts')).rejects.toEqual(error);
        expect(mockAdapter).toHaveBeenCalledTimes(1);
    });

    // ── 응답 인터셉터: 재발급 실패 → 강제 로그아웃 ───────────

    test('재발급 실패 시 토큰을 삭제하고 로그인 페이지로 이동한다', async () => {
        let assignedHref = '';
        Object.defineProperty(globalThis, 'location', {
            value: { set href(v) { assignedHref = v; } },
            writable: true,
            configurable: true,
        });

        const originalError = {
            config: { url: '/posts', headers: {}, _retry: false },
            response: { status: 401, data: { error: 'ACCESS_TOKEN_EXPIRED' } },
        };

        mockAdapter
            .mockRejectedValueOnce(originalError)
            .mockRejectedValueOnce({
                config: { url: '/auth/reissue' },
                response: { status: 401, data: {} },
            });

        // catch 블록에서 reissueError가 아닌 외부 error를 throw하므로 originalError가 reject됩니다
        await expect(axiosInstance.get('/posts')).rejects.toMatchObject({
            config: { url: '/auth/reissue' },
            response: { status: 401, data: {} },
        });

        expect(authStore.clearToken).toHaveBeenCalled();
        expect(assignedHref).toBe('/login');
    });
});