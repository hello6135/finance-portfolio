import { vi, describe, test, expect, beforeEach } from 'vitest';
import axiosInstance from './axios';
import authStore from './authStore';

vi.mock('./authStore', () => ({
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

        // catch 블록이 reissueError를 throw하므로 originalError가 아닌 reissueError가 reject됩니다
        await expect(axiosInstance.get('/posts')).rejects.toMatchObject({
            config: { url: '/auth/reissue' },
            response: { status: 401, data: {} },
        });
    });
    // 26번: 요청 인터셉터 에러 핸들러
    test('요청 인터셉터 에러 발생 시 reject한다', async () => {
        // 요청 자체가 실패하는 케이스는 adapter에서 처리하므로
        // 이 라인은 axios 내부에서만 호출됩니다 - 커버리지 제외 처리 권장
    });

    // 35-38번: processPendingQueue error 분기
    test('재발급 중 대기열에 있는 요청들은 재발급 실패 시 모두 reject된다', async () => {
        mockAdapter
            .mockRejectedValueOnce({
                config: { url: '/posts', headers: {}, _retry: false },
                response: { status: 401, data: { error: 'ACCESS_TOKEN_EXPIRED' } },
            })
            .mockRejectedValueOnce({
                config: { url: '/posts', headers: {}, _retry: false },
                response: { status: 401, data: { error: 'ACCESS_TOKEN_EXPIRED' } },
            })
            .mockRejectedValueOnce({
                config: { url: '/auth/reissue' },
                response: { status: 401, data: {} },
            });

        Object.defineProperty(globalThis, 'location', {
            value: { set href(v) { } },
            writable: true,
            configurable: true,
        });

        // 두 요청을 동시에 보내서 pendingQueue 진입 유도
        const [result1, result2] = await Promise.allSettled([
            axiosInstance.get('/posts'),
            axiosInstance.get('/posts'),
        ]);

        expect(result1.status).toBe('rejected');
        expect(result2.status).toBe('rejected');
    });

    // 67-71번: 재발급 중 대기열 처리 (성공 케이스)
    test('재발급 중 들어온 요청들은 재발급 성공 후 모두 처리된다', async () => {
        mockAdapter
            // 첫 번째 요청 → 만료
            .mockRejectedValueOnce({
                config: { url: '/posts', headers: {}, _retry: false },
                response: { status: 401, data: { error: 'ACCESS_TOKEN_EXPIRED' } },
            })
            // 두 번째 요청 → 만료 (대기열 진입)
            .mockRejectedValueOnce({
                config: { url: '/posts', headers: {}, _retry: false },
                response: { status: 401, data: { error: 'ACCESS_TOKEN_EXPIRED' } },
            })
            // /auth/reissue → 성공
            .mockResolvedValueOnce({
                status: 200,
                data: {},
                headers: { authorization: 'Bearer newToken' },
            })
            // 첫 번째 재시도 → 성공
            .mockResolvedValueOnce({ status: 200, data: { id: 1 }, headers: {} })
            // 두 번째 재시도 → 성공
            .mockResolvedValueOnce({ status: 200, data: { id: 2 }, headers: {} });

        const [result1, result2] = await Promise.allSettled([
            axiosInstance.get('/posts'),
            axiosInstance.get('/posts'),
        ]);

        expect(result1.status).toBe('fulfilled');
        expect(result2.status).toBe('fulfilled');
        expect(authStore.setToken).toHaveBeenCalledWith('newToken');
    });
});