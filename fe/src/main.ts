import {createApp} from 'vue'
import {createPinia} from 'pinia'
import axios from 'axios'

import "./index.css"
import App from './App.vue'
import router from './router'

// 通用字体
import 'vfonts/Lato.css'
// 等宽字体
import 'vfonts/FiraCode.css'


const app = createApp(App)

app.use(createPinia())
app.use(router)

// 配置 Axios
const axiosInstance = axios.create({
  withCredentials: true,
  timeout: 9 * 1000,
  timeoutErrorMessage: "E_NETWORK_TIMEOUT",
  // baseURL: "/api/"
})
app.provide("axios", axiosInstance)

app.mount('#app')
