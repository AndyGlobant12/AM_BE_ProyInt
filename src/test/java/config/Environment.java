package config;

/**
 * Resuelve la base URL del microservicio am-tc-region segun el ambiente activo.
 *
 * El ambiente se selecciona con la propiedad de sistema {@code -Denv=dev|qa|local}.
 * Por defecto se usa {@code dev}. Las URLs provienen de la coleccion Postman
 * am-tc-region.postman_collection-v3.json.
 */
public final class Environment {

    private Environment() {
        // clase de utilidad
    }

    public static String baseUrl() {
        String env = System.getProperty("env", "dev").toLowerCase();
        switch (env) {
            case "local":
                return "http://localhost:8085";
            case "qa":
                return "https://amx-c-mtpsbk-q.amlab7.com";
            case "dev":
            default:
                return "https://amx-c-mtpsbk-de.amlab7.com";
        }
    }

    public static String name() {
        return System.getProperty("env", "dev").toLowerCase();
    }
}
