import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发配置：5174 端口，/api 请求代理到后端 8080
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    // 允许局域网 / 内网穿透访问（默认只监听 localhost）
    host: true,
    // 公网域名白名单：Vite 5.4.12+ 会拦截未知 Host，内网穿透的域名必须在此登记
    allowedHosts: [
      '30f9c13c.r30.cpolar.top',
      '1d2bae60.r30.cpolar.top',
      '367b79f2.r30.cpolar.top'
    ],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 房源图片由后端静态资源（static/images）提供，开发时一并代理
      '/images': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
