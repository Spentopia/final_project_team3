import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { nodePolyfills } from 'vite-plugin-node-polyfills'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    nodePolyfills({ include: ['buffer', 'process'] }),
  ],
  resolve: {
    alias: [
      { find: '@', replacement: fileURLToPath(new URL('./src', import.meta.url)) },
    ],
  },
  define: {
    global: 'globalThis',
  },
  optimizeDeps: {
    include: ['buffer'],
  },
  server: {
    open: true,
  },
  assetsInclude: ['**/*.svg', '**/*.csv'],
})
