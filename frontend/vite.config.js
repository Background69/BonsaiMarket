import vue from '@vitejs/plugin-vue'
import {defineConfig} from 'vite'
import {fileURLToPath, URL} from 'node:url'

// https://vite.dev/config/
export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@inertiajs/vue3': fileURLToPath(new URL('./src/legacy/navigation.js', import.meta.url)),
            'ziggy-js': fileURLToPath(new URL('./src/legacy/route.js', import.meta.url)),
            'primevue/usetoast': fileURLToPath(new URL('./src/legacy/toast.js', import.meta.url)),
            'primevue/toast': fileURLToPath(new URL('./src/legacy/Toast.vue', import.meta.url)),
            '@agriverse/Layouts': fileURLToPath(new URL('./src/layouts', import.meta.url)),
            '@agriverse/Components': fileURLToPath(new URL('./src/components', import.meta.url)),
            '@agriverse/Composables': fileURLToPath(new URL('./src/composables', import.meta.url)),
            '@agriverse/Pages': fileURLToPath(new URL('./src/pages', import.meta.url)),
            '@agriverse': fileURLToPath(new URL('./src', import.meta.url)),
            '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
    },
    server: {
        proxy: {'/api': 'http://localhost:8080'},
    },
})
