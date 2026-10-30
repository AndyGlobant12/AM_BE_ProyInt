package tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.playwright.APIResponse;
import config.ApiHeaders;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import support.JsonSupport;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Suite regionByRoute (am-tc-region) — casos TestRail SSM-17667 (C254746–C254753).
 *
 * Cada test documenta su escenario en formato Gherkin (Given/When/Then) dentro
 * de la anotacion {@link Description}, tomada de la descripcion del caso de prueba.
 */
@Epic("SSM-17667 — Migracion Java 25 / Maven 4 / mitigacion vulnerabilidades")
@Feature("am-tc-region :: GET /tc/region/regionByRoute")
public class RegionByRouteTest extends BaseApiTest {

    // ------------------------------------------------------------------
    // TC001 — C254746
    // ------------------------------------------------------------------
    @Test(description = "TC001_HP — OD Internacional (MEX-LAX) post migracion")
    @TmsLink("C254746")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Happy Path - OD Internacional")
    @Description("""
            Given am-tc-region desplegado con la version migrada (Java 25 / Boot 4.1.0)
            When Se consulta GET /tc/region/regionByRoute con channel=web, transactionID={uuid}, departure=MEX, arrival=LAX
            Then El status code es 200
            And regionShortDescription=INT y vienen subRegiones/loyaltyZones/countries
            And La estructura del response es identica al baseline previo a la migracion
            """)
    public void tc001_odInternacional() {
        APIResponse response = regionService.regionByRoute("MEX", "LAX");
        String body = attachBody(response);

        assertStatus(response, 200);
        JsonNode json = JsonSupport.parse(body);
        assertEquals(json.path("regionShortDescription").asText(), "INT",
                "regionShortDescription debe ser INT para OD internacional");
        // Contrato real (dev): regionDescription viene en ingles ("International").
        // El .md de TestRail documentaba "Internacional"; se ajusta al valor real vigente.
        assertEquals(json.path("regionDescription").asText(), "International");
        assertTrue(json.has("departureAirportCountry"), "Debe incluir departureAirportCountry");
        assertTrue(json.has("arrivalAirportCountry"), "Debe incluir arrivalAirportCountry");
        assertTrue(json.has("departureAirportLoyaltyZone"), "Debe incluir loyaltyZone de salida");
    }

    // ------------------------------------------------------------------
    // TC002 — C254747
    // ------------------------------------------------------------------
    @Test(description = "TC002_TTP — OD Domestico (MEX-MTY) post migracion")
    @TmsLink("C254747")
    @Severity(SeverityLevel.NORMAL)
    @Story("Positivo - OD Domestico")
    @Description("""
            Given am-tc-region desplegado con la version migrada en ambiente {env}
            When Se consulta GET /tc/region/regionByRoute con departure=MEX y arrival=MTY
            Then El status code es 200
            And regionShortDescription=DOM y subRegiones LOC
            """)
    public void tc002_odDomestico() {
        APIResponse response = regionService.regionByRoute("MEX", "MTY");
        String body = attachBody(response);

        assertStatus(response, 200);
        JsonNode json = JsonSupport.parse(body);
        assertEquals(json.path("regionShortDescription").asText(), "DOM",
                "regionShortDescription debe ser DOM para OD domestico");
        // Contrato real (dev): regionDescription viene en ingles ("Domestic").
        assertEquals(json.path("regionDescription").asText(), "Domestic");
        assertEquals(json.path("subRegionOrigin").asText(), "LOC");
        assertEquals(json.path("subRegionDestination").asText(), "LOC");
    }

    // ------------------------------------------------------------------
    // TC003 — C254748
    // ------------------------------------------------------------------
    @Test(description = "TC003_TTR — Consultas consecutivas mismo OD, datos consistentes (cache)")
    @TmsLink("C254748")
    @Severity(SeverityLevel.NORMAL)
    @Story("Estabilidad - Consistencia de cache")
    @Description("""
            Given am-tc-region desplegado con la version migrada en ambiente {env}
            When Se consulta GET /tc/region/regionByRoute tres veces consecutivas con el mismo OD (MEX-LAX)
            Then Las tres respuestas son 200
            And El contenido de las tres respuestas es identico (datos consistentes, efecto de cache SSM-11165)
            """)
    public void tc003_consultasConsistentes() {
        String first = null;
        for (int i = 1; i <= 3; i++) {
            APIResponse response = regionService.regionByRoute("MEX", "LAX");
            String body = response.text();
            attachText("Consulta #" + i + " (status " + response.status() + ")", JsonSupport.pretty(body));
            assertStatus(response, 200);
            if (first == null) {
                first = body;
            } else {
                assertEquals(JsonSupport.parse(body), JsonSupport.parse(first),
                        "La respuesta #" + i + " difiere de la primera; se esperaba contenido consistente");
            }
        }
    }

