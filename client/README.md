# Frontend

## Convenciones para todas las funcionalidades

- **Componentes:** usar los componentes shadcn existentes en `src/components/ui` para controles, diálogos, tablas y estados de carga, error o vacío. Mantener HTML semántico para la estructura de la página.
- **Paginación:** paginar todas las colecciones de tamaño variable, incluidas las listas dentro de selectores y diálogos. Preferir paginación del backend cuando el endpoint la ofrece; si devuelve la colección completa, paginarla en el cliente. Al cambiar una búsqueda o filtro, volver a la primera página.
- **Autorización:** decidir la visibilidad y disponibilidad de funciones y acciones por los permisos del rol activo (`user.permissions`), nunca por el nombre del rol (`activeRole`). Declarar los permisos de navegación en `src/components/layout/sidebar-constants.ts`. El sidebar comunica la falta de acceso; el backend autoriza cada operación. Las rutas del frontend no deben redirigir a `/403` por falta de un permiso.

La separación entre `/portal` y `/gestion` sigue siendo una decisión de navegación de nivel de aplicación; dentro de cada rama, las funcionalidades se rigen por permisos. Ver también [estructura de rutas](../notes/routes.md).

---

# React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is enabled on this template. See [this documentation](https://react.dev/learn/react-compiler) for more information.

Note: This will impact Vite dev & build performances.

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])

```

You can also install [eslint-plugin-react-x](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x'
import reactDom from 'eslint-plugin-react-dom'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])

```
