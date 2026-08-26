export default {
  input: 'http://localhost:8080/api-docs',
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