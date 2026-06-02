import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class DashboardServeur {

    private final TacheManager    tacheManager;
    private final HistoriqueManager historiqueManager;
    private HttpServer             serveur;

    private static final int PORT = 8082;

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
            serveur.createContext("/api/taches", this::ajouterTacheAPI);      // ← CORRIGÉ
            serveur.createContext("/api/supprimer", this::supprimerTacheAPI); // ← CORRIGÉ
            serveur.createContext("/api/modifier", this::modifierTacheAPI);   // ← CORRIGÉ

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
        System.out.println("Recherche du fichier dashboard.html...");

        InputStream is = getClass().getResourceAsStream("/dashboard.html");

        if (is == null) {
            is = getClass().getResourceAsStream("dashboard.html");
        }

        if (is == null) {
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

    // ── CRUD API ──────────────────────────────────────────────────────────────

    private void ajouterTacheAPI(HttpExchange exchange) throws IOException {
        // Gérer les requêtes OPTIONS (CORS preflight)
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("📥 Requête ajout: " + body);

        String nom = extraireValeurJson(body, "nom");
        String debut = extraireValeurJson(body, "debut");
        String reset = extraireValeurJson(body, "reset");

        boolean success = tacheManager.ajouterTache(nom, debut, reset);

        String reponse = success ?
                "{\"success\": true, \"message\": \"Tâche ajoutée avec succès\"}" :
                "{\"success\": false, \"message\": \"Tâche existe déjà ou erreur\"}";

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(success ? 200 : 400, reponse.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(reponse.getBytes());
        }
    }

    private void supprimerTacheAPI(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("📥 Requête suppression: " + body);

        String nom = extraireValeurJson(body, "nom");

        boolean success = tacheManager.supprimerTacheParNom(nom);

        String reponse = success ?
                "{\"success\": true, \"message\": \"Tâche supprimée\"}" :
                "{\"success\": false, \"message\": \"Tâche non trouvée\"}";

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(success ? 200 : 404, reponse.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(reponse.getBytes());
        }
    }

    private void modifierTacheAPI(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("📥 Requête modification: " + body);

        String ancienNom = extraireValeurJson(body, "ancienNom");
        String nouveauNom = extraireValeurJson(body, "nouveauNom");
        String debut = extraireValeurJson(body, "debut");
        String reset = extraireValeurJson(body, "reset");

        boolean success = tacheManager.modifierTacheParNom(ancienNom, nouveauNom, debut, reset);

        String reponse = success ?
                "{\"success\": true, \"message\": \"Tâche modifiée\"}" :
                "{\"success\": false, \"message\": \"Tâche non trouvée\"}";

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(success ? 200 : 404, reponse.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(reponse.getBytes());
        }
    }

    private String extraireValeurJson(String json, String cle) {
        String pattern = "\"" + cle + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }
}