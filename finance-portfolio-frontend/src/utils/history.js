

export const history = {
    navigate: null,
    push(url) {
        if (this.navigate) {
            this.navigate(url);
        } else {
            console.warn("Navigate is not initialized yet.");
            globalThis.location.href = url;
        }
    }
};