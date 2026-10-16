import {ref, onMounted, onBeforeUnmount} from 'vue';

// Independent Home sections retain successful data when another resource fails.
export function usePublicResource(load, initial = []) {
    const data = ref(initial), loading = ref(true), error = ref(false);
    let requestId = 0;
    async function retry() {
        const id = ++requestId;
        loading.value = true;
        error.value = false;
        try {
            const result = await load();
            if (id === requestId) data.value = result;
        } catch {
            if (id === requestId) error.value = true;
        } finally {
            if (id === requestId) loading.value = false;
        }
    }
    onMounted(retry);
    onBeforeUnmount(() => requestId++);
    return {data, loading, error, retry};
}
