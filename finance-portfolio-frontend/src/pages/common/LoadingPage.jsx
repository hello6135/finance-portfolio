
const LoadingPage = () => {
    return (
        <div className="d-flex align-items-center justify-content-center"
            style={{ minHeight: 'calc(100vh - 96px)' }}>
            <output className="spinner-border text-primary">
                <span className="visually-hidden">Loading...</span>
            </output>
        </div>
    );
};

export default LoadingPage;