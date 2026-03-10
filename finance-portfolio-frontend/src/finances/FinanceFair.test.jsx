import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import FairValueCalculator from './FinanceFair';
import * as financeApi from '../api/financeApi'; // API 모킹을 위해 임포트

// financeApi의 getFairValue 함수를 가짜(Mock)로 만듭니다.
vi.mock('../api/financeApi');

describe('FairValueCalculator 컴포넌트 테스트', () => {

    it('사용자가 입력값을 넣으면 해당 값이 input에 반영되어야 한다', () => {
        render(<FairValueCalculator />);

        const dpsInput = screen.getByLabelText(/주당 배당금/);
        fireEvent.change(dpsInput, { target: { value: '1000' } });

        expect(dpsInput.value).toBe('1000');
    });

    it('계산하기 버튼을 누르면 API를 호출하고 결과를 화면에 출력해야 한다', async () => {
        // 1. API 응답 가짜 데이터 설정 (ResponseDto 구조)
        const mockResponse = { fairValue: 50000, message: "적정 주가가 계산되었습니다." };
        financeApi.getFairValue.mockResolvedValue(mockResponse);

        render(<FairValueCalculator />);

        // 2. 값 입력
        fireEvent.change(screen.getByLabelText(/주당 배당금/), { target: { value: '1000' } });
        fireEvent.change(screen.getByLabelText(/기대 수익률/), { target: { value: '7.0' } });
        fireEvent.change(screen.getByLabelText(/지속 성장률/), { target: { value: '5.0' } });

        // 3. 계산 버튼 클릭
        const submitButton = screen.getByRole('button', { name: /계산하기/ });
        fireEvent.click(submitButton);

        // 4. 결과 검증
        // 50,000원이 천 단위 콤마와 함께 표시되는지 확인
        await waitFor(() => {
            expect(screen.getByText(/결과: 50,000 원/)).toBeInTheDocument();
            expect(screen.getByText(/적정 주가가 계산되었습니다/)).toBeInTheDocument();
        });
    });

    it('API 호출 도중에는 버튼이 "계산 중..."으로 바뀌고 비활성화되어야 한다', async () => {
        // API 호출이 끝나지 않도록 지연시키는 가짜 Promise
        financeApi.getFairValue.mockReturnValue(new Promise(() => { }));

        render(<FairValueCalculator />);

        fireEvent.change(screen.getByLabelText(/주당 배당금/), { target: { value: '1000' } });
        fireEvent.change(screen.getByLabelText(/기대 수익률/), { target: { value: '7.0' } });
        fireEvent.change(screen.getByLabelText(/지속 성장률/), { target: { value: '5.0' } });

        const submitButton = screen.getByRole('button', { name: /계산하기/ });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(submitButton).toBeDisabled();
            expect(submitButton).toHaveTextContent('계산 중...');
        });

    });

    it('API 호출 실패 시 에러 로그를 찍고 알림창(alert)을 띄워야 한다', async () => {
        // 1. console.error와 alert가 실행되는지 감시(Spy)하고, 실제 실행은 막기
        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => { });
        const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => { });

        // 2. API가 에러를 던지도록 설정 (Reject 시나리오)
        financeApi.getFairValue.mockRejectedValue(new Error('네트워크 에러'));

        render(<FairValueCalculator />);

        // 3. 필수 입력값 채우기 (그래야 handleCalculate가 실행됨)
        fireEvent.change(screen.getByLabelText(/주당 배당금/), { target: { value: '1000' } });
        fireEvent.change(screen.getByLabelText(/기대 수익률/), { target: { value: '7' } });
        fireEvent.change(screen.getByLabelText(/지속 성장률/), { target: { value: '5' } });

        // 4. 계산 버튼 클릭
        fireEvent.click(screen.getByRole('button', { name: /계산하기/ }));

        // 5. 검증: console.error와 alert가 호출되었는가?
        await waitFor(() => {
            expect(consoleSpy).toHaveBeenCalledWith("계산 에러:", expect.any(Error));
            expect(alertSpy).toHaveBeenCalledWith("서버 통신 중 오류가 발생했습니다.");
        });

        // 6. 테스트 종료 후 원래 상태로 복구
        consoleSpy.mockRestore();
        alertSpy.mockRestore();
    });
});