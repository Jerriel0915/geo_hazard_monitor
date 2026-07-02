/**
 * Commitlint configuration — Conventional Commits + 中文适配
 *
 * 格式: <type>[(scope)]: <subject>
 *
 * 允许的 type: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert
 * scope:   小写英文 + 数字，如 device / alarm / admin
 * subject: 中英文均可，至少 2 个字符
 *
 * 示例:
 *   feat(device): 新增设备自注册 API
 *   fix(alarm): 综合告警策略更新接口支持局部更新
 *   refactor: 统一异常处理
 */
module.exports = {
  extends: ['@commitlint/config-conventional'],

  rules: {
    // type 必须小写
    'type-case': [2, 'always', 'lower-case'],

    // scope 必须小写
    'scope-case': [2, 'always', 'lower-case'],

    // subject 不限制大小写（支持中文）
    'subject-case': [0],

    // subject 不能为空
    'subject-empty': [2, 'never'],

    // subject 至少 2 个字符
    'subject-min-length': [2, 'always', 2],

    // subject 不以句号结尾
    'subject-full-stop': [2, 'never', '.'],

    // type 必须来自允许列表
    'type-enum': [2, 'always', [
      'feat',
      'fix',
      'docs',
      'style',
      'refactor',
      'perf',
      'test',
      'build',
      'ci',
      'chore',
      'revert',
    ]],

    // header 最大长度 100
    'header-max-length': [2, 'always', 100],
  },
}
