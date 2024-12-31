<script setup lang="tsx">

import {useMessage} from 'naive-ui'

const message = useMessage()
const emits = defineEmits<{
  newRule: [ruleType: string]
}>()

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
  if (key === "simple") {
    message.info("开发中...")
    return
  }
  emits("newRule", key)
}

</script>

<template>
  <n-card title="规则列表" size="small">

    <template #header-extra>
      <n-dropdown trigger="hover" :options="newOpts" @select="handleNewRule">
        <n-button type="primary" size="small">新建</n-button>
      </n-dropdown>
    </template>

    <!-- TODO 获取规则列表并填充进 table -->
    <n-list hoverable clickable>
      <n-list-item>111</n-list-item>
    </n-list>

  </n-card>
</template>

<style scoped>

</style>
