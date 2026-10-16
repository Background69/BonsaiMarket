import {computed} from 'vue';
import {useAuth} from './useAuth.js';
import {sellerDestination} from '../navigation/marketplace.js';

export function useMarketplaceNavigation() {
    const {user, state} = useAuth();
    const navigationReady = computed(() => state.ready && !state.unavailable);
    const sellerEntry = computed(() => navigationReady.value ? sellerDestination(user.value) : null);
    return {navigationReady, sellerEntry};
}
