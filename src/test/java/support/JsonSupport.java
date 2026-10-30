package support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utilidades para parsear y adjuntar el cuerpo JSON de las respuestas.
 */
public final class JsonSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSupport() {
    }

    public static JsonNode parse(String body) {
        try {
            return MAPPER.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo parsear el cuerpo como JSON: " + body, e);
        }
    }

    public static String pretty(String body) {
        try {
            Object json = MAPPER.readValue(body, Object.class);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            return body;
        }
    }
}
