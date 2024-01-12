import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "JIT Insights",
  description: "JIT compilation log analyzer",
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    nav: [
      { text: 'Home', link: '/' },
    ],
  },
  vite: {
    build: {
      // https://github.com/mapbox/node-pre-gyp/issues/661
      rollupOptions: {
        external: ["node:mock-aws-s3", "node:nock", "node:aws-sdk", "node:@mapbox/node-pre-gyp"],
      }
    }
  },
})
