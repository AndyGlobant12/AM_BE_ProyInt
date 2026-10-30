package services;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import config.ApiHeaders;
import io.qameta.allure.Step;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service Wrapper del microservicio am-tc-region.
 *
 * Es el equivalente del Page Object Model aplicado a servicios: encapsula los
 * endpoints, los headers y la construccion de las peticiones, de modo que las
 * clases de test solo expresen el "que" (escenario) y no el "como" (HTTP).
 *
 * Endpoints cubiertos:
 *  - GET /tc/region/regionByRoute
 *  - GET /tc/region/management/info
 */
public class RegionService {

    private static final String REGION_BY_ROUTE = "/tc/region/regionByRoute";
    private static final String MANAGEMENT_INFO = "/tc/region/management/info";

    private final APIRequestContext request;

    public RegionService(APIRequestContext request) {
        this.request = request;
    }

    /**
     * Consulta regionByRoute con un OD completo (departure + arrival).
     */
    @Step("GET /tc/region/regionByRoute | departure={departure} arrival={arrival}")
    public APIResponse regionByRoute(String departure, String arrival) {
        return regionByRoute(departure, arrival, newTransactionId());
    }

    /**
     * Variante con transactionId explicito (util para trazabilidad, ej. New Relic).
     */
    @Step("GET /tc/region/regionByRoute | departure={departure} arrival={arrival} txId={transactionId}")
    public APIResponse regionByRoute(String departure, String arrival, String transactionId) {
        Map<String, String> headers = baseHeaders(transactionId);
        if (departure != null) {
            headers.put(ApiHeaders.DEPARTURE, departure);
        }
        if (arrival != null) {
            headers.put(ApiHeaders.ARRIVAL, arrival);
        }
        return request.get(REGION_BY_ROUTE, withHeaders(headers));
    }

    /**
     * Consulta cruda: permite enviar exactamente el set de headers dado.
     * Util para casos negativos donde se omite un header obligatorio.
     */
    @Step("GET /tc/region/regionByRoute | headers personalizados")
    public APIResponse regionByRouteRaw(Map<String, String> headers) {
        return request.get(REGION_BY_ROUTE, withHeaders(headers));
    }

    /**
     * Construye un {@link RequestOptions} aplicando los headers uno a uno
     * (la API de Playwright no expone un setHeaders(Map) para RequestOptions).
     */
    private RequestOptions withHeaders(Map<String, String> headers) {
        RequestOptions options = RequestOptions.create();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            options = options.setHeader(entry.getKey(), entry.getValue());
        }
        return options;
    }

    /**
     * Consulta el endpoint de versionado del microservicio.
     */
    @Step("GET /tc/region/management/info")
    public APIResponse managementInfo() {
        return request.get(MANAGEMENT_INFO);
    }

    // ------------------------------------------------------------------
    // Helpers de construccion de headers
    // ------------------------------------------------------------------

    /**
     * Headers base comunes a todas las llamadas (Content-Type, channel, x-transactionId).
     */
    public Map<String, String> baseHeaders(String transactionId) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(ApiHeaders.CONTENT_TYPE, ApiHeaders.APPLICATION_JSON);
        headers.put(ApiHeaders.CHANNEL, ApiHeaders.CHANNEL_WEB);
        headers.put(ApiHeaders.TRANSACTION_ID, transactionId);
        return headers;
    }

    public String newTransactionId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Genera un transactionId de traza con prefijo (ej. para localizar en New Relic).
     */
    public String tracedTransactionId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
