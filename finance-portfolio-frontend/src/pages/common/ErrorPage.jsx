import { useNavigate, useSearchParams, useLocation } from 'react-router-dom';


// prop으로 값 수신
const ErrorPage = ({ status: propStatus, message: propMessage }) => {

    const navigate = useNavigate();
    // 엔드포인트로 값 수신
    const [searchParams] = useSearchParams();
    // 메모리로 값 수신
    const location = useLocation();

    console.log('Current Location Object:', location);
    console.log('State Message:', location.state?.message);

    // 엔드포인트(인터셉터) || 메모리(네비게이트) || props(라우트) || 기본 문구
    const status = searchParams.get('status') || location.state?.status || propStatus || 'Error';

    let rawMessage = location.state?.message || searchParams.get('message') || propMessage;

    let message = '알 수 없는 오류가 발생했습니다.';
    if (rawMessage) {
        try {
            message = decodeURIComponent(rawMessage);
        } catch {
            message = rawMessage;
        }
    }

    const subMessage = location.state?.subMessage;

    return (
        <div className="d-flex align-items-center justify-content-center"
            style={{ minHeight: 'calc(100vh - 56px)' }}>
            <div className="text-center">
                <h1 className="display-1 fw-bold text-muted">{status}</h1>
                <p className="fs-5 mb-4">{message}</p>
                {subMessage && <p className="text-muted small mb-4">{subMessage}</p>}
                <button onClick={() => navigate('/')} className="btn btn-primary">
                    홈으로
                </button>
            </div>
        </div>
    );
};

export default ErrorPage;