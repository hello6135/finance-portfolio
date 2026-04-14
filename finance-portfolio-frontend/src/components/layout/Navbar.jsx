import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import authStore from '../../store/authStore';
import { logoutMember } from '../../api/memberApi';


const Navbar = () => {
    const navigate = useNavigate();

    const onLogout = async () => {
        try {
            // 백엔드에 로그아웃 요청 (Refresh Token DB 삭제 + 쿠키 만료)
            const { status, message } = await logoutMember();
            if (status == 200 || status == 201) {
                alert(message);
            }
        } catch (error) {
            const message = error.response?.data || "로그아웃 중 오류가 발생했습니다."
            alert(message);
        } finally {
            // 로그아웃은 에러가 나도 클라이언트 상태는 정리
            authStore.clearToken();
            navigate('/', { replace: true });
        }
    };

    // 활성화 된 페이지 표시
    const navLinkClass = ({ isActive }) => {
        return `nav-link text-nowrap ${isActive ? 'text-white fw-bold' : 'text-secondary'}`;
    }

    return (
        <nav className="navbar navbar-expand-sm navbar-dark card-theme-custom fixed-top shadow">
            <div className="container">
                {/* 브랜드 로고 */}
                <NavLink to="/" className="navbar-brand fw-bold">
                    Finance Portfolio
                </NavLink>

                {/* 햄버거 버튼 (모바일용) */}
                <button
                    className="navbar-toggler"
                    type="button"
                    data-bs-toggle="collapse"
                    data-bs-target="#navbarNav"
                    aria-controls="navbarNav"
                    aria-expanded="false"
                    aria-label="Toggle navigation"
                >
                    <span className="navbar-toggler-icon"></span>
                </button>

                {/* 메뉴 영역 */}
                <div className="collapse navbar-collapse" id="navbarNav">
                    <ul className="navbar-nav me-auto gap-2 ">
                        <li className="nav-item">
                            <NavLink to="/posts" className={navLinkClass}>
                                게시판
                            </NavLink>
                        </li>
                        <li className="nav-item">
                            <NavLink to="/finance/fair" className={navLinkClass}>
                                금융계산기
                            </NavLink>
                        </li>
                        <li className="nav-item">
                            <NavLink to="/admin" className={navLinkClass}>
                                관리자
                            </NavLink>
                        </li>
                    </ul>

                    <br />

                    {/* 우측 로그인 세션 */}
                    <div className="d-flex align-items-center gap-3">
                        {authStore.isLoggedIn() ? (
                            <>
                                <span className="badge bg-secondary px-2 py-2 fw-normal text-nowrap">
                                    사용자님
                                </span>
                                <button
                                    onClick={onLogout}
                                    className="btn btn-outline-light btn-sm px-2 text-nowrap"
                                >
                                    로그아웃
                                </button>
                            </>
                        ) : (
                            <button
                                onClick={() => navigate('/login')}
                                className="btn btn-primary btn-sm px-2 text-nowrap"
                            >
                                로그인
                            </button>
                        )}
                    </div>
                </div>
            </div>
        </nav>
    );
};

export default Navbar;