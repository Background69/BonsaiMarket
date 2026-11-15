// Sensitive draft fields stay in memory. Reload recovery uses the existing server quote.
let draft = null;
export function rememberCheckoutDraft(userId, value) {
    draft = userId ? {userId, ...value} : null;
}
export function checkoutDraft(userId) {
    return draft?.userId === userId ? draft : null;
}
export function clearCheckoutDraft() { draft = null; }
