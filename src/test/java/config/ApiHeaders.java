package config;

/**
 * Constantes con los nombres de headers del contrato am-tc-region.
 *
 * Nota de contrato: el endpoint real usa los headers {@code departure} y
 * {@code arrival} para el origen/destino (no origin/destination), tal como
 * aparece en la coleccion Postman.
 */
public final class ApiHeaders {

    private ApiHeaders() {
    }

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String TRANSACTION_ID = "x-transactionId";
    public static final String CHANNEL = "channel";
    public static final String DEPARTURE = "departure";
    public static final String ARRIVAL = "arrival";

    public static final String CHANNEL_WEB = "web";
    public static final String APPLICATION_JSON = "application/json";
}
