/// <reference types="vite/client" />

interface ViteTypeOptions {
  // Deshabilita el uso de claves desconocidas en el objeto import.meta.env
  strictImportMetaEnv: unknown
}

interface ImportMetaEnv {
  readonly EXAMPLE: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}