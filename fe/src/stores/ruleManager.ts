import { defineStore } from 'pinia'
import type { AxiosInstance } from 'axios'
import { inject, ref } from 'vue'
import type { RuleItemVO } from '@/types'

export const useRuleManagerStore = defineStore('ruleManagerStore', () => {
  const axios = inject('axios') as AxiosInstance
  const ruleList = ref<RuleItemVO[]>([])

  // 获取规则列表
  const fetchRuleList = async () => {
    const resp = (await axios.get('/api/signRule/list')).data
    if (resp.code !== 2000) {
      throw new Error(`${resp.code} - ${resp.message}`)
    } else {
      ruleList.value = resp.data
    }
  }

  // 删除某条指定的规则
  const deleteRule = async (ruleId: number) => {
    const resp = (await axios.post('/api/signRule/delete', { ruleId })).data
    if (resp.code !== 2000) {
      throw new Error(`${resp.code} - ${resp.message}`)
    }
  }

  // 修改规则状态
  const toggleRuleStatus = async (ruleId: number, status: boolean) => {
    const resp = (await axios.post('/api/signRule/toggle-status', { ruleId, status })).data
    if (resp.code !== 2000) {
      throw new Error(`${resp.code} - ${resp.message}`)
    }
  }

  return { ruleList, fetchRuleList, deleteRule, toggleRuleStatus }
})
