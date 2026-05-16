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
            System.out.println("✅ Serveur HTTP créé sur port " + PORT);

            serveur.createContext("/", this::servirHtml);
            serveur.createContext("/api/data", this::servirJson);

            serveur.setExecutor(null);
            serveur.start();

            System.out.println("=" .repeat(50));
            System.out.println("📊 DASHBOARD DISPONIBLE :");
            System.out.println("   → http://localhost:" + PORT);
            System.out.println("=" .repeat(50));

        } catch (IOException e) {
            System.err.println("❌ Impossible de démarrer le serveur sur le port " + PORT);
            System.err.println("   Cause: " + e.getMessage());
            System.err.println("   Peut-être le port est déjà utilisé ?");
        }
    }

    public void arreter() {
        if (serveur != null) serveur.stop(0);
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void servirHtml(HttpExchange exchange) throws IOException {
        // Affiche le chemin de recherche pour debug
        System.out.println("Recherche du fichier dashboard.html...");

        // Essaye plusieurs méthodes pour charger le fichier
        InputStream is = getClass().getResourceAsStream("/dashboard.html");

        if (is == null) {
            // Essaye avec un chemin différent
            is = getClass().getResourceAsStream("dashboard.html");
        }

        if (is == null) {
            // Essaye de charger depuis le système de fichiers
            try {
                File file = new File("src/main/resources/dashboard.html");
                if (file.exists()) {
                    is = new FileInputStream(file);
                    System.out.println("Fichier trouvé dans src/main/resources/");
                }
            } catch (Exception e) {}
        }

        byte[] contenu;

        if (is != null) {
            contenu = is.readAllBytes();
            System.out.println("✅ dashboard.html chargé, taille: " + contenu.length + " bytes");
            is.close();
        } else {
            System.err.println("❌ dashboard.html INTROUVABLE !");
            // Crée un HTML minimal par défaut
            String htmlMinimal = """
            <!DOCTYPE html>
            <html>
            <head><title>Dashboard Lucien</title></head>
            <body>
                <h1>📊 Dashboard Lucien</h1>
                <div id="data"></div>
                <script>
                    fetch('/api/data')
                        .then(r => r.json())
                        .then(data => {
                            document.getElementById('data').innerHTML = 
                                '<pre>' + JSON.stringify(data, null, 2) + '</pre>';
                        });
                </script>
            </body>
            </html>
        """;
            contenu = htmlMinimal.getBytes(StandardCharsets.UTF_8);
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