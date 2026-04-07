import React, { useState } from 'react';
import { getFairValue } from '../../api/financeApi';

const FairValueCalculator = () => {
    // 1. 입력 데이터 상태 관리 (RequestDto 구조와 일치)
    const [form, setForm] = useState({
        dps: '',
        expectedYield: '',
        growthRate: ''
    });

    // 2. 응답 데이터 상태 관리 (ResponseDto 구조와 일치)
    const [result, setResult] = useState(null);
    const [loading, setLoading] = useState(false);

    // 입력값 변경 시 호출되는 핸들러
    const handleChange = (e) => {
        const { name, value } = e.target;
        setForm({ ...form, [name]: value });
    };

    // 3. 계산 실행 (비즈니스 로직 호출)
    const handleCalculate = async (e) => {
        e.preventDefault(); // 폼 제출 시 페이지 새로고침 방지
        setLoading(true);

        try {
            // 숫자로 변환하여 DTO 전송
            const requestDto = {
                dps: Number(form.dps),
                expectedYield: Number(form.expectedYield),
                growthRate: Number(form.growthRate)
            };

            const data = await getFairValue(requestDto);
            setResult(data); // { fairValue, message } 저장
        } catch (error) {
            console.error("계산 에러:", error);
            alert("서버 통신 중 오류가 발생했습니다.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="card shadow-sm mb-4">
            <div className="card-header bg-primary text-white">
                <h5 className="mb-0">📈 적정주가 계산기 (고든 성장 모델)</h5>
            </div>
            <div className="card-body">
                <form onSubmit={handleCalculate}>
                    <div className="mb-3">
                        <label htmlFor="dps" className="form-label">주당 배당금 (DPS)</label>
                        <div className="input-group">
                            <input id="dps" type="number" name="dps" className="form-control"
                                value={form.dps} onChange={handleChange} required placeholder="예: 5000" />
                            <span className="input-group-text">원</span>
                        </div>
                    </div>

                    <div className="mb-3">
                        <label htmlFor="expectedYield" className="form-label">기대 수익률 (%)</label>
                        <input id="expectedYield" type="number" name="expectedYield" step="0.1" className="form-control"
                            value={form.expectedYield} onChange={handleChange} required placeholder="예: 8.5" />
                    </div>

                    <div className="mb-3">
                        <label htmlFor="growthRate" className="form-label">지속 성장률 (%)</label>
                        <input id="growthRate" type="number" name="growthRate" step="0.1" className="form-control"
                            value={form.growthRate} onChange={handleChange} required placeholder="예: 2.0" />
                    </div>

                    <button type="submit" className="btn btn-primary w-100" disabled={loading}>
                        {loading ? (
                            <>
                                <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                                계산 중...
                            </>
                        ) : '계산하기'}
                    </button>
                </form>

                {/* 결과 출력 영역 */}
                {result && (
                    <div className="mt-4 p-3 bg-light border rounded text-center">
                        <h6 className="text-muted mb-2">계산된 적정 주가</h6>
                        <h3 className="text-primary fw-bold mb-1">
                            {result.fairValue.toLocaleString()} <small className="text-dark">원</small>
                        </h3>
                        <p className="small text-secondary mb-0 mt-2">{result.message}</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default FairValueCalculator;