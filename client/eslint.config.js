import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import pluginRouter from '@tanstack/eslint-plugin-router'
import pluginQuery from '@tanstack/eslint-plugin-query'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  ...pluginRouter.configs['flat/recommended'],
  ...pluginQuery.configs['flat/recommended-strict'],
  globalIgnores([
    '.tanstack',
    'dist',
    'src/routeTree.gen.ts'
  ]),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
    },
  },
  {
    files: ['src/routes/**/*.tsx'],
    rules: {
      // Route components must be named exports (not just referenced locally)
      // so the rule recognizes the file as component-only alongside `Route`.
      'react-refresh/only-export-components': ['off', { allowExportNames: ['Route'] }],
    },
  },
  {
    files: ['src/components/ui/**/*.{ts,tsx}'],
    rules: {
      // shadcn/ui components intentionally co-export helpers (variants, contexts, hooks)
      // alongside the component from the same file.
      'react-refresh/only-export-components': 'off',
    },
  },
])
