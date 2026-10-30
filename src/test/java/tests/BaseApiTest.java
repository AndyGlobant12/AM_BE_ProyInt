package tests;

import com.microsoft.playwright.APIResponse;
import config.Environment;
import config.PlaywrightApiClient;
import io.qameta.allure.Allure;
import services.RegionService;
import support.JsonSupport;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.nio.charset.StandardCharsets;

/**
 * Clase base de tests: gestiona el ciclo de vida del cliente Playwright y
 * expone el Service Wrapper listo para usar, mas helpers de reporte Allure.
 */
public abstract class BaseApiTest {

    protected PlaywrightApiClient client;
    protected RegionService regionService;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        client = new PlaywrightApiClient();
        regionService = new RegionService(client.request());
        Allure.parameter("Ambiente", Environment.name());
        Allure.parameter("Base URL", Environment.baseUrl());
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    /**
     * Adjunta el cuerpo de la respuesta (pretty JSON) al reporte Allure.
     */
    protected String attachBody(APIResponse response) {
        String raw = response.text();
        Allure.addAttachment("Response body (status " + response.status() + ")",
                "application/json",
                JsonSupport.pretty(raw),
                ".json");
        return raw;
    }

    protected void attachText(String name, String content) {
        Allure.addAttachment(name, "text/plain",
                new java.io.ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), ".txt");
    }
}