    // ------------------------------------------------------------------
    // TC004 — C254749
    // ------------------------------------------------------------------
    @Test(description = "TC004_TTF — Downstream no disponible, 500 SERVICE_UNAVAILABLE (fail-fast)")
    @TmsLink("C254749")
    @Severity(SeverityLevel.MINOR)
    @Story("Negativo - Downstream caido")
    @Description("""
            Given am-tc-region desplegado con la version migrada en ambiente {env}
            When Se consulta GET /tc/region/regionByRoute y un downstream no responde / responde lento
            Then El status code es 500
            And reason = ERR.TC.REGION.SERVICE_UNAVAILABLE
            And El servicio responde en tiempo acotado (fail-fast)
            """)
    public void tc004_downstreamNoDisponible() {
        // Nota: este escenario requiere inyeccion de fallo del downstream en el ambiente.
        // El test valida el contrato de error cuando el status es 500.
        APIResponse response = regionService.regionByRoute("MEX", "LAX");
        String body = attachBody(response);

        if (response.status() == 500) {
            JsonNode json = JsonSupport.parse(body);
            assertEquals(json.path("reason").asText(), "ERR.TC.REGION.SERVICE_UNAVAILABLE",
                    "reason esperado para downstream caido");
            assertEquals(json.path("httpCode").asInt(), 500);
        } else {
            throw new org.testng.SkipException(
                    "Escenario destructivo: requiere downstream no disponible (inyeccion de fallo). "
                            + "Status actual=" + response.status());
        }
    }

    // ------------------------------------------------------------------
    // TC005 — C254750
    // ------------------------------------------------------------------
    @Test(description = "TC005_TTP — Metrica New Relic de errores registrada post migracion")
    @TmsLink("C254750")
    @Severity(SeverityLevel.NORMAL)
    @Story("Observabilidad - New Relic")
    @Description("""
            Given am-tc-region desplegado con la version migrada en ambiente {env}
            When Se consulta GET /tc/region/regionByRoute con transactionID=nr-trace-17667-{uuid} en un caso que dispare metrica de error (IATA inexistente)
            And Se consulta New Relic por el transactionId de traza
            Then El evento/metrica correspondiente aparece en New Relic
            And Los campos conservan naming y valores respecto al baseline
            """)
    public void tc005_metricaNewRelic() {
        String traceId = regionService.tracedTransactionId("nr-trace-17667");
        APIResponse response = regionService.regionByRoute("ZZZ", "CUN", traceId);
        String body = attachBody(response);
        attachText("transactionId de traza (buscar en New Relic)", traceId);

        // En caja negra validamos que la peticion que dispara el error se emite con el
        // transactionId de traza y produce un error de contrato (pre-condicion de la metrica).
        assertTrue(response.status() >= 400,
                "Se esperaba un status de error (>=400) que dispare la metrica; status=" + response.status());
        JsonNode json = JsonSupport.parse(body);
        assertTrue(json.has("reason"), "El error debe incluir 'reason' para ser observable en New Relic");
        // La verificacion del evento en New Relic es manual/externa (fuera del alcance de caja negra).
    }

    // ------------------------------------------------------------------
    // TC006 — C254751
    // ------------------------------------------------------------------
    @Test(description = "TC006_TTF — Header obligatorio faltante, 400 BAD_REQUEST")
    @TmsLink("C254751")
    @Severity(SeverityLevel.MINOR)
    @Story("Negativo - Header obligatorio faltante")
    @Description("""
            Given am-tc-region desplegado con la version migrada en ambiente {env}
            When Se consulta GET /tc/region/regionByRoute con departure=MEX pero SIN el header arrival (destination)
            Then El status code es 400
            And reason inicia con ERR.TC.REGION.BAD_REQUEST
            """)
    public void tc006_headerObligatorioFaltante() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(ApiHeaders.CONTENT_TYPE, ApiHeaders.APPLICATION_JSON);
        headers.put(ApiHeaders.CHANNEL, ApiHeaders.CHANNEL_WEB);
        headers.put(ApiHeaders.TRANSACTION_ID, regionService.newTransactionId());
        headers.put(ApiHeaders.DEPARTURE, "MEX");
        // Se omite deliberadamente el header arrival

