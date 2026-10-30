package config;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;

/**
 * Encapsula el ciclo de vida de Playwright y expone un {@link APIRequestContext}
 * apuntando a la base URL del ambiente activo.
 *
 * Playwright for Java se usa aqui como cliente HTTP de APIs (APIRequest), sin
 * navegador. Este cliente es creado y cerrado por la clase base de tests.
 */
public class PlaywrightApiClient implements AutoCloseable {

    private final Playwright playwright;
    private final APIRequestContext requestContext;

    public PlaywrightApiClient() {
        this.playwright = Playwright.create();
        this.requestContext = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL(Environment.baseUrl())
                        .setIgnoreHTTPSErrors(true)
        );
    }

    public APIRequestContext request() {
        return requestContext;
    }

    @Override
    public void close() {
        if (requestContext != null) {
            requestContext.dispose();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}
