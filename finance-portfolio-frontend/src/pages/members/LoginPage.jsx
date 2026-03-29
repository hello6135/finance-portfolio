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
            const message = error.response?.data;

            setError(message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container mt-5">
            <div className="row justify-content-center">
                <div className="col-12 col-sm-8 col-md-5">
                    <div className="card shadow-sm">
                        <div className="card-body p-4">
                            <h3 className="card-title text-center mb-1">📈 Finance Portfolio</h3>
                            <p className="text-center text-muted mb-4">
                                로그인하여 포트폴리오를 관리하세요
                            </p>

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
                                    <div className="alert alert-danger py-2" role="alert">
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

                            <p className="text-center text-muted mb-0">
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
        </div>
    );
};

export default LoginPage;