<script setup lang="tsx">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useMessage } from 'naive-ui'
import { Codemirror } from 'vue-codemirror'
import { javascript } from '@codemirror/lang-javascript'
import { autocompletion, CompletionContext } from '@codemirror/autocomplete'
import { useRuleManagerStore } from '@/stores/ruleManager.ts'
import type { ExpertRuleFormType } from '@/types'


// 注入必要的组件
const store = useRuleManagerStore()
const message = useMessage()

// 保存按钮的状态
const disableSaveBtn = ref<boolean>(false)

onMounted(() => {
  // 监听 CTRL+S 事件
  window.addEventListener('keydown', handleCTRLS)
})
onBeforeUnmount(() => {
  // 移除 CTRL+S 事件
  window.removeEventListener('keydown', handleCTRLS)
})

// hook CTRL+S 为保存操作
const handleCTRLS = async (event: KeyboardEvent) => {
  console.log('handleCTRLS')
  if (event.ctrlKey && event.key === 's') {
    event.preventDefault()
    await saveRule()
  }
}

// 保存规则，根据提供的 ruleId，判断是新增还是编辑
const saveRule = async () => {
  console.log(`saveRule, formData: ${store.expertRuleFormData}`)
  try {
    disableSaveBtn.value = true
    // 更新规则，并把返回的规则ID赋值给当前表单
    await store.saveRule()
    message.success('保存规则成功')
  } catch (e) {
    message.error('保存规则失败，错误：' + JSON.stringify(e))
    console.error('saveRule error: ', e)
  } finally {
    disableSaveBtn.value = false
  }
}


// TODO 完成预定义的补全列表
const completions = [
  { label: 'getById', detail: '根据ID获取数据' },
  { label: 'getByParam', detail: '根据参数获取数据' },
  { label: 'getAll', detail: '获取所有数据' }
]

// TODO 完成补全函数
function getCompletions(context: CompletionContext) {
  const word = context.matchBefore(/get\w*/) // 匹配以 "get" 开头的单词
  if (!word || word.from === word.to) return null // 如果没有匹配到，返回 null

  return {
    from: word.from,
    options: completions
  }
}

// 启用补全功能
const autocompleteExtension = autocompletion({ override: [getCompletions] })
const extensions = [javascript(), autocompleteExtension]
</script>

<template>
  <n-card size="small">
    <n-form ref="formRef" :label-width="120" size="small" autocomplete="off">
      <n-form-item label="规则开关">
        <n-switch v-model:value="store.expertRuleFormData.status" />
      </n-form-item>

      <n-form-item label="规则名称">
        <n-input v-model:value="store.expertRuleFormData.name" placeholder="请输入方便自己分辨的规则名称" />
      </n-form-item>

      <n-form-item label="URL白名单（该规则仅对白名单内的范围生效，支持正则）">
        <n-input v-model:value="store.expertRuleFormData.filter" placeholder="例如: www\.baidu\.com" />
      </n-form-item>

      <n-form-item label="生效工具范围">
        <n-checkbox-group v-model:value="store.expertRuleFormData.toolFlag">
          <n-grid :cols="4" :y-gap="8">
            <n-gi>
              <n-checkbox value="TOOL_SUITE" label="TOOL_SUITE" />
            </n-gi>
            <n-gi>
              <n-checkbox value="TOOL_TARGET" label="TOOL_TARGET" />
            </n-gi>
            <n-gi>
              <n-checkbox value="TOOL_PROXY" label="TOOL_PROXY" />
            </n-gi>
            <n-gi>
              <n-checkbox value="TOOL_SPIDER" label="TOOL_SPIDER" />
            </n-gi>
            <n-gi>
              <n-checkbox value="TOOL_SCANNER" label="TOOL_SCANNER" />
            </n-gi>
            <n-gi>
              <n-checkbox value="TOOL_INTRUDER" label="TOOL_INTRUDER" />
            </n-gi>
            <n-gi>
              <n-checkbox value="TOOL_REPEATER" label="TOOL_REPEATER" />
            </n-gi>
            <n-gi>
              <n-checkbox value="TOOL_SEQUENCER" label="TOOL_SEQUENCER" />
            </n-gi>
            <n-gi>
              <n-checkbox value="TOOL_DECODER" label="TOOL_DECODER" />
            </n-gi>
            <n-gi>
              <n-checkbox value="TOOL_COMPARER" label="TOOL_COMPARER" />
            </n-gi>
            <n-gi>
              <n-checkbox value="TOOL_EXTENDER" label="TOOL_EXTENDER" />
            </n-gi>
          </n-grid>
        </n-checkbox-group>
      </n-form-item>

      <n-form-item label="规则内容">
        <codemirror
          v-model="store.expertRuleFormData.content"
          :extensions="extensions"
          ref="cm"
          :style="{height: '600px', width: '100%', 'font-family': 'monospace'}"
          :indent-with-tab="true"
        />
      </n-form-item>
    </n-form>

    <template #footer>
      <n-flex justify="center">
        <n-button :disabled="disableSaveBtn" type="primary" @click.stop.prevent="saveRule">保存</n-button>
        <n-button ghost type="primary" @click.stop.prevent="() => {message.info('规则测试功能开发中...')}">测试
        </n-button>
      </n-flex>
    </template>

  </n-card>
</template>

