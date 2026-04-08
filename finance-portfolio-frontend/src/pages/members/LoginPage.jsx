import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import authStore from '../../store/authStore';
import { loginMember } from '../../api/memberApi';


const LoginPage = () => {
    const navigate = useNavigate();
    const [form, setForm] = useState({ loginId: '', password: '' });
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

        try {
            const { status, message, token } = await loginMember(form);
            if ((status === 200 || status === 201) && token) {
                console.log(message);
                // 저장소에 토큰 저장
                authStore.setToken(token);

                navigate('/posts');
            }
        } catch (error) {
            // 객체 아닌 문자열만 추출
            const errorData = error.response?.data;
            const errorMessage = typeof errorData === 'object'
                ? errorData.message
                : (errorData || "로그인에 실패했습니다..");
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="d-flex align-items-center justify-content-center flex-grow-1">
            <div className="w-100" style={{ maxWidth: '420px', padding: '0 1rem' }}>
                {/* 로그인 카드 */}
                <div className="card shadow card-theme-custom">
                    <div className="card-body p-4">
                        <h4 className="card-title text-center mb-1" style={{ paddingBottom: '1rem' }}>로그인</h4>
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
                                <label htmlFor="password" className="form-label">비밀번호</label>
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

                            {error && (
                                <div className="alert alert-danger py-2 mb-3" role="alert">
                                    {error}
                                </div>
                            )}

                            <button
                                type="submit"
                                className="btn btn-primary w-100"
                                disabled={loading}
                            >
                                {loading ? '로그인 중...' : '로그인'}
                            </button>
                        </form>

                        <hr />

                        <p className="text-center mb-0" style={{ fontSize: '0.9rem' }}>
                            계정이 없으신가요?{' '}
                            <button
                                className="btn btn-link p-0 align-baseline"
                                onClick={() => navigate('/join')}
                            >
                                회원가입
                            </button>
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default LoginPage;