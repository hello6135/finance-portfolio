export const navRef = {
    navigate: null, // 여기에 useNavigate 훅이 담길 예정
};

export const history = {
    push(url, state = null) {
        if (navRef.navigate) {
            navRef.navigate(url, state ? { state } : undefined);
        } else {
            console.warn("Navigation 호출 시점에 리액트 엔진이 준비되지 않았습니다.");
            globalThis.location.href = url;
        }
    },

    goBack() {
        if (navRef.navigate) {
            navRef.navigate(-1);
        } else {
            globalThis.history.back();
        }
    },

    replace(url, state = null) {
        if (navRef.navigate) {
            navRef.navigate(url, { replace: true, ...(state && { state }) });
        } else {
            globalThis.location.replace(url);
        }
    }
};