import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('/node_modules/naive-ui/')) {
            return 'naive-ui';
          }
          if (id.includes('/node_modules/@css-render/')) {
            return 'naive-ui';
          }
          if (
            id.includes('/node_modules/vue/') ||
            id.includes('/node_modules/@vue/') ||
            id.includes('/node_modules/pinia/')
          ) {
            return 'vue-runtime';
          }
          return undefined;
        },
      },
    },
  },
});
