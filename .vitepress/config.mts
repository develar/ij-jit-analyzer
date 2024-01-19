import { defineConfig } from "vitepress"

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "JIT Insights",
  srcDir: "pages",
  cleanUrls: true,
  description: "JIT compilation log analyzer",
  themeConfig: {
    socialLinks: [
      {icon: "github", link: "https://github.com/develar/ij-jit-analyzer"},
    ],
    search: {
      provider: "local"
    },
    sidebar: [
      {text: "Glossary", link: "/"},
      {text: "Spent Time", link: "/duration"},
      {
        items: [
          {text: "Timeline", link: "/timeline"},
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
