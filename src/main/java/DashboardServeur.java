import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class DashboardServeur {

    private final TacheManager     tacheManager;
    private final HistoriqueManager historiqueManager;
    private HttpServer              serveur;
    private static final int        PORT = 8080;

    public DashboardServeur(TacheManager tm, HistoriqueManager hm) {
        this.tacheManager     = tm;
        this.historiqueManager = hm;
    }

    public void demarrer() {
        try {
            serveur = HttpServer.create(new InetSocketAddress(PORT), 0);
            serveur.createContext("/",            this::servirHtml);
            serveur.createContext("/api/data",    this::servirJson);
            serveur.createContext("/api/taches",  this::gererTaches);
            serveur.setExecutor(null);
            serveur.start();
            System.out.println("[Dashboard] http://localhost:" + PORT);
        } catch (IOException e) {
            System.err.println("[Dashboard] Démarrage impossible : " + e.getMessage());
        }
    }

    public void arreter() {
        if (serveur != null) serveur.stop(0);
    }

    // ── Route HTML ────────────────────────────────────────────────────────────

    private void servirHtml(HttpExchange ex) throws IOException {
        InputStream is = getClass().getResourceAsStream("/dashboard.html");
        byte[] contenu = is != null
                ? is.readAllBytes()
                : "<h1>dashboard.html introuvable</h1>".getBytes(StandardCharsets.UTF_8);
        repondre(ex, 200, "text/html; charset=UTF-8", contenu);
    }

    // ── Route données en direct ───────────────────────────────────────────────

    private void servirJson(HttpExchange ex) throws IOException {
        byte[] json = historiqueManager
                .toJsonDashboard(tacheManager)
                .getBytes(StandardCharsets.UTF_8);
        repondre(ex, 200, "application/json; charset=UTF-8", json);
    }

    // ── Route CRUD tâches ─────────────────────────────────────────────────────

    private void gererTaches(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        String methode = ex.getRequestMethod().toUpperCase();

        switch (methode) {
            case "OPTIONS" -> repondre(ex, 204, "text/plain", new byte[0]);

            // GET /api/taches → liste complète
            case "GET" -> {
                byte[] json = tachesEnJson().getBytes(StandardCharsets.UTF_8);
                repondre(ex, 200, "application/json; charset=UTF-8", json);
            }

            // POST /api/taches  body: {"nom":"...","debut":"HH:mm","reset":"HH:mm"}
            case "POST" -> {
                String body = lireBody(ex);
                try {
                    String nom   = extraire(body, "nom");
                    String debut = extraire(body, "debut");
                    String reset = extraire(body, "reset");
                    tacheManager.ajouterTache(nom, debut, reset);
                    repondreOk(ex, "Tâche ajoutée");
                } catch (Exception e) {
                    repondre(ex, 400, "text/plain", "Données invalides".getBytes());
                }
            }

            // PUT /api/taches?index=2  body: {"nom":"...","debut":"HH:mm","reset":"HH:mm"}
            case "PUT" -> {
                int index = indexDepuisQuery(ex);
                String body = lireBody(ex);
                try {
                    String nom   = extraire(body, "nom");
                    String debut = extraire(body, "debut");
                    String reset = extraire(body, "reset");
                    boolean ok = tacheManager.modifierTache(index, nom, debut, reset);
                    repondreOk(ex, ok ? "Tâche modifiée" : "Index invalide");
                } catch (Exception e) {
                    repondre(ex, 400, "text/plain", "Données invalides".getBytes());
                }
            }

            // DELETE /api/taches?index=2
            case "DELETE" -> {
                int index = indexDepuisQuery(ex);
                boolean ok = tacheManager.supprimerTache(index);
                repondreOk(ex, ok ? "Tâche supprimée" : "Index invalide");
            }

            default -> repondre(ex, 405, "text/plain", "Méthode non autorisée".getBytes());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String tachesEnJson() {
        var taches = tacheManager.getTaches();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < taches.size(); i++) {
            Tache t = taches.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format(
                    "{\"index\":%d,\"nom\":\"%s\",\"debut\":\"%s\",\"reset\":\"%s\",\"validee\":%b}",
                    i, t.getNom(), t.getHeureDebut(), t.getHeureReset(), t.isEstValidee()
            ));
        }
        return sb.append("]").toString();
    }

    private String lireBody(HttpExchange ex) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8))) {
            return br.lines().collect(Collectors.joining());
        }
    }

    private int indexDepuisQuery(HttpExchange ex) {
        String query = ex.getRequestURI().getQuery(); // "index=2"
        if (query != null && query.startsWith("index=")) {
            try { return Integer.parseInt(query.substring(6)); } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    private String extraire(String json, String cle) {
        String cherche = "\"" + cle + "\":\"";
        int debut = json.indexOf(cherche) + cherche.length();
        int fin   = json.indexOf('"', debut);
        return json.substring(debut, fin);
    }

    private void repondreOk(HttpExchange ex, String msg) throws IOException {
        byte[] b = ("{\"message\":\"" + msg + "\"}").getBytes(StandardCharsets.UTF_8);
        repondre(ex, 200, "application/json; charset=UTF-8", b);
    }

    private void repondre(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }
}