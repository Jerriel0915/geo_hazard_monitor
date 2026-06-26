import pluginVue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'

export default [
  // ── 全局忽略 ──
  { ignores: ['dist/**', 'node_modules/**', '*.config.*'] },

  // ── TypeScript 推荐规则 ──
  ...tseslint.configs.recommended,

  // ── Vue 3 推荐规则 ──
  ...pluginVue.configs['flat/recommended'],

  // ── Vue 文件：注入 TypeScript parser ──
  {
    files: ['**/*.vue'],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser,
      },
    },
  },

  // ── 项目级规则调优 ──
  {
    rules: {
      // ── 关闭纯格式化规则（应由 Prettier/编辑器处理） ──
      'vue/max-attributes-per-line': 'off',
      'vue/html-self-closing': 'off',
      'vue/html-closing-bracket-spacing': 'off',
      'vue/html-closing-bracket-newline': 'off',
      'vue/singleline-html-element-content-newline': 'off',
      'vue/multiline-html-element-content-newline': 'off',
      'vue/html-indent': 'off',
      'vue/first-attribute-linebreak': 'off',
      'vue/attributes-order': 'off',
      'vue/order-in-components': 'off',
      'vue/attribute-hyphenation': 'off',
      'vue/html-button-has-type': 'off',
      'vue/html-comment-content-spacing': 'off',
      'vue/html-comment-indent': 'off',

      // ── 项目惯例 ──
      'vue/multi-word-component-names': 'off',

      // ── 质量规则 ──
      '@typescript-eslint/no-explicit-any': 'warn',
      'no-console': ['warn', { allow: ['error'] }],
      '@typescript-eslint/no-unused-vars': [
        'warn',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
      'vue/no-v-html': 'warn',
    },
  },
]
