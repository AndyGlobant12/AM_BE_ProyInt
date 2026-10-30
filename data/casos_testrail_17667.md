# Casos de prueba TestRail — SSM-17667

**Run:** 13928 — *SSM-17667 T.C. Region - Actualización a Java 25, Maven 4 y mitigación de vulnerabilidades de Prisma y Veracode | QA*
**Proyecto:** 5 · **Suite:** 661 (section 48195) · **Referencia Jira:** SSM-17667 (épica SSM-14397)
**Microservicio:** am-tc-region · **Endpoint principal:** `GET /tc/region/regionByRoute`
**Total de casos:** 9 · **Estado del run:** 9 Passed / 0 Failed / 0 Blocked / 0 Untested
**Fecha de extracción:** 27/Oct/2026

> Catálogos TestRail — Tipo: 5=Destructive, 6=Functional · Prioridad: 1=Low, 2=Medium, 3=High, 4=Critical.
> Nomenclatura de prefijos: HP=Happy Path, TTP=positivo, TTR=repetición/estabilidad, TTF=flujo de fallo/negativo.

---

## TC001 — RegionByRoute OD Internacional post migración
- **Case ID:** C254746 · **Title:** TC001_HP_MT_TC REGION_REGIONBYROUTE OD INTERNACIONAL POST MIGRACION
- **Tipo:** Functional · **Prioridad:** Critical · **Estimado:** 2min · **Refs:** SSM-17667

**Descripción**
Validar que regionByRoute resuelve un OD internacional (MEX-LAX) con el response ampliado tras la migración Java 25 / Boot 4.1.0.

**Precondiciones**
- Endpoint: `GET /tc/region/regionByRoute`
- Microservicio am-tc-region desplegado con la versión migrada (Java 25 / Boot 4.1.0 / Jackson 3).
- Downstreams tb-region / tb-airports-info / tb-timezone disponibles.
- OD internacional de ejemplo: origin=MEX, destination=LAX.

**Pasos (Given/When/Then)**
- **Given** am-tc-region desplegado con la versión migrada en ambiente {env}
- **When** Se consulta `GET /tc/region/regionByRoute` con headers: channel=web, transactionID={uuid}, origin=MEX, destination=LAX
- **Then** El status code es 200
- **And** El response trae regionShortDescription=INT y las subRegiones/loyaltyZones/countries
- **And** La estructura del response es idéntica al baseline previo a la migración

**Resultado esperado**
Status 200 y respuesta del microservicio:
```json
{
    "airportOriginDestination": "MEXLAX",
    "regionDescription": "Internacional",
    "regionShortDescription": "INT",
    "subRegionDestination": "USA",
    "subRegionFinal": "USA",
    "subRegionOrigin": "LOC",
    "departureAirportLoyaltyZone": "Z01",
    "arrivalAirportLoyaltyZone": "Z04",
    "departureAirportCountry": "MX",
    "arrivalAirportCountry": "US"
}
```

---

## TC002 — RegionByRoute OD Doméstico post migración
- **Case ID:** C254747 · **Title:** TC002_TTP_MT_TC REGION_REGIONBYROUTE OD DOMESTICO POST MIGRACION
- **Tipo:** Functional · **Prioridad:** High · **Estimado:** 2min · **Refs:** SSM-17667

**Descripción**
Validar que regionByRoute resuelve un OD doméstico (MEX-MTY) con regionShortDescription=DOM tras la migración.

**Precondiciones**
- Endpoint: `GET /tc/region/regionByRoute`
- OD doméstico de ejemplo: origin=MEX, destination=MTY. Downstreams disponibles.

**Pasos (Given/When/Then)**
- **Given** am-tc-region desplegado con la versión migrada en ambiente {env}
- **When** Se consulta `GET /tc/region/regionByRoute` con origin=MEX y destination=MTY
- **Then** El status code es 200
- **And** regionShortDescription=DOM y subRegiones LOC

**Resultado esperado**
Status 200 y respuesta del microservicio:
```json
{
    "airportOriginDestination": "MEXMTY",
    "regionDescription": "Domestico",
    "regionShortDescription": "DOM",
    "subRegionDestination": "LOC",
    "subRegionFinal": "LOC",
    "subRegionOrigin": "LOC",
    "departureAirportLoyaltyZone": "Z00",
    "arrivalAirportLoyaltyZone": "Z00",
    "departureAirportCountry": "MX",
    "arrivalAirportCountry": "MX"
}
```

---

## TC003 — Consultas consecutivas mismo OD, datos consistentes
- **Case ID:** C254748 · **Title:** TC003_TTR_MT_TC REGION_CONSULTAS CONSECUTIVAS MISMO OD DATOS CONSISTENTES
- **Tipo:** Functional · **Prioridad:** Medium · **Estimado:** 2min · **Refs:** SSM-17667

**Descripción**
Validar que consultas consecutivas del mismo OD retornan datos consistentes (efecto de caché observable) tras la migración.

**Precondiciones**
- Endpoint: `GET /tc/region/regionByRoute`
- Regla vigente SSM-11165: caché para reducir llamadas a TB Airports/Region/TimeZone (sin invalidación forzada).
- Se valida el EFECTO (consistencia), no el mecanismo interno del caché (caja negra).

