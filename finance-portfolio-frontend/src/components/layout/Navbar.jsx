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
                console.log(message);
            }
        } catch {
            // 로그아웃은 에러가 나도 클라이언트 상태는 정리합니다
        } finally {
            authStore.clearToken();
            navigate('/login');
        }
    };


    return (
        <nav className="navbar navbar-expand-lg navbar-dark bg-dark px-4"
            style={{}}>
            <span className="navbar-brand">Finance Portfolio Navigation</span>
            <div className="d-flex gap-3">
                <NavLink
                    to="/posts"
                    className={({ isActive }) =>
                        'nav-link' + (isActive ? ' text-white fw-bold' : ' text-secondary')
                    }
                >
                    게시판
                </NavLink>
                <NavLink
                    to="/finance/fair"
                    className={({ isActive }) =>
                        'nav-link' + (isActive ? ' text-white fw-bold' : ' text-secondary')
                    }
                >
                    금융계산기
                </NavLink>
            </div>

            {/* 로그인 관련 */}
            <div className="ms-auto d-flex align-items-center gap-3">
                {authStore.isLoggedIn() ? (
                    <>
                        <span className="text-light badge bg-secondary px-3 py-2">
                            `사용자`님 {/* authStore.nickname 교체 예정 */}
                        </span>
                        <button onClick={onLogout} className="btn btn-outline-light btn-sm">
                            로그아웃
                        </button>
                    </>
                ) : (
                    <>
                        <span className="text-secondary small">로그인이 필요합니다</span>
                        <button
                            onClick={() => navigate('/login')}
                            className="btn btn-primary btn-sm px-3"
                        >
                            로그인
                        </button>
                    </>
                )}
            </div>
        </nav>
    );
};

export default Navbar;