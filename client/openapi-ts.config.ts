import { loadEnv } from 'vite'

const { DOCS_URL } = loadEnv(process.env.NODE_ENV ?? 'development', process.cwd(), '')

if (!DOCS_URL) {
  throw new Error('DOCS_URL is required in the environment')
} 

export default {
  input: DOCS_URL,
  output: 'src/generated',
  plugins: [
    {
      name: '@hey-api/client-fetch',
      runtimeConfigPath: './src/lib/hey-api.ts',
    },
    '@hey-api/sdk',
    '@tanstack/react-query',
    'zod',
  ],
}
