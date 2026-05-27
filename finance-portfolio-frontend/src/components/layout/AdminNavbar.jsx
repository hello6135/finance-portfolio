import React from 'react';
import { NavLink } from 'react-router-dom';

const AdminNavbar = () => {
    return (
        <div className="card-theme-custom border-0 sticky-top" style={{ zIndex: 1020 }}>
            <div className="container">
                <ul className="nav py-0 gap-1">
                    {/* 대시보드*/}
                    <li className="nav-item">
                        <NavLink
                            to="/admin/dashboard"
                            end
                            className={({ isActive }) =>
                                `nav-link btn-sm border-0 ${isActive ? 'bg-theme-custom' : 'card-theme-custom'}`
                            }
                        >
                            대시보드
                        </NavLink>
                    </li>
                    {/* 카테고리 관리 */}
                    <li className="nav-item">
                        <NavLink
                            to="/admin/categoryManager"
                            end
                            className={({ isActive }) =>
                                `nav-link btn-sm border-0 ${isActive ? 'bg-theme-custom' : 'card-theme-custom'}`
                            }
                        >
                            카테고리 관리
                        </NavLink>
                    </li>
                    {/* 회원 관리 */}
                    <li className="nav-item">
                        <NavLink
                            to="/admin/memberManager"
                            end
                            className={({ isActive }) =>
                                `nav-link btn-sm border-0 ${isActive ? 'bg-theme-custom' : 'card-theme-custom'}`
                            }
                        >
                            회원 관리
                        </NavLink>
                    </li>
                </ul>
            </div>
        </div>
    );
};

export default AdminNavbar;