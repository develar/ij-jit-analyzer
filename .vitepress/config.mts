import { defineConfig } from "vitepress"

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "JIT Insights",
  srcDir: "pages",
  cleanUrls: true,
  description: "JIT compilation log analyzer",
  themeConfig: {
    sidebar: [
      {text: "Glossary", link: "/"},
      {text: "Spent Time", link: "/duration"},
      {
        text: "Timeline",
        link: "/timeline",
        items:[
          {text: "Timeline by Thread", link: "/timeline-by-thread"},
        ]
      },
    ]
  },
  vite: {
    ssr: {
      noExternal: ["echarts", "zrender"],
    },
  },
})
