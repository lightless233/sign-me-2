<template>
  <div>
    <el-row :gutter="10">
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>规则列表</span>
              <el-button type="primary" size="small" @click="createNewRule">新建规则</el-button>
            </div>
          </template>

          <!-- card body -->
          <!-- 这地方塞个 table，后续拆分成单独的组件放出去，先写到一个文件里 -->
          <el-table :data="allSignRulesList" v-loading="tableLoading" @rowClick="loadRule">
            <el-table-column label="开关">
              <template #default="scope">
                <el-switch v-model="scope.row.status" @click.stop
                           @change="toggleRuleStatus($event, scope.row.ruleId,)"></el-switch>
              </template>
            </el-table-column>
            <el-table-column label="名称">
              <template #default="scope">
                {{ scope.row.ruleName }}
              </template>
            </el-table-column>
            <el-table-column label="操作">
              <template #default="scope">
                <el-popconfirm
                    title="确定要删除该规则吗？删除后无法恢复！"
                    placement="top"
                    icon-color="red"
                    :icon="ElIconInfoFilled"
                    confirm-button-text="好的"
                    cancel-button-text="不用了"
                    @confirm="deleteSignRule(scope.row.ruleId)"
                >
                  <template #reference>
                    <el-button :text="true" size="small" type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>

        </el-card>
      </el-col>
      <el-col :span="18">
        <el-card shadow="never" v-if="firstVisitPage">
          <div class="editor-placeholder">请从左侧选择已有规则，或创建新规则</div>
        </el-card>
        <el-card shadow="never" v-else v-loading="editorLoading">
          <template #header>
            <span>规则内容</span>
          </template>

          <!-- card body -->
          <el-form label-width="120px" label-position="top">
            <el-row>
              <el-col :span="12">
                <el-form-item label="规则开关">
                  <el-radio-group v-model="form.status">
                    <el-radio label="true">开启</el-radio>
                    <el-radio label="false">关闭</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row>
              <el-col :span="12">
                <el-form-item label="规则名称">
                  <el-input autocomplete="off" v-model="form.name"/>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row>
              <el-col :span="12">
                <el-form-item label="URL白名单 (支持正则，该规则的生效范围)">
                  <el-input autocomplete="off" v-model="form.filter"/>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row>
              <el-col>
                <el-form-item label="生效工具范围">
                  <el-checkbox-group v-model="form.toolFlag">
                    <el-checkbox label="TOOL_SUITE"/>
                    <el-checkbox label="TOOL_TARGET"/>
                    <el-checkbox label="TOOL_PROXY" checked/>
                    <el-checkbox label="TOOL_SPIDER"/>
                    <el-checkbox label="TOOL_SCANNER"/>
                    <el-checkbox label="TOOL_INTRUDER"/>
                    <el-checkbox label="TOOL_REPEATER" checked/>
                    <el-checkbox label="TOOL_SEQUENCER"/>
                    <el-checkbox label="TOOL_DECODER"/>
                    <el-checkbox label="TOOL_COMPARER"/>
                    <el-checkbox label="TOOL_EXTENDER"/>
                  </el-checkbox-group>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row>
              <el-col>
                <el-form-item label="规则内容">
                  <codemirror
                      v-model="form.content"
                      :extensions="extensions"
                      ref="cm"
                      :style="{height: '500px', width: '100%', 'font-family': 'monospace'}"
                      :indent-with-tab="true"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row justify="center">
              <el-col :span="6" style="text-align: center">
                <el-button type="primary" @click="handleSave" :disabled="!saveBtnEnabled">保存</el-button>
              </el-col>
            </el-row>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import {Codemirror} from "vue-codemirror"
import {javascript} from "@codemirror/lang-javascript"
import {useAPIFetch} from "~/composables/useAPIFetch";

/// 切换规则状态按钮的 loading 指示
const ruleStatusLoading = ref(false);
/// 保存按钮的状态
const saveBtnEnabled = ref(true);
/// 存储所有签名规则
const allSignRulesList = ref([]);
/// 表格的loading指示
const tableLoading = ref(true);
/// 编辑器的loading指示
const editorLoading = ref(false);
/// 是否第一次打开此页面，如果是，则展示一个提示，否则正常显示表单
const firstVisitPage = ref(true);

/// 表单内容定义
const form = reactive({
  id: -1,
  name: "",
  filter: "",
  status: "true",
  toolFlag: [],
  content: "Some code ...",
});

/// 生命周期钩子
onBeforeMount(async () => {
  console.log("onMounted!");
  // 获取所有的规则列表
  await getRules();
  // console.log(`all sign rule list:`, allSignRulesList)
});

/**
 * 获取所有规则列表
 * @returns {Promise<void>}
 */
const getRules = async () => {
  tableLoading.value = true;
  try {
    const resp = await $fetch("/api/signRule/list", {baseURL: useRuntimeConfig().public.baseURL});
    console.log("resp:", resp);
    if (resp.code !== 2000) {
      ElMessage.error(resp.message);
    } else {
      allSignRulesList.value = resp.data;
    }
  } catch (e) {
    ElMessage.error(`Error when make network request. Error: ${e}`)
  } finally {
    tableLoading.value = false;
  }
}

