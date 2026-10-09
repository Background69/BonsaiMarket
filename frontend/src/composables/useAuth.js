import {computed} from 'vue';
import {router, usePage} from '@inertiajs/vue3';
import {setApiToken, getApiToken} from '../services/api.js';

export function useAuth() {
    const page = usePage();

    // Single Source of Truth: Inertia's session data
    const user = computed(() => page.props.auth?.user || null);
    const isAuthenticated = computed(() => !!user.value);
    const role = computed(() => user.value?.role || null);

    const isBuyer = computed(() => role.value === 'buyer');
    const isSeller = computed(() => role.value === 'seller');
    const isAdmin = computed(() => role.value === 'admin');
    const isEmployee = computed(() => role.value === 'employee');

    function syncFromPageProps() {
        const pageToken = page.props.auth?.api_token;
        const currentToken = getApiToken();

        if (pageToken && pageToken !== currentToken) {
            setApiToken(pageToken);
        } else if (!user.value && currentToken) {
            setApiToken(null);
        }
    }

    function logout() {
        setApiToken(null);
        router.post('/logout');
    }

    return {
        user,
        isAuthenticated,
        role,
        isBuyer,
        isSeller,
        isAdmin,
        isEmployee,
        logout,
        syncFromPageProps,
    };
}
