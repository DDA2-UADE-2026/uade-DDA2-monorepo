# Flujo de trabajo con Git y GitHub

## Objetivo

El repositorio utiliza un flujo de ramas basado en pull requests para separar el desarrollo de la producción y evitar cambios directos en las ramas protegidas.

## Ramas

| Rama | Propósito |
|---|---|
| `main` | Código listo para producción. |
| `develop` | Integración del desarrollo del equipo. |
| `feature/*` | Desarrollo de una funcionalidad o cambio puntual. |

## Reglas obligatorias

- No se permiten pushes directos a `main` ni `develop`.
- `main` solo recibe pull requests desde `develop`.
- `develop` solo recibe pull requests desde ramas con el formato `feature/*`.
- Todo pull request debe pasar estos checks de GitHub Actions:
  - `validate-source-branch`
  - `client-build`
  - `server-assemble`
- Los pull requests deben estar dirigidos a la rama correcta antes de solicitar revisión.

## Convención de nombres

Las ramas de trabajo deben comenzar con `feature/` y continuar con un nombre corto, descriptivo y separado por guiones.

Ejemplos válidos:

```text
feature/login
feature/gestion-beneficios
feature/123-filtro-solicitudes
```

Ejemplos inválidos:

```text
bugfix/login
fix/error-login
develop/login
feature_login
```

## Flujo diario

### 1. Actualizar `develop`

Antes de comenzar una tarea, obtener la última versión de desarrollo:

```bash
git switch develop
git pull origin develop
```

### 2. Crear una rama de trabajo

Cada tarea debe comenzar desde `develop`:

```bash
git switch -c feature/nombre-corto
```

Ejemplo:

```bash
git switch -c feature/gestion-beneficios
```

### 3. Trabajar y crear commits

Los cambios se realizan exclusivamente en la rama `feature/*`:

```bash
git status
git add .
git commit -m "feat: agregar gestion de beneficios"
```

Los commits deben describir brevemente el cambio realizado.

### 4. Subir la rama

La primera vez que se sube una rama nueva:

```bash
git push -u origin feature/nombre-corto
```

Para los siguientes cambios:

```bash
git push
```

### 5. Abrir el pull request

El pull request debe seguir este flujo:

```text
feature/nombre-corto -> develop
```

Antes de solicitar el merge, verificar que:

- El destino sea `develop`.
- El nombre de la rama comience con `feature/`.
- `validate-source-branch` esté aprobado.
- `client-build` esté aprobado.
- `server-assemble` esté aprobado.
- No existan conflictos.

## Actualizar una feature con `develop`

Si `develop` recibió cambios mientras se trabaja en una feature, actualizar la rama antes de finalizar el pull request:

```bash
git switch develop
git pull origin develop
git switch feature/nombre-corto
git merge develop
```

Resolver los conflictos si aparecen, crear el commit correspondiente y subir los cambios:

```bash
git add .
git commit -m "chore: actualizar feature con develop"
git push
```

## Flujo de publicación

Cuando las funcionalidades necesarias ya fueron integradas en `develop`, se solicita un pull request hacia producción:

```text
develop -> main
```

Antes del merge:

1. Verificar que el destino sea `main`.
2. Confirmar que `validate-source-branch` esté aprobado.
3. Confirmar que `client-build` esté aprobado.
4. Confirmar que `server-assemble` esté aprobado.
5. Resolver cualquier conflicto.
6. Hacer merge del pull request.

Una vez integrada la funcionalidad en `develop`, la rama `feature/*` puede eliminarse desde GitHub o localmente:

```bash
git branch -d feature/nombre-corto
git push origin --delete feature/nombre-corto
```

## Flujos prohibidos

Estos flujos no están permitidos:

```text
feature/* -> main
bugfix/* -> develop
fix/* -> develop
push directo -> main
push directo -> develop
```

Los intentos incorrectos fallan por el check `validate-source-branch` o por las reglas de protección configuradas en GitHub.

## Configuración de GitHub Rulesets

El repositorio debe tener un ruleset activo que apunte a `main` y `develop`.

Configurar las siguientes reglas:

- `Require a pull request before merging`.
- `Require status checks to pass before merging`.
- `Restrict deletions`.
- `Block force pushes`.
- No agregar usuarios, equipos ni administradores a la lista de bypass.

Los checks obligatorios son:

- `validate-source-branch`
- `client-build`
- `server-assemble`

La validación del origen de cada pull request se realiza en `.github/workflows/branch-policy.yml`. El check permite únicamente:

```text
develop -> main
feature/* -> develop
```

La compilación del frontend y del backend se ejecuta mediante `.github/workflows/ci.yml`. El lint del frontend queda desactivado temporalmente hasta definir la nueva herramienta.

## Resumen del flujo

```text
develop
   |
   +--> feature/nueva-funcionalidad
             |
             +--> Pull request -> develop
                                      |
                                      +--> Pull request -> main
```
