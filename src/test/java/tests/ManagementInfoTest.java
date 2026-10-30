package tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.playwright.APIResponse;
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Suite management/info (am-tc-region) — caso TestRail SSM-17667 (C254754).
 */
@Epic("SSM-17667 — Migracion Java 25 / Maven 4 / mitigacion vulnerabilidades")
@Feature("am-tc-region :: GET /tc/region/management/info")
public class ManagementInfoTest extends BaseApiTest {

    // ------------------------------------------------------------------
    // TC009 — C254754
    // ------------------------------------------------------------------
    @Test(description = "TC009_TTP — Version management info")
    @TmsLink("C254754")
    @Severity(SeverityLevel.NORMAL)
    @Story("Version management info")
    @Description("""
            Given am-tc-region desplegado con la version migrada en ambiente {env}
            When Se consulta GET /tc/region/management/info
            Then El status code es 200
            And La respuesta incluye Environment, Microservice=am-tc-region y Version
            """)
    public void tc009_managementInfo() {
        APIResponse response = regionService.managementInfo();
        String body = attachBody(response);

        assertStatus(response, 200);
        JsonNode json = JsonSupport.parse(body);
        assertEquals(json.path("Microservice").asText(), "am-tc-region",
                "El microservicio reportado debe ser am-tc-region");
        assertTrue(json.has("Version"), "La respuesta debe incluir la Version desplegada");
        assertTrue(json.has("Environment"), "La respuesta debe incluir el Environment");
    }

    @Step("Verificar status code = {expected}")
    private void assertStatus(APIResponse response, int expected) {
        assertEquals(response.status(), expected,
                "Status code esperado " + expected + " pero fue " + response.status());
    }
}
