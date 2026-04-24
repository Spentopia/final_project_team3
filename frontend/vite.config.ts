import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  resolve: {
    alias: [
      { find: '@', replacement: fileURLToPath(new URL('./src', import.meta.url)) },
      { find: /^buffer$/, replacement: 'buffer/' },
      { find: /^node:buffer$/, replacement: 'buffer/' },
    ],
  },
  define: {
    global: 'globalThis',
  },
  optimizeDeps: {
    include: ['buffer', 'buffer/'],
  },
  server: {
    open: true,
  },
  assetsInclude: ['**/*.svg', '**/*.csv'],
})