**Pasos (Given/When/Then)**
- **Given** am-tc-region desplegado con la versión migrada en ambiente {env}
- **When** Se consulta `GET /tc/region/regionByRoute` tres veces consecutivas con el mismo OD (MEX-LAX)
- **Then** Las tres respuestas son 200
- **And** El contenido de las tres respuestas es idéntico (datos consistentes)

**Resultado esperado**
Validar status 200.
1. Datos consistentes entre consultas consecutivas del mismo OD.
2. Sin variación en estructura ni contenido (efecto de caché preservado, SSM-11165).
3. Sin regresión post-migración en el comportamiento de caché.

---

## TC004 — Downstream no disponible, 500 Service Unavailable
- **Case ID:** C254749 · **Title:** TC004_TTF_MT_TC REGION_DOWNSTREAM NO DISPONIBLE 500 SERVICE UNAVAILABLE
- **Tipo:** Destructive · **Prioridad:** Low · **Estimado:** 2min (forecast 18sec) · **Refs:** SSM-17667

**Descripción**
Validar que ante un downstream no disponible el servicio responde 500 ERR.TC.REGION.SERVICE_UNAVAILABLE de forma acotada (fail-fast por timeouts, CWE-404).

**Precondiciones**
- Endpoint: `GET /tc/region/regionByRoute`
- Error catalog (contrato): 500 ERR.TC.REGION.SERVICE_UNAVAILABLE por I/O error al llamar a un downstream.
- Remediación CWE-404 (tech solution): RestTemplate con connect-timeout=5000ms, read-timeout=10000ms.
- Escenario con un downstream (tb-region/airports-info/timezone) no disponible o lento.
- Se valida el EFECTO observable (error acotado), no el valor exacto del timeout.

**Pasos (Given/When/Then)**
- **Given** am-tc-region desplegado con la versión migrada en ambiente {env}
- **When** Se consulta `GET /tc/region/regionByRoute` y un downstream no responde / responde lento
- **Then** El status code es 500
- **And** reason = ERR.TC.REGION.SERVICE_UNAVAILABLE
- **And** El servicio responde en tiempo acotado (fail-fast, no se cuelga indefinidamente)

**Resultado esperado**
Status 500 y respuesta del microservicio:
```json
{
    "channel": "web",
    "httpCode": 500,
    "reason": "ERR.TC.REGION.SERVICE_UNAVAILABLE"
}
```

---

## TC005 — Métrica New Relic de errores registrada post migración
- **Case ID:** C254750 · **Title:** TC005_TTP_MT_TC REGION_METRICA NEW RELIC DE ERRORES REGISTRADA POST MIGRACION
- **Tipo:** Functional · **Prioridad:** High · **Estimado:** 2min · **Refs:** SSM-17667

**Descripción**
Validar regresión SSM-11165: la métrica de errores en New Relic sigue registrándose tras la migración.

**Precondiciones**
- Endpoint: `GET /tc/region/regionByRoute`
- Regla vigente SSM-11165 (B2): am-tc-region registra métrica de errores en New Relic.
- Riesgo de migración: Jackson 3 / agente New Relic bajo Java 25.
- Se usa un x-transactionId/transactionID de traza: nr-trace-17667-{uuid}.

**Pasos (Given/When/Then)**
- **Given** am-tc-region desplegado con la versión migrada en ambiente {env}
- **When** Se consulta `GET /tc/region/regionByRoute` con transactionID=nr-trace-17667-{uuid} en un caso que dispare la métrica de error (ej: IATA inexistente)
- **And** Se consulta New Relic (Logs / Custom Events) por el transactionId de traza
- **Then** El evento/métrica correspondiente aparece en New Relic
- **And** Los campos conservan naming y valores respecto al baseline

**Resultado esperado**
Validar trazabilidad.
1. La métrica/evento de errores se registra en New Relic (SSM-11165).
2. El transactionId de traza (nr-trace-17667-*) es localizable en New Relic.
3. El agente New Relic opera correctamente bajo Java 25 (evento efectivamente emitido).
4. Sin regresión en la observabilidad post-migración.

---

## TC006 — Error header obligatorio faltante, 400
- **Case ID:** C254751 · **Title:** TC006_TTF_MT_TC REGION_ERROR HEADER OBLIGATORIO FALTANTE 400
- **Tipo:** Destructive · **Prioridad:** Low · **Estimado:** 2min · **Refs:** SSM-17667

**Descripción**
Validar regresión: request sin un header obligatorio (ej: destination) retorna 400 ERR.TC.REGION.BAD_REQUEST tras la migración.

**Precondiciones**
- Endpoint: `GET /tc/region/regionByRoute`
- Error catalog (contrato): 400 ERR.TC.REGION.BAD_REQUEST por header requerido faltante.
- Se envía la petición sin el header destination (o sin origin/channel/transactionID).

**Pasos (Given/When/Then)**
- **Given** am-tc-region desplegado con la versión migrada en ambiente {env}
- **When** Se consulta `GET /tc/region/regionByRoute` con origin=MEX pero SIN el header destination
- **Then** El status code es 400
- **And** reason inicia con ERR.TC.REGION.BAD_REQUEST

