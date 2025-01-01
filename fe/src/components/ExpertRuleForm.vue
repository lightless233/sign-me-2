<script setup lang="tsx">
import { inject, ref, watch } from 'vue'
import type { AxiosInstance } from 'axios'
import { useMessage } from 'naive-ui'
import {Codemirror} from "vue-codemirror"
import {javascript} from "@codemirror/lang-javascript"
import { autocompletion, CompletionContext } from '@codemirror/autocomplete';
import { useRuleManagerStore } from '@/stores/ruleManager.ts'

// 表单数据类型定义
interface RuleFormType {
  // 如果 id 为 -1，则当前为新建规则
  // 如果 id > 0，则当前为编辑规则
  id: number
  name: string
  filter: string
  status: boolean
  toolFlag: string[] // string 类型，后端负责解析 string -> int 的计算
  content: string
}

// 注入必要的组件
const axios = inject('axios') as AxiosInstance
const message = useMessage()

// 定义 props
const props = defineProps<{
  ruleId: number,
}>()

// store
const store = useRuleManagerStore()

watch(() => props.ruleId, async (newVal, oldVal) => {
  console.log('watch: ruleId', newVal, oldVal)
  // 如果外部传入的 props.ruleId 发生了变化，有两种情况
  // 1. 如果传入的 id 为 -1，说明是新建规则，这时候要清空 formData
  // 2. 如果传入的 id > 0，说明是编辑规则，这时候要加载对应的规则详情
  // 除此之外，无论是哪种情况，应该都需要把 props.ruleId 赋值给 formData.id 或者是 localRuleId
  if (newVal === -1) {
    formData.value = {
      id: -1,
      name: '',
      filter: '',
      status: true,
      toolFlag: ['TOOL_PROXY', 'TOOL_REPEATER'],
      content: ''
    }
  } else {
    // 加载对应的规则详情
    const resp = (await axios.get(`/api/signRule/get_by_id`, { params: { ruleId: newVal } })).data
    if (resp.code !== 2000) {
      message.error('获取规则详情失败，错误：' + resp.message)
      console.error('fetchRuleDetail error: ', resp)
    } else {
      formData.value = {
        id: resp.data.ruleId,
        name: resp.data.ruleName,
        filter: resp.data.filter,
        status: resp.data.status,
        toolFlag: resp.data.readableToolFlag,
        content: resp.data.content
      }
    }
  }
})

// 保存按钮的状态
const disableSaveBtn = ref<boolean>(false)

// 表单数据定义
const formData = ref<RuleFormType>({
  id: -1,
  name: '',
  filter: '',
  status: true,
  toolFlag: ['TOOL_PROXY', 'TOOL_REPEATER'],
  content: ''
})

// TODO hook CTRL+S 为保存操作
// 保存规则，根据提供的 ruleId，判断是新增还是编辑
const saveRule = async () => {
  console.log('saveRule: formData', formData.value)
  const payload = {
    ruleName: formData.value.name,
    filter: formData.value.filter,
    content: formData.value.content,
    status: formData.value.status,
    toolFlag: formData.value.toolFlag.join(','),
    ruleType: 1
  } as any
  const url = formData.value.id === -1 ? '/api/signRule/create' : '/api/signRule/update'
  const _ = formData.value.id === -1 ? undefined : payload['ruleId'] = formData.value.id

  try {
    disableSaveBtn.value = true
    const resp = (await axios.post(url, payload)).data
    if (resp.code !== 2000) {
      message.error('保存规则失败，错误：' + resp.message)
      console.error('saveRule error: ', resp)
    } else {
      message.success('保存规则成功')
      await store.fetchRuleList()
    }
  } catch (e) {
    message.error('保存规则失败，错误：' + JSON.stringify(e))
    console.error('saveRule error: ', e)
  } finally {
    disableSaveBtn.value = false
  }
}



// 预定义的补全列表
const completions = [
  { label: 'getById', detail: '根据ID获取数据' },
  { label: 'getByParam', detail: '根据参数获取数据' },
  { label: 'getAll', detail: '获取所有数据' },
];

// 补全函数
function getCompletions(context: CompletionContext) {
  const word = context.matchBefore(/get\w*/); // 匹配以 "get" 开头的单词
  if (!word || word.from === word.to) return null; // 如果没有匹配到，返回 null

  return {
    from: word.from,
    options: completions,
  };
}

// 启用补全功能
const autocompleteExtension = autocompletion({ override: [getCompletions] });
const extensions = [javascript(), autocompleteExtension];
</script>

<template>
  <n-card size="small">
    <n-form ref="formRef" :label-width="120" size="small" autocomplete="off">
      <n-form-item label="规则开关">
        <n-switch v-model:value="formData.status" />
      </n-form-item>

      <n-form-item label="规则名称">
        <n-input v-model:value="formData.name" />
      </n-form-item>

      <n-form-item label="URL白名单（该规则仅对白名单内的范围生效，支持正则）">
        <n-input v-model:value="formData.filter" />
      </n-form-item>

      <n-form-item label="生效工具范围">
        <n-checkbox-group v-model:value="formData.toolFlag">
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
        <!-- TODO 使用 codemirror 替换 -->
<!--        <n-input type="textarea" v-model:value="formData.content" placeholder="" />-->
        <codemirror
          v-model="formData.content"
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
        <n-button ghost type="primary" @click.stop.prevent="() => {message.info('规则测试功能开发中...')}" >测试</n-button>
      </n-flex>
    </template>

  </n-card>
</template>

