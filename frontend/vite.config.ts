import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
    plugins: [react()],
    server: {
        proxy: {
            '/api': {
                target: 'https://formatis.online:8080', // Адрес твоего Java Backend
                changeOrigin: true,
                secure: false,
            },
        },
    },
});