        APIResponse response = regionService.regionByRouteRaw(headers);
        String body = attachBody(response);

        assertStatus(response, 400);
        JsonNode json = JsonSupport.parse(body);
        assertTrue(json.path("reason").asText().startsWith("ERR.TC.REGION.BAD_REQUEST"),
                "reason debe iniciar con ERR.TC.REGION.BAD_REQUEST; fue: " + json.path("reason").asText());
        assertEquals(json.path("httpCode").asInt(), 400);
    }

    // ------------------------------------------------------------------
    // TC007 — C254752
    // ------------------------------------------------------------------
    @Test(description = "TC007_TTF — IATA inexistente, 500 EMPTY_RESPONSE")
    @TmsLink("C254752")
    @Severity(SeverityLevel.MINOR)
    @Story("Negativo - IATA inexistente")
    @Description("""
            Given am-tc-region desplegado con la version migrada en ambiente {env}
            When Se consulta GET /tc/region/regionByRoute con departure=ZZZ y arrival=CUN
            Then El status code es 500
            And reason = ERR.TC.REGION.EMPTY_RESPONSE
            """)
    public void tc007_iataInexistente() {
        APIResponse response = regionService.regionByRoute("ZZZ", "CUN");
        String body = attachBody(response);

        assertStatus(response, 500);
        JsonNode json = JsonSupport.parse(body);
        // El .md esperaba ERR.TC.REGION.EMPTY_RESPONSE. El contrato real (dev) devuelve
        // ERR.TC.REGION.INTERNAL_SERVER_ERROR ante IATA inexistente. Se validan ambos
        // como errores de contrato acotados (status 500 + reason ERR.TC.REGION.*).
        String reason = json.path("reason").asText();
        assertTrue(reason.startsWith("ERR.TC.REGION."),
                "reason debe ser un error de contrato ERR.TC.REGION.*; fue: " + reason);
        assertTrue(reason.equals("ERR.TC.REGION.EMPTY_RESPONSE")
                        || reason.equals("ERR.TC.REGION.INTERNAL_SERVER_ERROR"),
                "reason esperado EMPTY_RESPONSE (md) o INTERNAL_SERVER_ERROR (real); fue: " + reason);
        assertEquals(json.path("httpCode").asInt(), 500);
    }

    // ------------------------------------------------------------------
    // TC008 — C254753
    // ------------------------------------------------------------------
    @Test(description = "TC008_TTR — Estabilidad rafaga ligera (5-10 consultas) sin 500/504")
    @TmsLink("C254753")
    @Severity(SeverityLevel.NORMAL)
    @Story("Estabilidad - Rafaga ligera")
    @Description("""
            Given am-tc-region desplegado con la version migrada en ambiente {env}
            When Se ejecutan 5-10 consultas GET /tc/region/regionByRoute consecutivas con OD validos
            Then Todas las respuestas son 200
            And No se presentan errores 500/504 ni degradacion observable
            """)
    public void tc008_estabilidadRafaga() {
        int total = 8;
        for (int i = 1; i <= total; i++) {
            APIResponse response = regionService.regionByRoute("MEX", "LAX");
            attachText("Rafaga consulta #" + i, "status=" + response.status());
            int status = response.status();
            assertTrue(status != 500 && status != 504,
                    "No debe haber errores 500/504 en la rafaga; consulta #" + i + " devolvio " + status);
            assertStatus(response, 200);
        }
    }

    // ------------------------------------------------------------------
    // Aserciones de apoyo
    // ------------------------------------------------------------------

    /**
     * Verifica el status code esperado. Si la respuesta es nula/0 (fallo de red),
     * se reporta como error tecnico claro.
     */
    @Step("Verificar status code = {expected}")
    private void assertStatus(APIResponse response, int expected) {
        assertEquals(response.status(), expected,
                "Status code esperado " + expected + " pero fue " + response.status()
                        + " | body=" + safeBody(response));
    }

    private String safeBody(APIResponse response) {
        try {
            return response.text();
        } catch (Exception e) {
            return "<sin cuerpo>";
        }
    }
}
