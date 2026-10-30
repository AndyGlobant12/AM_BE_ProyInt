# Guía de contribución

Este documento describe el flujo de trabajo de ramas y commits usado en el
proyecto **QA Backend API — TC Region (SSM-17667)**.

## Estrategia de ramas (GitFlow simplificado)

```
main                                          ← rama estable (entregable final)
 └── develop                                  ← rama de integración
      └── feature/<descripcion-corta>         ← desarrollo de funcionalidades
```

| Rama | Propósito | Recibe merge desde |
|------|-----------|--------------------|
| `main` | Versión estable y entregable. Siempre desplegable. | `develop` |
| `develop` | Integración de funcionalidades terminadas. | `feature/*` |
| `feature/*` | Desarrollo aislado de una funcionalidad o conjunto de pruebas. | — |

### Flujo de trabajo

1. Partir de `develop` para crear una rama `feature/...`.
2. Desarrollar y commitear en la rama `feature/...`.
3. Abrir un Pull Request de `feature/...` → `develop`.
4. Tras revisar e integrar, abrir un Pull Request de `develop` → `main`.

## Convención de commits

Se usa un prefijo que indica el tipo de cambio:

| Prefijo | Uso |
|---------|-----|
| `feat` | Nueva funcionalidad o caso de prueba |
| `fix` | Corrección de un error |
| `docs` | Cambios de documentación |
| `test` | Cambios en pruebas existentes |
| `chore` | Mantenimiento, configuración, build |
| `refactor` | Refactorización sin cambio de comportamiento |

Ejemplo:

```
feat: agrega validacion de contrato para endpoint management/info
```

## Pull Requests

- Todo cambio llega a `main` mediante Pull Request (nunca push directo).
- Cada PR debe completar la plantilla (`.github/pull_request_template.md`).
- Se recomienda incluir el resumen de ejecución de pruebas en la descripción.

## Ejecución de pruebas

```bash
mvn clean test                 # ejecuta la suite (ambiente dev por defecto)
mvn clean test -Denv=qa        # ejecuta contra QA
allure serve target/allure-results   # reporte interactivo
```
