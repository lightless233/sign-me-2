import { defineStore } from 'pinia'
import type { AxiosInstance } from 'axios'
import { inject, ref } from 'vue'
import { useMessage } from 'naive-ui'
import type { RuleItemVO } from '@/types'

export const useRuleManagerStore = defineStore('ruleManagerStore', () => {
  const axios = inject('axios') as AxiosInstance
  const message = useMessage()
  const ruleList = ref<RuleItemVO[]>([])

  // 获取规则列表
  const fetchRuleList = async () => {
    try {
      const resp = (await axios.get('/rule/list')).data
      if (resp.code !== 2000) {
        message.error('获取规则列表失败，错误：' + resp.message)
        console.error('获取规则列表失败，错误：', resp.message)
      }
    } catch (e) {
      message.error('获取规则列表失败，错误：' + JSON.stringify(e))
      console.error('获取规则列表失败，错误: ', e)
    }
  }

  return { ruleList, fetchRuleList }
})
