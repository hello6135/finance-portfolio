export const navRef = {
    navigate: null, // 여기에 useNavigate 훅이 담길 예정
};

export const history = {
    push(url) {
        if (navRef.navigate) {
            // 리액트 엔진이 로드된 상태라면 SPA 라우팅 실행 (깜빡임 X)
            navRef.navigate(url);
        } else {
            // 만약 앱 로딩 전이나 특수 상황이라면 브라우저 강제 이동 (Fallback)
            console.warn("Navigation 호출 시점에 리액트 엔진이 준비되지 않았습니다.");
            globalThis.location.href = url;
        }
    },

    goBack() {
        if (navRef.navigate) {
            navRef.navigate(-1); // 리액트 라우터의 뒤로 가기 방식
        } else {
            globalThis.history.back(); // 브라우저 네이티브 뒤로 가기
        }
    },

    replace(url) {
        if (navRef.navigate) {
            navRef.navigate(url, { replace: true });
        } else {
            globalThis.location.replace(url);
        }
    }
};