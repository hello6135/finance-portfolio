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
        <div style={{ padding: '20px', border: '1px solid #ccc', borderRadius: '8px' }}>
            <h3>📈 적정주가 계산기 (고든 성장 모델)</h3>
            <form onSubmit={handleCalculate}>
                <div style={{ marginBottom: '10px' }}>
                    <label htmlFor="dps">주당 배당금 (DPS): </label>
                    <input id="dps" type="number" name="dps" value={form.dps} onChange={handleChange} required />
                </div>
                <div style={{ marginBottom: '10px' }}>
                    <label htmlFor="expectedYield">기대 수익률 (%): </label>
                    <input id="expectedYield" type="number" name="expectedYield" step="0.1" value={form.expectedYield} onChange={handleChange} required />
                </div>
                <div style={{ marginBottom: '10px' }}>
                    <label htmlFor="growthRate">지속 성장률 (%): </label>
                    <input id="growthRate" type="number" name="growthRate" step="0.1" value={form.growthRate} onChange={handleChange} required />
                </div>
                <button type="submit" disabled={loading}>
                    {loading ? '계산 중...' : '계산하기'}
                </button>
            </form>

            {/* 4. 결과 출력 영역 (ResponseDto 활용) */}
            {result && (
                <div style={{ marginTop: '20px', padding: '10px', backgroundColor: '#444' }}>
                    <h4>결과: {result.fairValue.toLocaleString()} 원</h4>
                    <p style={{ fontSize: '0.9em', color: '#bbb' }}>{result.message}</p>
                </div>
            )}
        </div>
    );
};

export default FairValueCalculator;