import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { joinMember } from '../../api/memberApi';

const JoinPage = () => {
    const navigate = useNavigate();
    const [form, setForm] = useState({ loginId: '', password: '', nickname: '' });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
        setError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        if (form.password.length < 4) {
            setError('비밀번호는 최소 4자 이상이어야 합니다.');
            setLoading(false);
            return;
        }

        try {
            const { status, message } = await joinMember(form);
            if (status === 200 || status === 201) {
                alert(message);
                navigate('/login');
            }
        } catch (error) {
            const status = error.response?.status;
            const errorData = error.response?.data; // message 대신 data라는 이름을 주로 씁니다.

            let errorMessage = '서버 오류가 발생했습니다..'; // 기본 메시지

            if (status >= 400 && status < 500) {
                // 1. 서버가 객체로 보냈을 경우 ({ message: "..." })
                if (errorData && typeof errorData === 'object' && errorData.message) {
                    errorMessage = errorData.message;
                }
                // 2. 서버가 단순 문자열로 보냈을 경우 ("이미 존재하는...")
                else if (typeof errorData === 'string') {
                    errorMessage = errorData;
                }
            }

            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="d-flex align-items-center justify-content-center flex-grow-1">
            <div className="w-100" style={{ maxWidth: '420px', padding: '0 1rem' }}>
                {/* 회원가입 카드 */}
                <div className="card shadow card-theme-custom">
                    <div className="card-body p-4">
                        <h4 className="card-title text-center mb-1">회원가입</h4>

                        <hr />

                        <form onSubmit={handleSubmit}>
                            <div className="mb-3">
                                <label htmlFor="loginId" className="form-label">아이디</label>
                                <input
                                    id="loginId"
                                    type="text"
                                    name="loginId"
                                    className="form-control"
                                    value={form.loginId}
                                    onChange={handleChange}
                                    placeholder="아이디를 입력하세요"
                                    required
                                    autoFocus
                                />
                            </div>

                            <div className="mb-3">
                                <label htmlFor="password" className="form-label">
                                    비밀번호
                                    <span className="text-muted ms-1" style={{ fontSize: '0.8rem' }}>
                                        (4자 이상)
                                    </span>
                                </label>
                                <input
                                    id="password"
                                    type="password"
                                    name="password"
                                    className="form-control"
                                    value={form.password}
                                    onChange={handleChange}
                                    placeholder="비밀번호를 입력하세요"
                                    required
                                />
                            </div>

                            <div className="mb-3">
                                <label htmlFor="nickname" className="form-label">닉네임</label>
                                <input
                                    id="nickname"
                                    type="text"
                                    name="nickname"
                                    className="form-control"
                                    value={form.nickname}
                                    onChange={handleChange}
                                    placeholder="닉네임을 입력하세요"
                                    required
                                />
                            </div>

                            {error && (
                                <div className="alert alert-danger py-2" role="alert">
                                    {error}
                                </div>
                            )}

                            <button
                                type="submit"
                                className="btn btn-primary w-100"
                                disabled={loading}
                            >
                                {loading ? '처리 중...' : '회원가입'}
                            </button>
                        </form>

                        <hr />

                        <p className="text-center mb-0">
                            이미 계정이 있으신가요?{' '}
                            <button
                                className="btn btn-link p-0 align-baseline"
                                onClick={() => navigate('/login')}
                            >
                                로그인
                            </button>
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default JoinPage;