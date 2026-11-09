import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// During development the React app runs on :5173 and forwards /api calls to Spring Boot on :8080,
// so the browser sees one origin and you need no CORS configuration.
// `npm run build` (also run by Maven) writes into target/classes/static, which Spring Boot serves on :8080.
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../target/classes/static',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
