import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAwsStatus, getDashboardSummary } from '../../api/adminApi';
import { getCloudWatchLogs } from '../../api/cloudwatchApi';
import LoadingPage from '../common/LoadingPage';

const AdminDashboard = () => {
    const navigate = useNavigate();

    // 대시보드 인프라 상태 관리
    const [dashboardData, setDashboardData] = useState(null);
    const [awsStatus, setAwsStatus] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // 실시간 로그 모니터링 UI 상태 관리
    const [logs, setLogs] = useState([]);
    const [logsLoading, setLogsLoading] = useState(false);
    const [level, setLevel] = useState('ALL');
    const [search, setSearch] = useState('');

    // 통계 및 AWS 리소스 상태 로드
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

    // 추상화된 API 모듈을 호출하여 화면의 상태(State)만 갱신
    const fetchLogs = useCallback(async () => {
        setLogsLoading(true);
        try {
            const parsedLogs = await getCloudWatchLogs({ level, search });
            setLogs(parsedLogs);
        } catch (err) {
            console.error('대시보드 로그 연동 실패:', err);
        } finally {
            setLogsLoading(false);
        }
    }, [level, search]);

    // 인터랙션 필터 체인지 라이프사이클 훅
    useEffect(() => {
        fetchLogs();
    }, [fetchLogs]);

    const handleSearchSubmit = (e) => {
        e.preventDefault();
        fetchLogs();
    };

    const getLogLevelColor = (logLevel) => {
        switch (logLevel) {
            case 'ERROR': return 'text-danger fw-bold';
            case 'WARN': return 'text-warning fw-bold';
            case 'INFO': return 'text-info';
            default: return 'text-light';
        }
    };

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

    // 실시간 로그 콘솔 렌더링
    const renderLogContent = () => {
        if (logsLoading) {
            return <div className="text-center py-5">CloudWatch 연동 로그 로딩 중...</div>;
        }

        if (logs.length === 0) {
            return <div className="text-center py-5">해당 세그먼트 내역에 부합하는 클라우드워치 실시간 데이터가 존재하지 않습니다.</div>;
        }

        return (
        <>
            {logs.map((log) => (
                <div key={log.id} className="border-bottom border-secondary border-opacity-10 py-1">
                    <span className="me-2">[{log.timestamp}]</span>
                    <span className={`me-2 ${getLogLevelColor(log.level)}`}>[{log.level}]</span>
                    <span className="text-light text-break">{log.message}</span>
                </div>
            ))}
        </>
        );
    };

    if (loading) return <LoadingPage />;

    return (
        <div className="container py-4">
            {/* 상단 헤더 */}
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h2 className="mb-0">관리자 대시보드</h2>
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

            {error && <div className="alert alert-danger mb-4">{error}</div>}

            {/* 통합 실시간 로그 콘솔 레이아웃 */}
            <div className="card border-0 shadow-sm">
                <div className="card-header bg-white border-0 pt-3 pb-0 d-flex justify-content-between align-items-center">
                    <div>
                        <h5 className="fw-bold mb-0">실시간 시스템 로그 모니터링</h5>
                        <small className="text-muted">AWS CloudWatch를 통해 프론트엔드에서 직접 로그 스트림을 필터링 및 수집합니다.</small>
                    </div>
                    <button 
                        onClick={fetchLogs} 
                        className="btn btn-sm btn-outline-dark"
                        disabled={logsLoading}
                    >
                        {logsLoading ? '갱신 중...' : '실시간 동기화'}
                    </button>
                </div>
                
                <div className="card-body">
                    <form onSubmit={handleSearchSubmit} className="row g-2 align-items-center mb-3">
                        <div className="col-12 col-sm-3">
                            <select 
                                className="form-select form-select-sm" 
                                value={level} 
                                onChange={(e) => setLevel(e.target.value)}
                            >
                                <option value="ALL">ALL (전체 로그)</option>
                                <option value="INFO">INFO</option>
                                <option value="WARN">WARN</option>
                                <option value="ERROR">ERROR</option>
                            </select>
                        </div>
                        <div className="col-12 col-sm-7">
                            <input 
                                type="text" 
                                className="form-control form-control-sm" 
                                placeholder="검색 키워드를 입력하세요... (예: NullPointerException)" 
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                            />
                        </div>
                        <div className="col-12 col-sm-2">
                            <button type="submit" className="btn btn-dark btn-sm w-100">검색</button>
                        </div>
                    </form>

                    <div className="card bg-dark text-light border-0">
                        <div className="card-header bg-secondary bg-opacity-25 border-bottom border-secondary border-opacity-10 py-2">
                            <span className="font-monospace small">CloudWatch Stream: {import.meta.env.VITE_AWS_LOG_STREAM_NAME}</span>
                        </div>
                        <div className="card-body font-monospace p-3" style={{ minHeight: '300px', maxHeight: '450px', overflowY: 'auto', fontSize: '0.8rem', lineHeight: '1.4' }}>
                            {renderLogContent()}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AdminDashboard;