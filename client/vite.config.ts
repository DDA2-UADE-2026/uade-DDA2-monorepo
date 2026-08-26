import { defineConfig, loadEnv, type PluginOption } from 'vite'
import react, { reactCompilerPreset } from '@vitejs/plugin-react'
import babel from '@rolldown/plugin-babel'
import { tanstackRouter } from '@tanstack/router-plugin/vite'
import tailwindcss from '@tailwindcss/vite'
import { qrcode } from 'vite-plugin-qrcode';
import { visualizer } from 'rollup-plugin-visualizer'
import { devtools } from "@tanstack/devtools-vite"

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, import.meta.dirname, '')

  return {
    plugins: [
      devtools(),
      tailwindcss(),
      tanstackRouter({
        target: 'react',
        autoCodeSplitting: true,
        quoteStyle: "double",
      }),
      react(),
      babel({ presets: [reactCompilerPreset()] }),
      qrcode({
        filter: (url) => url.startsWith('http://192.168.0')
      }),
      ...(mode === 'visualizer'
        ? [
            visualizer({
              filename: 'dist/bundle-report.html',
              template: 'treemap',
              open: true,
              gzipSize: true,
              brotliSize: true,
            }) as PluginOption,
          ]
        : []),
    ],
    resolve: {
      alias: {
        "@": `${import.meta.dirname}/src`,
      },
    },
    server: {
      proxy: env.VITE_PROXY_URL
        ? {
            '/proxy': {
              target: env.VITE_PROXY_URL,
              changeOrigin: true,
              rewrite: (path) => path.replace(/^\/proxy/, ''),
              configure: (proxy) => {
                proxy.on('proxyReq', (proxyReq, req) => {
                  proxyReq.removeHeader('origin')
                  console.log(`[proxy] -> ${req.method} ${req.url}`)
                })
                proxy.on('proxyRes', (proxyRes, req) => {
                  console.log(`[proxy] <- ${proxyRes.statusCode} ${req.method} ${req.url}`)
                })
                proxy.on('error', (err, req) => {
                  console.error(`[proxy] error on ${req.method} ${req.url}:`, err.message)
                })
              },
            },
          }
        : undefined,
    },
  }
})
