// https://nuxt.com/docs/api/configuration/nuxt-config
import {defineNuxtConfig} from "nuxt/config";

// noinspection HttpUrlsUsage
export default defineNuxtConfig({
  devtools: {enabled: true},
  modules: [
    '@element-plus/nuxt'
  ],
  routeRules: {
    // "/": {redirect: "/rule-manager", /**prerender: false**/}
  },
  runtimeConfig: {
    public: {
      baseURL: process.env.BASE_URL || "http://localhost:3336/"
    }
  },
  ssr: false
})
