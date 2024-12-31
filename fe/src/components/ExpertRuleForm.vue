<script setup lang="tsx">
import { inject, ref } from 'vue'
import type { AxiosInstance } from 'axios'
import { useMessage } from 'naive-ui'

// 注入必要的组件
const axios = inject('axios') as AxiosInstance
const message = useMessage()

// 保存按钮的状态
const disableSaveBtn = ref<boolean>(false)

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
// TODO 增加保存对应的 axios 请求
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
      // TODO 抛出事件，触发规则列表的刷新
    }
  } catch (e) {
    message.error('保存规则失败，错误：' + JSON.stringify(e))
    console.error('saveRule error: ', e)
  } finally {
    disableSaveBtn.value = false
  }
}
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
        <n-checkbox-group :value="formData.toolFlag">
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
        <n-input type="textarea" v-model:value="formData.content" placeholder="" />
      </n-form-item>
    </n-form>

    <template #footer>
      <n-flex justify="center">
        <n-button :disabled="disableSaveBtn" type="primary" @click.stop.prevent="saveRule">保存</n-button>
      </n-flex>
    </template>

  </n-card>
</template>

