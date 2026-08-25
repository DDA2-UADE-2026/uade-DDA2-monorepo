/// <reference types="vite/client" />

interface ViteTypeOptions {
  // Deshabilita el uso de claves desconocidas en el objeto import.meta.env
  strictImportMetaEnv: unknown
}

interface ImportMetaEnv {
  readonly EXAMPLE: string
  readonly VITE_SERVER_URL: string
  readonly VITE_CLIENT_URL: string
  readonly DEVTOOLS_IN_PROD: 'true' | 'false'
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}