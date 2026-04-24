import React, { useState, useEffect } from 'react';
// 작성하신 API 함수들을 import 한다고 가정합니다.
import { getCategories, createCategory, updateCategory, deleteCategory } from '../../api/categoryApi';

const AdminCategoryManager = () => {
    const [categories, setCategories] = useState([]);
    const [isLoading, setIsLoading] = useState(false);

    // 입력 폼 상태
    const [formData, setFormData] = useState({ name: '', sortOrder: 0 });
    const [editingId, setEditingId] = useState(null);

    // 카테고리 목록 GET
    const fetchCategories = async () => {
        setIsLoading(true);
        try {
            const data = await getCategories();
            // order 순으로 정렬하여 표시
            const sortedData = data.sort((a, b) => a.sortOrder - b.sortOrder);
            setCategories(sortedData);
        } catch (error) {
            console.error("카테고리 로드 실패:", error);
            alert("카테고리 목록을 불러오는 데 실패했습니다.");
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchCategories();
    }, []);

    // 입력 핸들러
    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: name === 'sortOrder' ? Number.parseInt(value) || 0 : value
        }));
    };

    // 생성 및 수정 처리
    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!formData.name.trim()) return;

        try {
            if (editingId) {
                await updateCategory(editingId, formData);
            } else {
                await createCategory(formData);
            }
            // 폼 초기화 및 새로고침
            setFormData({ name: '', sortOrder: 0 });
            setEditingId(null);
            fetchCategories();
        } catch (error) {
            console.error("저장 실패:", error);
            alert("저장에 실패했습니다.");
        }
    };

    // 수정 모드 진입
    const handleEdit = (category) => {
        setEditingId(category.id);
        setFormData({ name: category.name, sortOrder: category.sortOrder });
    };

    // 삭제 처리
    const handleDelete = async (id) => {
        if (!globalThis.confirm("정말 삭제하시겠습니까?")) return;

        try {
            await deleteCategory(id);
            fetchCategories();
        } catch (error) {
            console.error("삭제 실패:", error);
            alert("삭제에 실패했습니다.");
        }
    };

    // 카테고리 목록 렌더
    const renderContent = () => {
        // 1. 로딩 중인 경우
        if (isLoading) {
            return (
                <tr>
                    <td colSpan="3" className="text-center py-4">로딩 중...</td>
                </tr>
            );
        }

        // 2. 데이터가 없는 경우
        if (categories.length === 0) {
            return (
                <tr>
                    <td colSpan="3" className="text-center py-4">등록된 카테고리가 없습니다.</td>
                </tr>
            );
        }

        // 3. 정상 데이터 출력
        return categories.map((category) => (
            <tr key={category.id}>
                <td>{category.sortOrder}</td>
                <td><strong>{category.name}</strong></td>
                <td className="text-center">
                    <button
                        className="btn btn-outline-secondary btn-sm me-2"
                        onClick={() => handleEdit(category)}
                    >
                        수정
                    </button>
                    <button
                        className="btn btn-outline-danger btn-sm"
                        onClick={() => handleDelete(category.id)}
                    >
                        삭제
                    </button>
                </td>
            </tr>
        ));
    };

    return (
        <div className="container py-4">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h2 className="mb-0">카테고리 관리</h2>
            </div>

            {/* 입력 폼 섹션 */}
            <div className="card mb-5">
                <div className="card-body">
                    <form onSubmit={handleSubmit} className="row g-3 align-items-end">
                        <div className="col-md-5">
                            <label htmlFor="categoryName" className="form-label">카테고리 명</label>
                            <input
                                id="categoryName"
                                type="text"
                                name="name"
                                className="form-control"
                                value={formData.name}
                                onChange={handleInputChange}
                                placeholder="예: 공지사항"
                                required
                            />
                        </div>
                        <div className="col-md-3">
                            <label htmlFor="categorySortOrder" className="form-label">노출 순서</label>
                            <input
                                id="categorySortOrder"
                                type="number"
                                name="sortOrder"
                                className="form-control"
                                value={formData.sortOrder}
                                onChange={handleInputChange}
                                required
                            />
                        </div>
                        <div className="col-md-4">
                            <button type="submit" className={`btn ${editingId ? 'btn-warning' : 'btn-primary'} w-100`}>
                                {editingId ? '수정 완료' : '새 카테고리 추가'}
                            </button>
                            {editingId && (
                                <button
                                    type="button"
                                    className="btn btn-link btn-sm w-100 mt-1"
                                    onClick={() => { setEditingId(null); setFormData({ name: '', sortOrder: 0 }); }}
                                >
                                    취소
                                </button>
                            )}
                        </div>
                    </form>
                </div>
            </div>

            {/* 목록 섹션 */}
            <div className="table-responsive">
                <table className="table table-hover align-middle">
                    <thead className="table-light">
                        <tr>
                            <th style={{ width: '10%' }}>순서</th>
                            <th style={{ width: '60%' }}>카테고리 이름</th>
                            <th style={{ width: '30%' }} className="text-center">관리</th>
                        </tr>
                    </thead>
                    <tbody>
                        {/* 카테고리 목록 렌더 */}
                        {renderContent()}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default AdminCategoryManager;