import { defineConfig } from "vitepress"

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "JIT Insights",
  srcDir: "pages",
  cleanUrls: true,
  description: "JIT compilation log analyzer",
  themeConfig: {
    sidebar: [
      {text: "Introduction", link: "/"},
      {text: "Spent Time", link: "/duration"},
      {text: "Timeline", link: "/timeline"},
    ]
  },
  vite: {
    ssr: {
      noExternal: ["echarts", "zrender"],
    },
  },
})
