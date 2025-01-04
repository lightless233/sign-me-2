<script setup lang="ts">

import RuleList from '@/components/RuleList.vue'
import ExpertRuleForm from '@/components/ExpertRuleForm.vue'
import SimpleRuleForm from '@/components/SimpleRuleForm.vue'
import BlankRuleForm from '@/components/BlankRuleForm.vue'
import { useRuleManagerStore } from '@/stores/ruleManager.ts'

const store = useRuleManagerStore()

</script>

<template>
  <n-grid x-gap="12" :cols="24">
    <n-gi :span="6">
      <!-- 侧边栏，规则列表，新增规则按钮 -->
      <rule-list />
    </n-gi>
    <n-gi :span="18">
      <!-- 新建规则的表单，根据新建的时候选择的类型，展示不同的表单 -->
      <!-- TODO 是否需要换成子路由呢？ -->
      <expert-rule-form v-if="store.currentRuleType === 'expert'" />
      <simple-rule-form v-else-if="store.currentRuleType === 'simple'" />
      <blank-rule-form v-else />
    </n-gi>
  </n-grid>
</template>

<style scoped>

</style>
