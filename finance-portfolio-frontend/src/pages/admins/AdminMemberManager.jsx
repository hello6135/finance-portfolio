import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router';
import { getAdminMembers, toggleMemberBan, unlockMemberAccount } from '../../api/adminApi';
import LoadingPage from '../common/LoadingPage';

const AdminMemberManager = () => {
    const navigate = useNavigate();
    const [memberPage, setMemberPage] = useState({ content: [], totalPages: 0, number: 0 });
    const [isInitialLoading, setIsInitialLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false); // 가드 상태 일치
    const [currentPage, setCurrentPage] = useState(0);

    // 회원 목록 GET
    const fetchMembers = useCallback(async (page, showGlobalLoading = false) => {
        if (showGlobalLoading) setIsInitialLoading(true);
        try {
            const data = await getAdminMembers(page, 10);
            setMemberPage(data);
        } catch (error) {
            console.error("회원 목록 로드 실패:", error);
            alert("회원 목록을 불러오는 데 실패했습니다.");
            navigate('/login');
        } finally {
            setIsInitialLoading(false);
        }
    }, []);

    // 메모리 누수 방지 및 초기 로드 트랙킹
    useEffect(() => {
        let isMounted = true;
        if (isMounted) {
            fetchMembers(currentPage, true);
        }
        return () => {
            isMounted = false;
        };
    }, [currentPage, fetchMembers]);

    // 회원 정지 / 해제 처리
    const handleBanToggle = async (memberId, currentBanStatus) => {
        if (isSubmitting) return;
        const actionText = currentBanStatus ? "해제" : "정지";
        if (!globalThis.confirm(`해당 회원을 정말 ${actionText}하시겠습니까?`)) return;

        setIsSubmitting(true);
        try {
            await toggleMemberBan(memberId, !currentBanStatus);
            alert(`${actionText}되었습니다.`);
            await fetchMembers(currentPage);
        } catch (error) {
            console.error("정지 상태 변경 실패:", error);
            alert("상태 변경에 실패했습니다.");
        } finally {
            setIsSubmitting(false);
        }
    };

    // 잠금 계정 수동 해제 처리
    const handleUnlock = async (memberId) => {
        if (isSubmitting) return;
        if (!globalThis.confirm("이 계정의 로그인 실패 횟수를 초기화하고 잠금을 해제하시겠습니까?")) return;

        setIsSubmitting(true);
        try {
            await unlockMemberAccount(memberId);
            alert("계정 잠금이 해제되었습니다.");
            await fetchMembers(currentPage);
        } catch (error) {
            console.error("잠금 해제 실패:", error);
            alert("잠금 해제에 실패했습니다.");
        } finally {
            setIsSubmitting(false);
        }
    };

    // 공통 가드 조건 적용
    if (isInitialLoading) return <LoadingPage />;
    if (!memberPage?.content) return null;

    const renderContent = () => {
    if (memberPage.content.length === 0) {
        return (
            <tr>
                <td colSpan="6" className="text-center py-4">가입된 회원이 없습니다.</td>
            </tr>
        );
    }

    return (
        <>
            {memberPage.content.map((member) => (
                <tr key={member.id} className={member.isBanned ? 'table-light text-muted' : ''}>
                    <td>{member.id}</td>
                    <td>{member.loginId}</td>
                    <td><strong>{member.nickname}</strong></td>
                    <td>
                        <span className={`badge ${member.role === 'ADMIN' ? 'bg-danger' : 'bg-primary'}`}>
                            {member.role}
                        </span>
                    </td>
                    <td>
                        {member.loginFailCount >= 5 ? (
                            <div className="d-flex align-items-center gap-2">
                                <span className="badge bg-warning text-dark">잠김 ({member.loginFailCount}회)</span>
                                <button
                                    type="button"
                                    className="btn btn-sm btn-outline-warning"
                                    onClick={() => handleUnlock(member.id)}
                                    disabled={isSubmitting}
                                >
                                    해제
                                </button>
                            </div>
                        ) : (
                            <span className="text-secondary">{member.loginFailCount} / 5</span>
                        )}
                    </td>
                    <td className="text-center">
                        {member.role === 'ADMIN' ? (
                            <span className="text-muted small">-</span>
                        ) : (
                            <button
                                type="button"
                                className={`btn btn-sm ${member.isBanned ? 'btn-outline-success' : 'btn-outline-danger'}`}
                                onClick={() => handleBanToggle(member.id, member.isBanned)}
                                disabled={isSubmitting}
                            >
                                {member.isBanned ? '정지 해제' : '회원 정지'}
                            </button>
                        )}
                    </td>
                </tr>
            ))}
        </>
    );
};

    return (
        <div className="container py-4">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h2 className="mb-0">회원 관리</h2>
            </div>

            <div className="table-responsive card mb-4">
                <table className="table table-hover align-middle mb-0">
                    <thead className="table-light">
                        <tr>
                            <th style={{ width: '10%' }}>No</th>
                            <th style={{ width: '25%' }}>아이디</th>
                            <th style={{ width: '25%' }}>닉네임</th>
                            <th style={{ width: '15%' }}>권한</th>
                            <th style={{ width: '15%' }}>로그인 실패</th>
                            <th style={{ width: '10%' }} className="text-center">제어</th>
                        </tr>
                    </thead>
                    <tbody>
                        {renderContent()}
                    </tbody>
                </table>
            </div>

            {/* 페이지네이션 인터페이스 */}
            {memberPage.totalPages > 1 && (
                <nav className="d-flex justify-content-center mt-4">
                    <ul className="pagination shadow-sm">
                        <li className={`page-item ${currentPage === 0 ? 'disabled' : ''}`}>
                            <button
                                type="button"
                                className="page-link"
                                onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                                disabled={isSubmitting}
                            >
                                이전
                            </button>
                        </li>
                        {[...new Array(memberPage.totalPages).keys()].map((pageIndex) => (
                            <li key={pageIndex} className={`page-item ${currentPage === pageIndex ? 'active' : ''}`}>
                                <button
                                    type="button"
                                    className="page-link"
                                    onClick={() => setCurrentPage(pageIndex)}
                                    disabled={isSubmitting}
                                >
                                    {pageIndex + 1}
                                </button>
                            </li>
                        ))}
                        <li className={`page-item ${currentPage === memberPage.totalPages - 1 ? 'disabled' : ''}`}>
                            <button
                                type="button"
                                className="page-link"
                                onClick={() => setCurrentPage(prev => Math.min(memberPage.totalPages - 1, prev + 1))}
                                disabled={isSubmitting}
                            >
                                다음
                            </button>
                        </li>
                    </ul>
                </nav>
            )}
        </div>
    );
};

export default AdminMemberManager;