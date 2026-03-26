import { useNavigate } from 'react-router-dom';

const ErrorPage = ({ status, message }) => {
    const navigate = useNavigate();

    return (
        <div className="container mt-5 text-center">
            <h2>{status || '오류'}</h2>
            <p>{message || '알 수 없는 오류가 발생했습니다.'}</p>
            <button onClick={() => navigate('/')} className="btn btn-secondary">
                홈으로
            </button>
        </div>
    );
};

export default ErrorPage;