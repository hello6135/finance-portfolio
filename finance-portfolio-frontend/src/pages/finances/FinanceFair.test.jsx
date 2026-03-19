import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import FairValueCalculator from './FinanceFair';
import * as financeApi from '../../api/financeApi';

vi.mock('../../api/financeApi');

describe('FairValueCalculator 컴포넌트 테스트', () => {

    beforeEach(() => {
        vi.clearAllMocks();
    });

    // 1. 입력값 반영
    it('사용자가 입력값을 넣으면 해당 값이 input에 반영되어야 한다', () => {
        render(<FairValueCalculator />);

        const dpsInput = screen.getByLabelText(/주당 배당금/);
        fireEvent.change(dpsInput, { target: { value: '1000' } });

        expect(dpsInput.value).toBe('1000');
    });

    // 2. 계산 성공 시 결과 출력
    it('계산하기 버튼을 누르면 API를 호출하고 결과를 화면에 출력해야 한다', async () => {
        const mockResponse = { fairValue: 50000, message: "적정 주가가 계산되었습니다." };
        financeApi.getFairValue.mockResolvedValue(mockResponse);

        render(<FairValueCalculator />);

        fireEvent.change(screen.getByLabelText(/주당 배당금/), { target: { value: '1000' } });
        fireEvent.change(screen.getByLabelText(/기대 수익률/), { target: { value: '7.0' } });
        fireEvent.change(screen.getByLabelText(/지속 성장률/), { target: { value: '5.0' } });

        fireEvent.click(screen.getByRole('button', { name: /계산하기/ }));

        await waitFor(() => {
            expect(screen.getByText(/결과: 50,000 원/)).toBeInTheDocument();
            expect(screen.getByText(/적정 주가가 계산되었습니다/)).toBeInTheDocument();
        });
    });

    // 3. 로딩 중 버튼 비활성화
    it('API 호출 도중에는 버튼이 "계산 중..."으로 바뀌고 비활성화되어야 한다', async () => {
        financeApi.getFairValue.mockReturnValue(new Promise(() => { }));

        render(<FairValueCalculator />);

        fireEvent.change(screen.getByLabelText(/주당 배당금/), { target: { value: '1000' } });
        fireEvent.change(screen.getByLabelText(/기대 수익률/), { target: { value: '7.0' } });
        fireEvent.change(screen.getByLabelText(/지속 성장률/), { target: { value: '5.0' } });

        fireEvent.click(screen.getByRole('button', { name: /계산하기/ }));

        await waitFor(() => {
            const btn = screen.getByRole('button', { name: /계산 중/ });
            expect(btn).toBeDisabled();
        });
    });

    // 4. API 실패 시 에러 처리
    it('API 호출 실패 시 에러 로그를 찍고 알림창(alert)을 띄워야 한다', async () => {
        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => { });
        const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => { });

        financeApi.getFairValue.mockRejectedValue(new Error('네트워크 에러'));

        render(<FairValueCalculator />);

        fireEvent.change(screen.getByLabelText(/주당 배당금/), { target: { value: '1000' } });
        fireEvent.change(screen.getByLabelText(/기대 수익률/), { target: { value: '7' } });
        fireEvent.change(screen.getByLabelText(/지속 성장률/), { target: { value: '5' } });

        fireEvent.click(screen.getByRole('button', { name: /계산하기/ }));

        await waitFor(() => {
            expect(consoleSpy).toHaveBeenCalledWith("계산 에러:", expect.any(Error));
            expect(alertSpy).toHaveBeenCalledWith("서버 통신 중 오류가 발생했습니다.");
        });

        consoleSpy.mockRestore();
        alertSpy.mockRestore();
    });
});