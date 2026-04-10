import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class DashboardServeur {

    private final TacheManager    tacheManager;
    private final HistoriqueManager historiqueManager;
    private HttpServer             serveur;

    private static final int PORT = 8080;

    public DashboardServeur(TacheManager tm, HistoriqueManager hm) {
        this.tacheManager    = tm;
        this.historiqueManager = hm;
    }

    public void demarrer() {
        try {
            serveur = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Route : page HTML principale
            serveur.createContext("/", this::servirHtml);

            // Route : données JSON en temps réel
            serveur.createContext("/api/data", this::servirJson);

            serveur.setExecutor(null);
            serveur.start();
            System.out.println("[Dashboard] Accessible sur http://localhost:" + PORT);

        } catch (IOException e) {
            System.err.println("[Dashboard] Impossible de démarrer : " + e.getMessage());
        }
    }

    public void arreter() {
        if (serveur != null) serveur.stop(0);
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void servirHtml(HttpExchange exchange) throws IOException {
        // On lit le fichier dashboard.html depuis le classpath
        InputStream is = getClass().getResourceAsStream("/dashboard.html");
        byte[] contenu;

        if (is != null) {
            contenu = is.readAllBytes();
        } else {
            contenu = "<h1>dashboard.html introuvable</h1>".getBytes(StandardCharsets.UTF_8);
        }

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, contenu.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(contenu);
        }
    }

    private void servirJson(HttpExchange exchange) throws IOException {
        // CORS pour dev local
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        byte[] json = historiqueManager
                .toJsonDashboard(tacheManager)
                .getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(200, json.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json);
        }
    }
}