**Resultado esperado**
Status 400 y respuesta del microservicio:
```json
{
    "channel": "web",
    "httpCode": 400,
    "reason": "ERR.TC.REGION.BAD_REQUEST.Required request parameter 'destination' ... is not present"
}
```

---

## TC007 — Error IATA inexistente, 500 Empty Response
- **Case ID:** C254752 · **Title:** TC007_TTF_MT_TC REGION_ERROR IATA INEXISTENTE 500 EMPTY RESPONSE
- **Tipo:** Destructive · **Prioridad:** Low · **Estimado:** 2min · **Refs:** SSM-17667

**Descripción**
Validar regresión: un OD con IATA inexistente (origin=ZZZ) retorna 500 ERR.TC.REGION.EMPTY_RESPONSE tras la migración.

**Precondiciones**
- Endpoint: `GET /tc/region/regionByRoute`
- Error catalog (contrato/Postman): 500 ERR.TC.REGION.EMPTY_RESPONSE cuando no hay datos para el OD.
- Se envía origin=ZZZ (IATA inexistente), destination=CUN.

**Pasos (Given/When/Then)**
- **Given** am-tc-region desplegado con la versión migrada en ambiente {env}
- **When** Se consulta `GET /tc/region/regionByRoute` con origin=ZZZ y destination=CUN
- **Then** El status code es 500
- **And** reason = ERR.TC.REGION.EMPTY_RESPONSE

**Resultado esperado**
Status 500 y respuesta del microservicio:
```json
{
    "channel": "web",
    "httpCode": 500,
    "reason": "ERR.TC.REGION.EMPTY_RESPONSE"
}
```

---

## TC008 — Estabilidad consultas consecutivas post migración
- **Case ID:** C254753 · **Title:** TC008_TTR_MT_TC REGION_ESTABILIDAD CONSULTAS CONSECUTIVAS POST MIGRACION
- **Tipo:** Functional · **Prioridad:** Medium · **Estimado:** 2min · **Refs:** SSM-17667

**Descripción**
Validar estabilidad funcional con ráfaga ligera (5-10 consultas) sin errores 500/504 tras la migración.

**Precondiciones**
- Endpoint: `GET /tc/region/regionByRoute`
- Microservicio migrado desplegado y estable. Ráfaga ligera de 5-10 consultas (estabilidad, no rendimiento).

**Pasos (Given/When/Then)**
- **Given** am-tc-region desplegado con la versión migrada en ambiente {env}
- **When** Se ejecutan 5-10 consultas `GET /tc/region/regionByRoute` consecutivas con OD válidos
- **Then** Todas las respuestas son 200
- **And** No se presentan errores 500/504 ni degradación observable

**Resultado esperado**
Validar status 200 en todas.
1. Servicio estable bajo ráfaga ligera (5-10 consultas).
2. Sin errores 500/504.
3. Estabilidad funcional post-migración (rendimiento → plan de estrés).

---

## TC009 — Version management info
- **Case ID:** C254754 · **Title:** TC009_TTP_MT_TC REGION_VERSION MANAGEMENT INFO
- **Tipo:** Functional · **Prioridad:** High · **Estimado:** 2min · **Refs:** SSM-17667

**Descripción**
Verificar versión del microservicio TC REGION vía endpoint management/info.

**Precondiciones**
- Endpoint: `GET /tc/region/management/info`

**Pasos**
1. Consultar `GET /tc/region/management/info`.
2. Verificar respuesta exitosa con status 200.
3. Confirmar que la versión desplegada corresponde al release.

**Resultado esperado**
Status 200 y respuesta del microservicio:
```json
{
    "Environment": "dev",
    "Microservice": "am-tc-region",
    "Version": "x.x.x-SNAPSHOT"
}
```

---

## Resumen

| # | Case ID | Tipo | Prioridad | Escenario |
|---|---------|------|-----------|-----------|
| TC001 | C254746 | Functional | Critical | OD Internacional (MEX-LAX) → 200 INT |
| TC002 | C254747 | Functional | High | OD Doméstico (MEX-MTY) → 200 DOM |
| TC003 | C254748 | Functional | Medium | Consultas consecutivas mismo OD → datos consistentes (caché) |
| TC004 | C254749 | Destructive | Low | Downstream caído → 500 SERVICE_UNAVAILABLE (fail-fast) |
| TC005 | C254750 | Functional | High | Métrica de error en New Relic registrada post migración |
| TC006 | C254751 | Destructive | Low | Header obligatorio faltante → 400 BAD_REQUEST |
| TC007 | C254752 | Destructive | Low | IATA inexistente → 500 EMPTY_RESPONSE |
| TC008 | C254753 | Functional | Medium | Ráfaga ligera 5-10 consultas → estabilidad, sin 500/504 |
| TC009 | C254754 | Functional | High | Endpoint management/info → versión desplegada |

*Fuente: TestRail run 13928 (casos C254746–C254754). Comentarios/resultados no incluidos por solicitud.*
