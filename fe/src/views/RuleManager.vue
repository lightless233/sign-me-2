<script setup lang="ts">

import RuleList from '@/components/RuleList.vue'
import ExpertRuleForm from '@/components/ExpertRuleForm.vue'
import SimpleRuleForm from '@/components/SimpleRuleForm.vue'
import BlankRuleForm from '@/components/BlankRuleForm.vue'
import { ref } from 'vue'

// TODO 把这两个移动到 store 里面
const currentRuleType = ref<string>('')
const currentRuleId = ref<number>(-1)

const handleNewRule = (ruleType: string) => {
  console.log('handleNewRule: ruleType', ruleType)
  currentRuleType.value = ruleType
}

// 加载指定规则，并展示规则编辑器表单
const handleLoadRule = async (ruleId: number, ruleType: string) => {
  console.log('handleLoadRule: ruleId=', ruleId)
  currentRuleId.value = ruleId
  currentRuleType.value = ruleType
}
</script>

<template>
  <n-grid x-gap="12" :cols="24">
    <n-gi :span="6">
      <!-- 侧边栏，规则列表，新增规则按钮 -->
      <rule-list @newRule="handleNewRule" @loadRule="handleLoadRule" />
    </n-gi>
    <n-gi :span="18">
      <!-- 新建规则的表单，根据新建的时候选择的类型，展示不同的表单 -->
      <!-- TODO 是否需要换成子路由呢？ -->
      <expert-rule-form v-if="currentRuleType === 'expert'" :ruleId="currentRuleId" />
      <simple-rule-form v-else-if="currentRuleType === 'simple'" />
      <blank-rule-form v-else />
    </n-gi>
  </n-grid>
</template>

<style scoped>

</style>
