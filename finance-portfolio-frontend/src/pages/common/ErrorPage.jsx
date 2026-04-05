import { useNavigate, useSearchParams, useLocation } from 'react-router-dom';


// prop으로 값 수신
const ErrorPage = ({ status: propStatus, message: propMessage }) => {

    const navigate = useNavigate();
    // 엔드포인트로 값 수신
    const [searchParams] = useSearchParams();
    // 메모리로 값 수신
    const location = useLocation();

    // 엔드포인트(인터셉터) || props(라우트) || 메모리(네비게이트) || 기본 문구
    const status = searchParams.get('status') || location.state?.status || propStatus || '오류';
    const message = searchParams.get('message') || location.state?.message || propMessage || '알 수 없는 오류가 발생했습니다.';

    console.log(searchParams.get('status'));
    console.log(location.state?.status);

    return (
        <div className="container mt-5 text-center">
            <h2>{status}</h2>
            <p>{message}</p>
            <button onClick={() => navigate('/')} className="btn btn-secondary">
                홈으로
            </button>
        </div>
    );
};

export default ErrorPage;