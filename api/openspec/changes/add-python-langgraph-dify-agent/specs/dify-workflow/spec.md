## ADDED Requirements

### Requirement: 仅由服务端调用 Dify
系统 MUST 只由可信服务端组件调用 Dify。

#### Scenario: 浏览器检查 Agent 通信
- **WHEN** Vue 客户端与 Agent 服务通信
- **THEN** 客户端不能获得 Dify API Key 或无限制的工作流凭证

### Requirement: Dify 外层工作流
Dify MUST 承担外层意图路由，并可以在有状态预约任务中调用 LangGraph 子 Agent。

#### Scenario: 预约请求需要复杂信息收集
- **WHEN** Dify 判断预约请求缺少参数或需要多次工具调用
- **THEN** Dify 携带签名 Agent 上下文调用 LangGraph 预约接口
- **AND** Dify 返回结构化预约草稿或追问内容

### Requirement: 工作流导出版本化
仓库 MUST 保存每个必需工作流的可导入 Dify 定义。

#### Scenario: 在干净环境中重建工作流
- **WHEN** 开发者将仓库中的工作流 DSL 导入兼容的 Dify 实例
- **THEN** 安全问答和预约分析工作流可以被重建
- **AND** 导入过程不需要仓库中的任何真实密钥

### Requirement: 低置信度知识回答
安全知识工作流在检索置信度不足时 MUST 避免生成无依据回答。

#### Scenario: 没有相关安全文档
- **WHEN** 检索结果均未达到配置的相似度阈值
- **THEN** 工作流明确说明未找到可靠依据
- **AND** 引导用户联系实验室管理员
