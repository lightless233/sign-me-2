export interface RuleItemVO {
  ruleId: number
  ruleName: string
  ruleType: number
  filter: string
  status: boolean
  readableToolFlag: Array<string>
  toolFlag: number
  content: string
}

// 表单数据类型定义
export interface ExpertRuleFormType {
  // 如果 id 为 -1，则当前为新建规则
  // 如果 id > 0，则当前为编辑规则
  id: number
  name: string
  filter: string
  status: boolean
  toolFlag: string[] // string 类型，后端负责解析 string -> int 的计算
  content: string
}
