---
alwaysApply: true
scene: git_message
---

每次提交的 Commit 信息都应包含以下结构：
```text
<type>(<scope>): <subject>
// 空一行
<body>
// 空一行
<footer>
```

- **Header (必填):** 包含 `type`（类型）、`scope`（作用域）和 `subject`（简述）。
- **Body (选填):** 详细说明，解释 **“为什么做”** 和 **“主要变动是什么”**。
- **Footer (选填):** 关联 Issue 编号或备注不兼容的破坏性变更（Breaking Change）。

常用的标准类型列表：
- `feat`: 新增功能 (Feature)
- `fix`: 修复 Bug
- `docs`: 仅修改文档 (Documentation)
- `style`: 代码格式调整（不影响逻辑，如空格、缩进等）
- `refactor`: 代码重构（既不是新增功能，也不是修复 Bug）
- `perf`: 性能优化 (Performance)
-  `test`: 增加或修改单元测试/集成测试
-  `chore`: 构建过程或辅助工具的变动（如更新依赖库）
-  `revert`: 代码回滚