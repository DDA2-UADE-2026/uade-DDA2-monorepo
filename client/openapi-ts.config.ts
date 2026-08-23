export default {
  input: 'https://api-muni-uade-dev.fabriziob.com/api-docs',
  output: 'src/generated',
  plugins: [
    '@hey-api/client-fetch',
    '@hey-api/sdk',
    '@tanstack/react-query',
  ],
}