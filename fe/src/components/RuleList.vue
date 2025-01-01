<script setup lang="tsx">
import { inject, onMounted, ref } from 'vue'
import { useMessage } from 'naive-ui'
import type { AxiosInstance } from 'axios'
import type { RuleItemVO } from '@/types'
import { useRuleManagerStore } from '@/stores/ruleManager.ts'


// 注入必要组件
const axios = inject('axios') as AxiosInstance
const message = useMessage()

// 定义事件
const emits = defineEmits<{
  newRule: [ruleType: string]
  loadRule: [ruleId: number, ruleType: string]
}>()

// store
const store = useRuleManagerStore()

// 新建规则
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
  try {
    await store.fetchRuleList()
  } catch (e: any) {
    console.error('fetchRuleList error: ', e)
    message.error('获取规则列表失败，错误：' + e.message)
  }
}

// 删除某条指定规则
const deleteRule = async (ruleId: number) => {
  try {
    await store.deleteRule(ruleId)
    message.success('删除规则成功')
    // 如果删除成功了，重新加载规则列表
    await store.fetchRuleList()
  } catch (e: any) {
    console.error('deleteRule error: ', e)
    message.error('删除规则失败，错误：' + e.message)
  }
}

// 修改规则状态
const toggleRuleStatus = async (ruleId: number, status: boolean) => {
  console.log('toggle: status', status)
  try {
    await store.toggleRuleStatus(ruleId, status)
    message.success('修改规则状态成功')
  } catch (e) {
    message.error('修改规则状态失败')
    console.error('toggleRuleStatus error: ', e)
  }
}

// 触发事件，告诉父组件加载指定规则的表单
const loadRule = async (ruleId: number, ruleType: number) => {
  console.log('loadRule: ruleId', ruleId)
  emits('loadRule', ruleId, ruleType === 1 ? 'expert' : 'simple')
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
      <tr v-for="rule in store.ruleList" @click.stop.prevent="loadRule(rule.ruleId, rule.ruleType)">
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
