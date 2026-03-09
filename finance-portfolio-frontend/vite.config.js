import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  },
  test: {
    globals: true,
    environment: 'jsdom', // 브라우저 환경 모사
    coverage: {
      provider: 'v8', // 커버리지 엔진
      reporter: ['text', 'lcov'], // SonarQube가 읽을 수 있도록 lcov 필수
    },
  }
})
