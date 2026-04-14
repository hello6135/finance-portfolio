import React, { useEffect, useState } from 'react';
import { NavLink } from 'react-router-dom';
import { getCategories } from '../../api/categoryApi';

const CategoryNavbar = () => {
    const [categories, setCategories] = useState([]);

    useEffect(() => {
        const fetchCategories = async () => {
            try {
                const data = await getCategories();
                setCategories(data);
            } catch (error) {
                console.error("카테고리 로딩 실패:", error);
            }
        };
        fetchCategories();
    }, []);

    return (
        <div className="card-theme-custom border-0 sticky-top" style={{ zIndex: 1020 }}>
            <div className="container">
                <ul className="nav py-0 gap-1">
                    {/* 전체 보기 기본 메뉴 */}
                    <li className="nav-item">
                        <NavLink
                            to="/posts"
                            end
                            className={({ isActive }) =>
                                `nav-link btn-sm border-0 ${isActive ? 'bg-theme-custom' : 'card-theme-custom'}`
                            }
                        >
                            전체
                        </NavLink>
                    </li>
                    {/* 동적 카테고리 메뉴 */}
                    {categories.map((category) => (
                        <li className="nav-item" key={category.id}>
                            <NavLink
                                to={`/posts?category=${category.id}`}
                                className={({ isActive }) =>
                                    `nav-link btn-sm border-0 ${isActive ? 'bg-theme-custom' : 'card-theme-custom'}`
                                }
                            >
                                {category.name}
                            </NavLink>
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    );
};

export default CategoryNavbar;