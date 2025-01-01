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
