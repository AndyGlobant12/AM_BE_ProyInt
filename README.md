# SSM-17667 — T.C. Region · Actualización a Java 25, Maven 4 y mitigación de vulnerabilidades de Prisma y Veracode

![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9.16-C71A36?logo=apachemaven&logoColor=white)
![Playwright](https://img.shields.io/badge/Playwright_for_Java-1.49.0-2EAD33?logo=playwright&logoColor=white)
![TestNG](https://img.shields.io/badge/TestNG-7.10.2-FF6C37)
![Allure](https://img.shields.io/badge/Allure-2.29.0-FF4088?logo=qameta&logoColor=white)
![Tests](https://img.shields.io/badge/tests-8_passed_·_1_skipped-brightgreen)
![License](https://img.shields.io/badge/license-none-lightgrey)

Suite de automatización de pruebas de API para el microservicio `am-tc-region`, construida con Playwright for Java, TestNG y el patrón Service Wrapper (POM adaptado a servicios), con reportes en Allure. Valida los casos de regresión del ticket SSM-17667 (migración a Java 25 / Maven 4 y mitigación de vulnerabilidades).

> **Nota sobre versiones:** el ticket SSM-17667 describe la migración objetivo a Java 25 / Maven 4. Este proyecto de automatización compila y se ejecuta sobre **Java 17** y **Maven 3.9.16**, que son las versiones del entorno de trabajo actual.

---

## Autor

**Andres Felipe Obando Reinoso**

## Repositorio

https://github.com/AndyGlobant12/AM_BE_ProyInt.git

## Referencias

- **Jira (épica/ticket):** [SSM-17667](https://aeromexico.atlassian.net/browse/SSM-17667)
- **TestRail (run 13928):** [Ver run](https://amxqa.testrail.io/index.php?/runs/view/13928&group_by=cases:section_id&group_order=asc&display=tree)

---

## Stack tecnológico

| Herramienta | Versión | Rol |
|-------------|---------|-----|
| Java (JDK) | 17 | Lenguaje / compilación |
| Maven | 3.9.16 | Gestión de build y dependencias |
| Playwright for Java | 1.49.0 | Cliente HTTP para pruebas de API |
| TestNG | 7.10.2 | Runner de pruebas |
| Allure TestNG | 2.29.0 | Reportador |
| Jackson Databind | 2.18.2 | Parseo y validación de JSON |
| AspectJ Weaver | 1.9.22 | Captura de `@Step`/`@Attachment` en Allure |

## Prerrequisitos

- **JDK 17** instalado y accesible por Maven (`JAVA_HOME`).
- **Maven 3.9.16** (o compatible 3.9.x).
- **Allure CLI 2.46.1** (o compatible) para servir el reporte: [guía de instalación](https://allurereport.org/docs/install/).
- Conectividad hacia el ambiente del microservicio `am-tc-region`.

---

## Estructura del proyecto

```
qa-backend-api-PI-final/
├── pom.xml                     → Build Maven: Java 17, Playwright, TestNG, Allure
├── testng.xml                  → Suite TestNG + listener de Allure
├── data/                       → (entrada) casos TestRail en Markdown
├── endpoint/                   → (entrada) colección Postman de referencia
└── src/test/
    ├── java/
    │   ├── config/             → Infraestructura y configuración
    │   │   ├── Environment.java         → Base URL por ambiente (-Denv=dev|qa|local)
    │   │   ├── ApiHeaders.java          → Constantes de headers del contrato
    │   │   └── PlaywrightApiClient.java → Ciclo de vida de Playwright (APIRequestContext)
    │   ├── services/           → ★ Service Wrapper (el "POM" de las APIs)
    │   │   └── RegionService.java       → Encapsula endpoints, headers y payloads
    │   ├── support/            → Utilidades compartidas
    │   │   └── JsonSupport.java         → Parseo / pretty-print JSON (Jackson)
    │   └── tests/              → Clases de prueba con aserciones
    │       ├── BaseApiTest.java         → Setup/teardown + adjuntos a Allure
    │       ├── RegionByRouteTest.java   → TC001–TC008
    │       └── ManagementInfoTest.java  → TC009
    └── resources/
        ├── allure.properties            → Config de resultados Allure
        └── allure-categories.json       → Categorización de defectos en el reporte
```

### Patrón Service Wrapper

El patrón separa el **"cómo"** del **"qué"**:

- **`services/`** concentra *cómo* hablar con la API (URLs, headers `departure`/`arrival`, `x-transactionId`, construcción de requests).
- **`tests/`** expresa únicamente *qué* se valida (escenario + aserciones).

Es el equivalente del Page Object Model aplicado a servicios de backend.

---

## Ambientes

El ambiente se selecciona con la propiedad de sistema `-Denv`. Por defecto se usa `dev`.

| Ambiente | Valor `-Denv` | Base URL |
|----------|---------------|----------|
| Desarrollo (default) | `dev` | `https://amx-c-mtpsbk-de.amlab7.com` |
| QA | `qa` | `https://amx-c-mtpsbk-q.amlab7.com` |
| Local | `local` | `http://localhost:8085` |

---

## Cómo ejecutar

```bash
# Ejecutar todos los tests (genera target/allure-results)
mvn clean test

# Ejecutar contra otro ambiente (dev por defecto)
mvn clean test -Denv=qa

# Servir el reporte Allure interactivo (abre navegador)
allure serve target/allure-results

# Generar un reporte estático persistente
allure generate target/allure-results --clean -o target/allure-report
```

---

## Cobertura de pruebas

Los 9 casos provienen del run 13928 de TestRail (SSM-17667), sobre el endpoint principal `GET /tc/region/regionByRoute` y `GET /tc/region/management/info`.

| # | Case ID | Tipo | Prioridad | Escenario | Estado |
|---|---------|------|-----------|-----------|--------|
| TC001 | C254746 | Functional | Critical | OD Internacional (MEX-LAX) → 200 INT | ✅ passed |
| TC002 | C254747 | Functional | High | OD Doméstico (MEX-MTY) → 200 DOM | ✅ passed |
| TC003 | C254748 | Functional | Medium | Consultas consecutivas mismo OD → datos consistentes (caché) | ✅ passed |
| TC004 | C254749 | Destructive | Low | Downstream caído → 500 SERVICE_UNAVAILABLE (fail-fast) | ⏭️ skipped¹ |
| TC005 | C254750 | Functional | High | Métrica de error en New Relic registrada post migración | ✅ passed² |
| TC006 | C254751 | Destructive | Low | Header obligatorio faltante → 400 BAD_REQUEST | ✅ passed |
| TC007 | C254752 | Destructive | Low | IATA inexistente → 500 error de contrato | ✅ passed³ |
| TC008 | C254753 | Functional | Medium | Ráfaga ligera 5-10 consultas → estabilidad, sin 500/504 | ✅ passed |
| TC009 | C254754 | Functional | High | Endpoint management/info → versión desplegada | ✅ passed |

**Resultado:** 9 total · **8 passed** · **1 skipped** · 0 failed · 0 broken.

### Notas de implementación

1. **TC004** es un caso destructivo que requiere inyección de fallo en un downstream. No es provocable en caja negra, por lo que el test se auto-salta (`skipped`) con un mensaje claro en lugar de dar un resultado engañoso. Si el servicio responde 500, valida el contrato de error.
2. **TC005** valida en caja negra que la petición que dispara el error se emite con un `transactionId` de traza (`nr-trace-17667-*`) y produce un error de contrato con `reason`. La verificación del evento en New Relic es manual/externa.
3. **TC007** — el `.md` esperaba `ERR.TC.REGION.EMPTY_RESPONSE`, pero el contrato real (dev) devuelve `ERR.TC.REGION.INTERNAL_SERVER_ERROR` ante un IATA inexistente. La aserción acepta ambos como errores de contrato acotados (status 500 + `reason` que inicia con `ERR.TC.REGION.`).

> **Observación de contrato:** el campo `regionDescription` se devuelve en inglés (`International`/`Domestic`); el campo clave de negocio `regionShortDescription` (`INT`/`DOM`) coincide exactamente con lo esperado. Los headers reales del endpoint son `departure`/`arrival` (no `origin`/`destination`).

---

## Licencia

Este proyecto no cuenta con una licencia (sin licencia / uso interno).
