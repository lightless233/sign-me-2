<script setup lang="tsx">
const props = defineProps<{
  ruleId: number
}>()
const visible = defineModel<boolean>('visible')

/**
 * 点击测试规则按钮
 */
const handleClickTest = async () => {

}
</script>

<template>
  <n-modal v-model:show="visible">
    <n-card title="测试规则" size="small" role="dialog" aria-modal="true" style="width:80%">
      <n-form size="small" label-placement="left" label-width="5rem">

        <n-grid :x-gap="48" :cols="2">
          <!-- LEFT -->
          <n-gi :span="1">
            <n-grid :cols="1" :x-gap="24">
              <n-form-item-gi :span="1" label="Host">
                <n-input autocomplete="off" placeholder="" />
              </n-form-item-gi>

              <n-form-item-gi :span="1" label="Port">
                <n-input autocomplete="off" placeholder="" />
              </n-form-item-gi>

              <n-form-item-gi :span="1" label="Protocol">
                <n-input autocomplete="off" placeholder="" />
              </n-form-item-gi>

              <n-form-item-gi :span="1" label="HTTP Request">
                <n-input type="textarea" :autosize="{minRows: 20}" autocomplete="off" placeholder="" />
              </n-form-item-gi>
            </n-grid>
          </n-gi>

          <!-- RIGHT -->
          <n-gi :span="1">
            <n-form-item label="修改后 HTTP Request" label-placement="top">
              <n-input type="textarea" :autosize="{minRows: 26}" placeholder="" readonly />
            </n-form-item>
          </n-gi>
        </n-grid>
      </n-form>

      <n-flex justify="center">
        <n-button type="primary">测试</n-button>
      </n-flex>

    </n-card>
  </n-modal>
</template>

