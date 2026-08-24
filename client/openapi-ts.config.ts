export default {
  input: 'https://api-muni-uade-dev.fabriziob.com/api-docs',
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