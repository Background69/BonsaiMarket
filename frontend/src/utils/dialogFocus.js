export function trapDialogFocus(event) {
    const dialog = event.currentTarget;
    const controls = [...dialog.querySelectorAll('button:not(:disabled),a[href],input:not(:disabled),select:not(:disabled),textarea:not(:disabled),[tabindex]:not([tabindex="-1"])')]
        .filter(control => control.getClientRects().length && !control.closest('[inert]'));
    const first = controls[0], last = controls.at(-1);
    if (!first) {event.preventDefault(); dialog.focus();}
    else if (!dialog.contains(document.activeElement)) {event.preventDefault(); first.focus();}
    else if (event.shiftKey && document.activeElement === first) {event.preventDefault(); last.focus();}
    else if (!event.shiftKey && document.activeElement === last) {event.preventDefault(); first.focus();}
}
