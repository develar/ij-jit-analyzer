import { defineConfig } from "vitepress"

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "JIT Insights",
  srcDir: "pages",
  cleanUrls: true,
  description: "JIT compilation log analyzer",
  themeConfig: {
    search: {
      provider: "local"
    },
    sidebar: [
      {text: "Glossary", link: "/"},
      {text: "Spent Time", link: "/duration"},
      {
        text: "Timeline",
        link: "/timeline",
        items: [
          {text: "Timeline by Thread (count)", link: "/timeline-by-thread"},
          {text: "Timeline by Thread (size)", link: "/timeline-by-thread-size"},
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
