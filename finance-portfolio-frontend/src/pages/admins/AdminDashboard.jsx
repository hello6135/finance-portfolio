import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAwsStatus, getDashboardSummary } from '../../api/adminApi';
import LoadingPage from '../common/LoadingPage';

const AdminDashboard = () => {
    const navigate = useNavigate();

    const [dashboardData, setDashboardData] = useState(null);
    const [awsStatus, setAwsStatus] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        Promise.all([getDashboardSummary(), getAwsStatus()])
            .then(([dashboardRes, awsRes]) => {
                setDashboardData(dashboardRes);
                setAwsStatus(awsRes);
                setLoading(false);
            })
            .catch((err) => {
                console.error('데이터 로드 실패:', err);
                setError('데이터를 불러오는 중 오류가 발생했습니다.');
                setLoading(false);
            });
    }, []);

    const stats = [
        { title: '총 게시글', count: dashboardData?.totalPostCount ?? 0, unit: '개', color: 'text-primary' },
        { title: 'S3 오브젝트', count: dashboardData?.totalImageCount ?? 0, unit: '개', color: 'text-warning' },
        { title: '총 회원 수', count: dashboardData?.totalMemberCount ?? 0, unit: '명', color: 'text-success' },
        {
            title: "AWS S3",
            count: awsStatus?.s3?.status || "DOWN",
            unit: awsStatus?.s3?.status === "UP" ? "정상" : "점검필요",
            color: awsStatus?.s3?.status === "UP" ? "text-success" : "text-danger",
            isStatus: true
        },
        {
            title: "AWS EC2",
            count: awsStatus?.ec2?.instanceState?.toUpperCase() || "UNKNOWN",
            unit: awsStatus?.ec2?.status === "UP" ? "연결됨" : "연결안됨",
            color: awsStatus?.ec2?.instanceState === "running" ? "text-info" : "text-warning",
            isStatus: true
        },
    ];

    if (loading) return <LoadingPage />;

    return (
        <div className="container py-4">
            {/* 상단 헤더 */}
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h2 className="mb-0">관리자 대시보드</h2>
                    <p className="text-muted mb-0">시스템 현황 및 콘텐츠를 관리합니다.</p>
                </div>
                <button onClick={() => navigate('/')} className="btn btn-outline-secondary btn-sm">
                    사용자 메인으로
                </button>
            </div>

            {/* 통계 카드 섹션 */}
            <div className="row g-4 mb-5">
                {stats.map((item, index) => (
                    <div className="col-12 col-sm-6 col-lg-3" key={index}>
                        <div className="card border-0 shadow-sm h-100">
                            <div className="card-body">
                                <h6 className="card-subtitle mb-2 text-muted fw-bold">{item.title}</h6>
                                <div className={`h3 mb-0 fw-bold ${item.color}`}>
                                    {item.isStatus ? (
                                        <span>{item.count}</span>
                                    ) : (
                                        // 1000 단위 콤마
                                        <span>{Number(item.count).toLocaleString()}</span>
                                    )}
                                    <small className="fs-6 text-muted ms-1">{item.unit}</small>
                                </div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>

            {/* 관리 도구 섹션 */}
            <div className="row g-4">
                {/* 게시글 관리 요약 */}
                <div className="col-12 col-md-6">
                    <div className="card border-0 shadow-sm">
                        <div className="card-header bg-white fw-bold border-0 pt-3">
                            최근 게시글 현황
                        </div>
                        <div className="card-body">
                            <table className="table table-hover">
                                <thead className="table-light">
                                    <tr>
                                        <th>ID</th>
                                        <th>제목</th>
                                        <th>작성일</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td>128</td>
                                        <td>삼성전자 주가 분석...</td>
                                        <td>2026-04-22</td>
                                    </tr>
                                    <tr>
                                        <td>127</td>
                                        <td>포트폴리오 리밸런싱...</td>
                                        <td>2026-04-21</td>
                                    </tr>
                                </tbody>
                            </table>
                            <button className="btn btn-light w-100 btn-sm text-muted">전체 보기</button>
                        </div>
                    </div>
                </div>

                {/* 시스템 설정 가이드 */}
                <div className="col-12 col-md-6">
                    <div className="card border-0 shadow-sm bg-dark text-white">
                        <div className="card-body p-4">
                            <h5 className="fw-bold mb-3">시스템 관리 도구</h5>
                            <div className="d-grid gap-2">
                                <button className="btn btn-outline-light text-start border-secondary">
                                    <i className="bi bi-person-gear me-2"></i> 회원 권한 설정
                                </button>
                                <button className="btn btn-outline-light text-start border-secondary">
                                    <i className="bi bi-shield-check me-2"></i> 보안 로그 확인
                                </button>
                                <button className="btn btn-outline-light text-start border-secondary">
                                    <i className="bi bi-cloud-arrow-up me-2"></i> S3 스토리지 정리
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AdminDashboard;