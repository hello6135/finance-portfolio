import axiosInstance from './axios';
import { getFairValue } from './financeApi';

jest.mock('./axios');

describe('getFairValue API 테스트', () => {
    const mockRequest = {
        dps: 1000,
        growthRate: 0.05,
        expectedYield: 0.1
    };

    test('성공 시 적정 가치 데이터를 반환해야 한다', async () => {
        const mockResponse = { fairValue: 21000, message: "성공" };
        axiosInstance.post.mockResolvedValue({ data: mockResponse });

        const result = await getFairValue(mockRequest);

        // API 호출 경로와 파라미터가 맞는지 검증
        expect(axiosInstance.post).toHaveBeenCalledWith('/fin/fair', mockRequest);
        expect(result).toEqual(mockResponse);
    });

    test('실패 시 에러를 throw 해야 한다', async () => {
        const errorMessage = "네트워크 에러";
        axiosInstance.post.mockRejectedValue(new Error(errorMessage));

        await expect(getFairValue(mockRequest)).rejects.toThrow(errorMessage);
    });
});