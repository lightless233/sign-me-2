import { defineStore } from 'pinia'
import type { AxiosInstance } from 'axios'
import { inject, ref } from 'vue'
import type { ExpertRuleFormType, RuleItemVO } from '@/types'

export const useRuleManagerStore = defineStore('ruleManagerStore', () => {
    const axios = inject('axios') as AxiosInstance

    // 存储规则列表
    const ruleList = ref<RuleItemVO[]>([])

    // 当前选中的、或者说是需要展示的规则类型，用于渲染 ExpertRuleForm 或者 SimpleRuleForm
    const currentRuleType = ref<string>('')

    // 专家规则的表单数据
    const expertRuleFormData = ref<ExpertRuleFormType>({
      id: -1,
      name: '',
      filter: '',
      status: true,
      toolFlag: ['TOOL_PROXY', 'TOOL_REPEATER'],
      content: `function main() {
    // More see: https://github.com/lightless233/sign-me-2
    const ts = utils.getTimestamp();
    return [
        {name: "timestamp", value: ts, location: ParameterType.HEADER, action: EditAction.OVERRIDE},
        {name: "sign", value: utils.md5(ts), location: ParameterType.PARAM_URL, action: EditAction.OVERRIDE},
    ]
}`
    })

    // 重置专家规则表单数据
    const resetExpertRuleFormData = () => {
      expertRuleFormData.value = {
        id: -1,
        name: '',
        filter: '',
        status: true,
        toolFlag: ['TOOL_PROXY', 'TOOL_REPEATER'],
        content: `function main() {
    // More see: https://github.com/lightless233/sign-me-2
    const ts = utils.getTimestamp();
    return [
        {name: "timestamp", value: ts, location: ParameterType.HEADER, action: EditAction.OVERRIDE},
        {name: "sign", value: utils.md5(ts), location: ParameterType.PARAM_URL, action: EditAction.OVERRIDE},
    ]
}`
      }
    }

    // 根据规则 ID 获取规则详情
    const getRuleById = async (ruleId: number) => {
      const resp = (await axios.get('/api/signRule/get_by_id', { params: { ruleId } })).data
      if (resp.code !== 2000) {
        throw new Error(`${resp.code} - ${resp.message}`)
      } else {
        const rule = resp.data
        expertRuleFormData.value = {
          id: rule.ruleId,
          name: rule.ruleName,
          filter: rule.filter,
          status: rule.status,
          toolFlag: rule.readableToolFlag,
          content: rule.content
        }
      }
    }

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

    // 创建一条新的规则
    const createNewRule = async () => {
      const payload = {
        ruleName: expertRuleFormData.value.name,
        filter: expertRuleFormData.value.filter,
        content: expertRuleFormData.value.content,
        status: expertRuleFormData.value.status,
        toolFlag: expertRuleFormData.value.toolFlag.join(','),
        ruleType: 1  // 专家规则
      }
      const resp = (await axios.post('/api/signRule/create', payload)).data
      if (resp.code !== 2000) {
        throw new Error(`${resp.code} - ${resp.message}`)
      } else {
        expertRuleFormData.value.id = resp.data.ruleId
      }
    }

    // 根据规则 ID 更新一条规则
    const updateRule = async () => {
      const payload = {
        ruleId: expertRuleFormData.value.id,
        ruleName: expertRuleFormData.value.name,
        filter: expertRuleFormData.value.filter,
        content: expertRuleFormData.value.content,
        status: expertRuleFormData.value.status,
        toolFlag: expertRuleFormData.value.toolFlag.join(','),
        ruleType: 1  // 专家规则
      }

      const resp = (await axios.post('/api/signRule/update', payload)).data
      if (resp.code !== 2000) {
        throw new Error(`${resp.code} - ${resp.message}`)
      } else {
        expertRuleFormData.value.id = resp.data.ruleId
      }
    }

    // 保存规则
    const saveRule = async () => {
      console.log(`[ruleManagerStore] saveRule called, ruleId: ${expertRuleFormData.value.id}`)
      if (expertRuleFormData.value.id === -1) {
        await createNewRule()
      } else {
        await updateRule()
      }

      await fetchRuleList()
    }

    return {
      ruleList, expertRuleFormData: expertRuleFormData, currentRuleType,
      fetchRuleList, deleteRule, toggleRuleStatus, saveRule, getRuleById, resetExpertRuleFormData
    }
  }
)
