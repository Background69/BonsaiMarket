import {apiGet, apiPost} from '../services/api.js';
import {usePage} from '../legacy/navigation.js';

// Backend roles -> role names the existing layout/header already understands.
const ROLE_MAP = {ADMIN: 'admin', SELLER: 'seller', CUSTOMER: 'buyer'};

function toViewUser(dto) {
    return dto ? {id: dto.id, name: dto.name, email: dto.email, emailVerified: dto.emailVerified, role: ROLE_MAP[dto.role] || 'buyer'} : null;
}

function setUser(user) {
    usePage().props.auth.user = user;
}

/** Restores the session after a page reload (the session cookie lives in the browser). */
export async function loadCurrentUser() {
    try {
        setUser(toViewUser(await apiGet('/auth/me')));
    } catch {
        setUser(null);
    }
}

export async function login(email, password) {
    const user = toViewUser(await apiPost('/auth/login', {email, password}));
    setUser(user);
    return user;
}

export async function logout() {
    try {
        await apiPost('/auth/logout');
    } finally {
        setUser(null);
    }
}

export const register = (name, email, password) => apiPost('/auth/register', {name, email, password});
export const verifyEmail = token => apiPost('/auth/verify-email', {token});
export const resendVerification = email => apiPost('/auth/resend-verification', {email});