/**
 * 点击创建新规则按钮，清空表单
 * @returns {Promise<void>}
 */
const createNewRule = async () => {
  firstVisitPage.value = false;
  form.id = -1;
  form.name = "";
  form.filter = "";
  form.status = "true";
  form.toolFlag = [];
  form.content = "Some code ...";
}

/**
 * 点击表格某一行时，加载该规则的内容到编辑器中
 */
const loadRule = async (row, _columns, _event) => {
  firstVisitPage.value = false;
  editorLoading.value = true;
  // console.log("row:",row, "column:", column, "event:", event, "ruleId:", row.ruleId);
  const ruleId = row.ruleId;
  try {
    const {data, error} = await useAPIFetch("/api/signRule/get_by_id", {query: {ruleId}});
    if (error.value) {
      ElMessage.error(`Error when create new sign rule. Error: ${error.value}`);
      console.log(`Error:`, error.value);
      return;
    }

    const response = data.value;
    if (response.code !== 2000) {
      ElMessage.error(response.message);
      return;
    }

    const vo = response.data;
    form.id = ruleId;
    form.name = vo.ruleName;
    form.filter = vo.filter;
    form.status = vo.status ? "true" : "false";
    form.toolFlag = vo.readableToolFlag;
    form.content = vo.content;
  } catch (e) {
    ElMessage.error(`Error when make network request. Error: ${e}`);
    console.error(`Error:`, e);
  } finally {
    editorLoading.value = false;
  }

}

/**
 * 保存规则
 * @returns {Promise<void>}
 */
const handleSave = async () => {
  // TODO 保存的时候，禁用一下按钮，防止连续点击
  // 构造发给后端的数据
  saveBtnEnabled.value = false;
  const payload = {
    ruleName: form.name,
    filter: form.filter,
    content: form.content,
    status: form.status === "true",
    // ruleType: 1,  // 这里规则类型是写死的，只可能是复杂规则
    toolFlag: form.toolFlag.join(",") // string 类型，后端负责解析 string -> int 的计算
  };
  console.log("payload:", payload);

  // 判断是新增还是更新
  let url = "/api/signRule/create"
  if (form.id !== -1) {
    // update
    url = "/api/signRule/update"
    payload["ruleId"] = form.id
  } else {
    payload["ruleType"] = 1
  }

  try {
    const {data, error} = await useAPIFetch(url, {method: "POST", body: payload});
    // console.log(data, error)
    if (error.value) {
      ElMessage.error(`Error when create new sign rule. Error: ${error.value}`);
      console.log(`Error:`, error.value);
    } else {
      const response = data.value;
      // console.log("response:", response);
      if (response.code !== 2000) {
        ElMessage.error(response.message);
      } else {
        form.id = response.data.ruleId;
        ElMessage.success("保存成功");

        // 重新加载规则列表
        await getRules();
      }
    }
  } catch (e) {
    ElMessage.error(`Error when make network request. Error: ${e}`);
    console.error(`Error:`, e);
  } finally {
    saveBtnEnabled.value = true;
  }
}

/**
 * 修改规则状态
 * @returns {Promise<void>}
 */
const toggleRuleStatus = async (event, ruleId) => {
  // console.log("click!", ruleId, event);
  ruleStatusLoading.value = true;
  try {
    const {data, error} = await useAPIFetch("/api/signRule/toggle-status", {
      method: "POST", body: {
        ruleId: ruleId,
        status: event,
      }
    });

    if (error.value) {
      ElMessage.error(`Error when toggle sign rule status. Error: ${error.value}`);
      console.error(error.value);
      return;
    }

    const resp = data.value;
    if (resp.code !== 2000) {
      ElMessage.error(resp.message);
    } else {
      ElMessage.success("修改成功!");
    }

  } catch (e) {
    ElMessage.error(`Error when make network request. Error: ${e}`);
    console.error(e);
  } finally {
    ruleStatusLoading.value = false;
  }
}

/**
 * 删除签名规则
 * @returns {Promise<void>}
 */
const deleteSignRule = async (id) => {
  try {
    const {data, error} = await useAPIFetch("/api/signRule/delete", {method: "POST", body: {ruleId: id}});
    if (error.value) {
      ElMessage.error(`Error when delete sign rule status. Error: ${error.value}`);
      console.error(error.value);
      return;
    }

    const resp = data.value;
    if (resp.code !== 2000) {
      ElMessage.error(resp.message);
    } else {
      ElMessage.success("删除成功!");
      await getRules();
    }
  } catch (e) {
    ElMessage.error(`Error when make network request. Error: ${e}`);
    console.error(e);
  }
}


const handleCMChange = async () => {
}
const extensions = [javascript()];
</script>

<style>
.cm-editor.cm-focused {
  outline: 0 solid white
}

.cm-editor .cm-content {
  font-family: Consolas, monospace
}
</style>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.editor-placeholder {
  color: #606266;
  font-size: 16px;
  text-align: center;
  height: 200px;
  vertical-align: center;
  line-height: 200px;
}
</style>