/// <reference types="vite/client" />
/// <reference types="vitest/globals" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  /** URL бэкенда для /oauth2/authorization (см. Vite proxy — только /api) */
  readonly VITE_OAUTH_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
