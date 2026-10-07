export function useToast() {
    return {
        add(message) {
            window.dispatchEvent(new CustomEvent('bonsai:toast', {detail: message.summary || 'Chức năng đang được phát triển.'}));
        }
    };
}
