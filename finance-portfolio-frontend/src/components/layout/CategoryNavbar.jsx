import React, { useEffect, useState } from 'react';
import { NavLink, useLocation } from 'react-router';
import { getCategories } from '../../api/categoryApi';

const CategoryNavbar = () => {
    const [categories, setCategories] = useState([]);

    // 현재 경로 및 쿼리 정보
    const location = useLocation();
    // 현재 URL의 쿼리 스트링에서 category 값을 추출
    const queryParams = new URLSearchParams(location.search);
    const currentCategory = queryParams.get('category');

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
                    {/* 전체 보기: 쿼리 파라미터가 없는 기본 활성화 */}
                    <li className="nav-item">
                        <NavLink
                            to="/posts"
                            className={`nav-link btn-sm border-0 ${currentCategory ? 'card-theme-custom' : 'bg-theme-custom'}`}
                        >
                            전체
                        </NavLink>
                    </li>
                    {/* 동적 카테고리 메뉴 */}
                    {categories.map((category) => {
                        // 현재 카테고리 ID와 쿼리 스트링의 ID가 일치하는지 확인
                        const isCategoryActive = currentCategory === String(category.id);

                        return (
                            <li className="nav-item" key={category.id}>
                                <NavLink
                                    to={`/posts?category=${category.id}`}
                                    className={`nav-link btn-sm border-0 ${isCategoryActive ? 'bg-theme-custom' : 'card-theme-custom'}`}
                                >
                                    {category.name}
                                </NavLink>
                            </li>
                        );
                    })}
                </ul>
            </div>
        </div>
    );
};

export default CategoryNavbar;