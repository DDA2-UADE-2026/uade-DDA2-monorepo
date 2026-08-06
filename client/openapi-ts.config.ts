export default {
  input: 'http://localhost:8080/v3/api-docs', // TODO: Wire a implementacion real
  output: 'src/api/generated',
  plugins: [
    '@hey-api/client-fetch',
    '@hey-api/sdk',
    '@tanstack/react-query',
  ],
}