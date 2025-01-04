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
  // 参数类型
  { label: 'ParameterType.PARAM_URL', detail: 'URL参数类型' },
  { label: 'ParameterType.PARAM_BODY', detail: '请求体参数类型' },
  { label: 'ParameterType.PARAM_COOKIE', detail: 'Cookie参数类型' },
  { label: 'ParameterType.PARAM_XML', detail: 'XML参数类型' },
  { label: 'ParameterType.PARAM_XML_ATTR', detail: 'XML属性参数类型' },
  { label: 'ParameterType.PARAM_MULTIPART_ATTR', detail: 'Multipart属性参数类型' },
  { label: 'ParameterType.PARAM_JSON', detail: 'JSON参数类型' },
  { label: 'ParameterType.HEADER', detail: 'Header参数类型' },

  // 排序类型
  { label: 'SortType.ASC', detail: '升序排序' },
  { label: 'SortType.DESC', detail: '降序排序' },

  // 操作类型
  { label: 'EditAction.RAW', detail: '原始操作' },
  { label: 'EditAction.ADD', detail: '添加操作' },
  { label: 'EditAction.UPDATE', detail: '更新操作' },
  { label: 'EditAction.OVERRIDE', detail: '覆盖操作' },
  { label: 'EditAction.DELETE', detail: '删除操作' },

  // 工具函数
  { label: 'utils.md5', detail: '计算字符串的MD5哈希值' },
  { label: 'utils.sha1', detail: '计算字符串的SHA1哈希值' },
  { label: 'utils.hash', detail: '计算字符串的指定算法哈希值' },
  { label: 'utils.base64encode', detail: '对字符串进行Base64编码' },
  { label: 'utils.base64decode', detail: '对Base64字符串进行解码' },
  { label: 'utils.getParametersByType', detail: '根据类型获取参数列表' },
  { label: 'utils.getParametersByName', detail: '根据名称获取参数列表' },
  { label: 'utils.getParametersByNameInLocation', detail: '根据名称和位置获取参数列表' },
  { label: 'utils.getParametersByNameInLocationFirstOrNull', detail: '根据名称和位置获取第一个参数或返回null' },
  { label: 'utils.getHeaderByName', detail: '根据名称获取Header' },
  { label: 'utils.getCookieByName', detail: '根据名称获取Cookie' },
  { label: 'utils.sortParameters', detail: '对参数列表进行排序' },
  { label: 'utils.getTimestamp', detail: '获取当前时间戳，当参数为1时，获取10位精确到秒的时间戳，否则获取13位精确到毫秒的时间戳' },
  { label: 'utils.convParametersToMap', detail: '将参数列表转换为Map' },

  // HTTP客户端函数
  { label: 'httpClient.request', detail: '发送HTTP请求' },
  { label: 'httpClient.get', detail: '发送GET请求' },
  { label: 'httpClient.post', detail: '发送POST请求' },

  // 日志函数
  { label: 'log', detail: '记录日志信息' }
]

// TODO 完成补全函数
function getCompletions(context: CompletionContext) {
  // 匹配以字母开头的单词
  const word = context.matchBefore(/[\w.]*/);
  if (!word || word.from === word.to && !context.explicit) return null;

  // 过滤出与当前输入匹配的补全项
  const filteredCompletions = completions.filter(completion => {
    return completion.label.startsWith(word.text);
  });

  // 如果没有匹配的补全项，返回 null
  if (filteredCompletions.length === 0) return null;

  // 返回补全建议
  return {
    from: word.from,
    options: filteredCompletions
  };
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

