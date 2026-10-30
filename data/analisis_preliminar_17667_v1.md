# Análisis preliminar — SSM-17667

**Microservicio:** am-tc-region · **Proyecto:** SSM (Self Service / MYB)
**Fecha de análisis:** 27/Oct/2026 · **Versión:** v1

---

## 1. Resumen ejecutivo

**SSM-17667 — T.C. Region · Actualización a Java 25, Maven 4 y mitigación de vulnerabilidades de Prisma y Veracode**

| Campo | Valor |
|---|---|
| Tipo | Story |
| Proyecto | SSM (Self Service / MYB) |
| Prioridad | Critical |
| Estado | **PO approved** (cerrado, resolución *Done* el 13/Oct/2026) |
| Épica padre | SSM-14397 — *Reliability Travel Hub/SR 2nd Half 26* |
| Microservicio | `am-tc-region` (https://github.com/EC-AMX/am-tc-region) |
| Componente | Nueva Arquitectura |
| Asignado (Dev) | Pablo Fernando Sinchi Coronel |
| QA | Jonathan Marin / Christian Hernández (QA Lead) |
| PO | Isabel Gallardo |
| Release | R26.10.02A |
| Sprint | MT-B5-04 (cerrado 20/Oct/2026) |

Es una historia de **upgrade tecnológico y seguridad**, no funcional: migrar `am-tc-region` de Java 17 → **Java 25**, Maven 3 → **Maven 4**, y cerrar los hallazgos de **Prisma Cloud** y **Veracode**, manteniendo el comportamiento del servicio sin cambios de contrato. Pertenece a la iniciativa transversal de Ciberseguridad que está migrando toda la nueva arquitectura a Java 25.

El servicio `am-tc-region` es un **microservicio compositor** que resuelve la región final de un vuelo a partir de los códigos IATA de salida/llegada, orquestando tres microservicios base (`am-tb-region`, `am-tb-airports-info`, `am-tb-timezone`) vía `RestTemplate`. Sin base de datos, sin Spring Cloud/Feign, sin AWS SDK, sin Spring Security.

---

## 2. Alcance técnico (según la Solución Técnica adjunta en el ticket)

El propio ticket trae un documento de solución técnica muy completo. Lo esencial:

**Baseline de vulnerabilidades (escaneo Trivy del 21/Sep/2026):** 15 findings HIGH/CRITICAL en `pom.xml`, **100% transitivos** del BOM de Spring Boot 3.5.14:

- **Tomcat embebido 10.1.54** → 9 vulns (6 CRITICAL + 3 HIGH) — el driver de urgencia.
- **Jackson 2.21.2** → 3 HIGH.
- **Spring Framework 6.2.18** → 3 HIGH.

Ninguna dependencia declarada directamente (ModelMapper, Gson, newrelic-api, springdoc, logback) tiene CVEs propios.

**Estrategia de remediación:** subir el parent a **Spring Boot 4.1.0**, que arrastra Spring Framework 7 + Jakarta EE 11 + Tomcat 11 + Jackson 3, cerrando los 15 findings de una sola vez, y habilita el salto a Java 25 LTS.

**Matriz de versiones objetivo:**

| Componente | Actual | Objetivo |
|---|---|---|
| Java | 17 | **25 (LTS)** |
| Spring Boot | 3.5.14 | **4.1.0** |
| Starter web | `spring-boot-starter-web` | **`spring-boot-starter-webmvc`** (renombrado en Boot 4) |
| Lombok | sin pin | **1.18.40** |
| JaCoCo | 0.8.12 | **0.8.15** |
| springdoc-openapi | 2.8.6 | **3.0.3** |
| Maven | sin wrapper | 3.9.16 + Maven Wrapper |

**Breaking changes con impacto real en código** (bajo, muy acotado):

- Renombrar el starter a `spring-boot-starter-webmvc`.
- Reemplazar `@MockBean`/`@SpyBean` (eliminados en Boot 4) por `@MockitoBean` en 3 tests.
- `spring.jackson.use-jackson2-defaults=true` para preservar contratos de serialización (Jackson 3 cambia defaults).
- Subir springdoc a 3.0.3 (si no, no arranca con Boot 4).

**Riesgos principales señalados:** gate de cobertura del 90% (JaCoCo 0.8.12 no instrumenta bytecode 69), reflexión de ModelMapper/Gson bajo la encapsulación fuerte de Java 25, compatibilidad del **agente** New Relic con Java 25, y disponibilidad de la **imagen base JDK 25 en ECR** al desplegar. También recomienda **Virtual Threads** como mejora post-migración (fuera de alcance).

---

## 3. Jerarquía y subtareas

Épica **SSM-14397** (*Reliability Travel Hub/SR 2nd Half 26*). La historia se entregó con **12 subtareas, todas en Task Done**, cubriendo el ciclo completo:

- Análisis y réplica de ambiente, desarrollo de la solución, pruebas unitarias, validación/FWT, Pull Request, validación de integración.
- Pruebas funcionales, BDD y de estrés en DEV; funcionales y BDD en QA; correo a QA.

Evidencias en TestRail (runs 13927/13928 funcionales, 13939/13940 BDD) y QA validada por Jonathan Marin. DoR 100%, DoD con cobertura ≥90% y sin errores en SonarQube.

---

## 4. Items relacionados (misma iniciativa de Ciberseguridad / Java 25)

SSM-17667 **no es un caso aislado**: es parte de una ola de upgrades idénticos sobre la nueva arquitectura.

**Historias de upgrade Java 25 con el mismo patrón (mismo título / misma plantilla de subtareas):**

| Clave | Microservicio | Estado |
|---|---|---|
| SSM-19301 | T.C. Notifications — misma plantilla exacta "Java 25, Maven 4, Prisma y Veracode" | Task Done |
| SSM-19300 | T.B. Flight Amenities — misma plantilla exacta | Task Done |
| SSM-19200 | am-sb-search-flight | Ready for QA |
| SSM-19199 | am-sb-special-refund | Ready to QA Approval |
| SSM-19198 | am-sb-create-compensation | Ready for Integration |
| SSM-19195 | am-rc-flight-event-dispatcher | PO approved |
| SSM-19194 | am-rb-flight-consumer-kafka | PO approved |
| SSM-15188 | am-sc-pax-list | PO approved |
| SSM-15189 | am-sc-flight-confirmation | PO approved |
| SSM-15190 | am-sb-voucher-sp | PO approved |
| SSM-15191 | am-sc-volantio | PO approved |
| SSM-15192 | am-sb-ticket-info | Released |
| SSM-15193 | am-sb-special-emd | PO approved |
| SSM-15194 | am-sb-session-hotel | PO approved |
| SSM-15196 | am-sb-verification-compensation | PO approved |
| SSM-15178 | am-sb-flight-confirmation-sp | Released |

**Más historias de seguridad relacionadas:** SSM-19519 / SSM-18553 / SSM-18473 (*Vulnerabilidades B5-Sx*, épica Portal A&B), SSM-19017 (T.C. Reservation — validación de apellido para mitigar vulnerabilidades, misma épica SSM-14397), y los upgrades SSM-19437 / SSM-19436 (am-sb-single-billing / stamping-billing, con subtareas de reportes Sonar+Veracode).

**Observaciones de esas relaciones:**

- Hay **dos redacciones de subtareas** conviviendo: unas con un flujo de upgrade muy detallado (scaffolding, ajustar build, re-ejecutar Veracode, validar compatibilidad, README) y la plantilla "normalizada" idéntica que usan SSM-19301 y SSM-19300. SSM-17667 sigue el modelo con subtareas de ciclo QA (DEV/QA funcional+BDD+estrés).
- Los desarrolladores recurrentes de esta iniciativa son **Jose Luis Gonzalez Villalba** y **Juan Jose Gonzalez Vargas** (la mayoría de los upgrades Java 25); `am-tc-region` lo hizo Pablo Sinchi.
- Varios hermanos siguen **en vuelo** (SSM-19198/19199/19200), por lo que la solución técnica de SSM-17667 (migración vía Spring Boot 4.x) es un **patrón reutilizable** para esos pendientes.

---

## 5. Hallazgos y recomendaciones

1. **El ticket ya está cerrado (PO approved).** Si el objetivo es replicar el trabajo en otros servicios, la Solución Técnica embebida en SSM-17667 es prácticamente una guía paso a paso lista para reutilizar en SSM-19198/19199/19200 y futuros upgrades.

2. **Inconsistencia de versión del parent entre tickets.** SSM-17667 documenta objetivo **Spring Boot 4.1.0**; el hermano SSM-19613 (am-sb-special-refund) usa **Spring Boot 4.1.1**. Conviene alinear una versión estándar de parent para toda la iniciativa y evitar divergencia.

3. **Deuda técnica identificada pero fuera de alcance** (quedó documentada, no resuelta): `RestTemplate` sin timeouts (CWE-404, prioridad alta por ser compositor I/O-bound con 3 llamadas síncronas), ausencia de circuit breaker/retry, `@EnableCaching` duplicado, URLs hardcodeadas en Swagger, y README desactualizado. Vale la pena crear tickets de seguimiento si no existen.

4. **Dependencias de plataforma** que son riesgo cruzado para toda la ola: imagen base JDK 25 en ECR y compatibilidad del agente New Relic con Java 25. Si no están confirmadas a nivel plataforma, bloquean cualquiera de los upgrades hermanos.

5. **Maven 4 no se adoptó realmente.** La solución técnica recomienda mantener modo compatibilidad sobre Maven 3.9.16 + wrapper, porque Maven 4 seguía en RC sin GA. El título del ticket dice "Maven 4" pero la entrega efectiva fue 3.9.16. Esto conviene aclararlo para no asumir que Maven 4 quedó en producción.

---

*Fuente: Jira SSM-17667 y búsqueda de items relacionados en el proyecto SSM. Documento de análisis preliminar.*
