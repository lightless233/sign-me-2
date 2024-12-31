<script setup lang="tsx">
import { inject, onMounted, ref } from 'vue'
import { useMessage } from 'naive-ui'
import type { AxiosInstance } from 'axios'

interface RuleItemVO {
  ruleId: number
  ruleName: string
  ruleType: number
  filter: string
  status: boolean
  readableToolFlag: Array<string>
  toolFlag: number
  content: string
}

// 注入必要组件
const axios = inject('axios') as AxiosInstance
const message = useMessage()

// 定义事件
const emits = defineEmits<{
  newRule: [ruleType: string]
  loadRule: [ruleId: number]
}>()

// tableLoading 状态
const tableLoading = ref<boolean>(false)

// 表格数据
const ruleList = ref<RuleItemVO[]>([])

const newOpts = [
  {
    label: '专家规则',
    key: 'expert'
  },
  {
    label: '简单规则',
    key: 'simple'
  }
]
const handleNewRule = (key: string) => {
  // 抛出事件，告诉父组件触发了新建规则的操作，需要根据情况渲染表单了
  if (key === 'simple') {
    message.info('开发中...')
    return
  }
  emits('newRule', key)
}

// 获取规则列表
const fetchRuleList = async () => {
  tableLoading.value = true
  try {
    // 获取规则列表
    const resp = (await axios.get('/api/signRule/list')).data
    if (resp.code !== 2000) {
      message.error('获取规则列表失败，错误：' + resp.message)
      console.error('fetchRuleList error: ', resp)
    } else {
      // 规则列表填充进 table
      console.log('fetchRuleList: resp', resp)
      ruleList.value = resp.data
    }
  } catch (e) {
    message.error('获取规则列表失败')
    console.error('fetchRuleList error: ', e)
  } finally {
    tableLoading.value = false
  }
}

// 删除某条指定规则
const deleteRule = async (ruleId: number) => {
  console.log('deleteRule: ruleId', ruleId)
  try {
    const resp = (await axios.post('/api/signRule/delete', { ruleId })).data
    if (resp.code !== 2000) {
      message.error('删除规则失败，错误：' + resp.message)
      console.error('deleteRule error: ', resp)
    } else {
      message.success('删除规则成功')
      // 触发重新获取规则列表
      await fetchRuleList()
    }
  } catch (e) {
    message.error('删除规则失败')
    console.error('deleteRule error: ', e)
  }
}

// 修改规则状态
const toggleRuleStatus = async (ruleId: number, status: boolean) => {
  console.log('toggle: status', status)
  try {
    const resp = (await axios.post('/api/signRule/toggle-status', { ruleId, status })).data
    if (resp.code !== 2000) {
      message.error('修改规则状态失败，错误：' + resp.message)
      console.error('toggleRuleStatus error: ', resp)
    } else {
      message.success('修改规则状态成功')
    }
  } catch (e) {
    message.error('修改规则状态失败')
    console.error('toggleRuleStatus error: ', e)
  }
}

const loadRule = async (ruleId: number) => {
  console.log('loadRule: ruleId', ruleId)
  // 触发事件，告诉父组件加载指定规则的表单
  emits('loadRule', ruleId)
}

// 生命周期钩子
onMounted(async () => {
  await fetchRuleList()
})

</script>

<template>
  <n-card title="规则列表" size="small">

    <template #header-extra>
      <n-dropdown
        trigger="click"
        :options="[{label: '专家规则',key: 'expert'},{label: '简单规则',key: 'simple'}]"
        @select="handleNewRule"
      >
        <n-button type="primary" size="small">新建</n-button>
      </n-dropdown>
    </template>

    <!-- 填充规则列表 -->
    <n-table size="small" :bordered="false">
      <thead>
      <tr>
        <th>规则名称</th>
        <th>开关</th>
        <th>操作</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="rule in ruleList" @click.stop.prevent="loadRule">
        <td>{{ rule.ruleName }}</td>
        <td>
          <n-switch
            @click.stop.prevent
            v-model:value="rule.status"
            @update:value="async (status: boolean) => {await toggleRuleStatus(rule.ruleId, status)}" />
        </td>
        <td>
          <n-popconfirm
            positive-text="确认"
            negative-text="取消"
            @positive-click="deleteRule(rule.ruleId)"
            @click.stop.prevent
          >
            <template #trigger>
              <n-button text type="error" @click.prevent.stop>删除</n-button>
            </template>
            确认删除 `{{ rule.ruleName }}` 规则？
          </n-popconfirm>
        </td>
      </tr>
      </tbody>
    </n-table>

  </n-card>
</template>
