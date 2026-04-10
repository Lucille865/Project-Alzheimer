import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class HistoriqueManager {

    // Fichier de persistance dans le répertoire de l'appli
    private static final String FICHIER = "historique_lucien.json";

    // Clé = date "yyyy-MM-dd", valeur = liste d'événements du jour
    private final Map<String, List<EvenementTache>> historique = new LinkedHashMap<>();

    private static final DateTimeFormatter FMT_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public HistoriqueManager() {
        chargerDepuisFichier();
    }

    /** Appelée depuis InterfaceLucien quand Lucien appuie sur OUI. */
    public void enregistrerValidation(Tache t, boolean enRetard) {
        String today = LocalDate.now().format(FMT_DATE);
        historique.computeIfAbsent(today, k -> new ArrayList<>())
                .add(new EvenementTache(t.getNom(), enRetard));
        sauvegarderVersFichier();
    }

    /** Retourne le JSON complet des 7 derniers jours pour le dashboard. */
    public String toJsonDashboard(TacheManager tacheManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // ── Résumé aujourd'hui ────────────────────────────────────────────
        String today      = LocalDate.now().format(FMT_DATE);
        long   validees   = tacheManager.getNbValidees();
        int    total      = tacheManager.getNbTotal();
        int    pct        = total == 0 ? 0 : (int) (validees * 100 / total);

        sb.append("\"aujourd_hui\":{");
        sb.append("\"date\":\"").append(today).append("\",");
        sb.append("\"validees\":").append(validees).append(",");
        sb.append("\"total\":").append(total).append(",");
        sb.append("\"pourcentage\":").append(pct).append(",");
        sb.append("\"evenements\":[");

        List<EvenementTache> evtsToday =
                historique.getOrDefault(today, Collections.emptyList());
        sb.append(evtsToday.stream()
                .map(EvenementTache::toJson)
                .collect(Collectors.joining(",")));
        sb.append("]},");

        // ── Historique 7 jours ────────────────────────────────────────────
        sb.append("\"historique_7j\":[");

        List<String> derniers7Jours = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            derniers7Jours.add(LocalDate.now().minusDays(i).format(FMT_DATE));
        }

        StringJoiner joinerJours = new StringJoiner(",");
        for (String date : derniers7Jours) {
            List<EvenementTache> evts =
                    historique.getOrDefault(date, Collections.emptyList());
            int nbValideesJour = evts.size();
            int retards        = (int) evts.stream().filter(EvenementTache::isEnRetard).count();
            int pctJour        = total == 0 ? 0 : nbValideesJour * 100 / total;

            StringBuilder jour = new StringBuilder();
            jour.append("{");
            jour.append("\"date\":\"").append(date).append("\",");
            jour.append("\"validees\":").append(nbValideesJour).append(",");
            jour.append("\"retards\":").append(retards).append(",");
            jour.append("\"pourcentage\":").append(pctJour).append(",");
            jour.append("\"evenements\":[");
            jour.append(evts.stream()
                    .map(EvenementTache::toJson)
                    .collect(Collectors.joining(",")));
            jour.append("]}");
            joinerJours.add(jour);
        }

        sb.append(joinerJours);
        sb.append("],");

        // ── Liste des tâches du jour (pour affichage statut en direct) ────
        sb.append("\"taches_du_jour\":[");
        StringJoiner joinerTaches = new StringJoiner(",");
        for (Tache t : tacheManager.getTaches()) {
            joinerTaches.add(String.format(
                    "{\"nom\":\"%s\",\"plage\":\"%s\",\"validee\":%b}",
                    t.getNom(), t.getPlageHoraire(), t.isEstValidee()
            ));
        }
        sb.append(joinerTaches);
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    // ── Persistance fichier (JSON simple, sans librairie) ─────────────────────

    private void sauvegarderVersFichier() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FICHIER))) {
            sb_fichier(pw);
        } catch (IOException e) {
            System.err.println("[HistoriqueManager] Erreur sauvegarde : " + e.getMessage());
        }
    }

    private void sb_fichier(PrintWriter pw) {
        pw.print("{");
        StringJoiner joinerJours = new StringJoiner(",");
        for (Map.Entry<String, List<EvenementTache>> entry : historique.entrySet()) {
            StringBuilder ligne = new StringBuilder();
            ligne.append("\"").append(entry.getKey()).append("\":[");
            ligne.append(entry.getValue().stream()
                    .map(EvenementTache::toJson)
                    .collect(Collectors.joining(",")));
            ligne.append("]");
            joinerJours.add(ligne);
        }
        pw.print(joinerJours);
        pw.print("}");
    }

    private void chargerDepuisFichier() {
        File f = new File(FICHIER);
        if (!f.exists()) return;

        try {
            String contenu = Files.readString(f.toPath());
            // Parsing manuel léger — suffisant pour notre format connu
            // Format : {"2025-01-01":[{...},{...}],"2025-01-02":[...]}
            parseJson(contenu);
        } catch (IOException e) {
            System.err.println("[HistoriqueManager] Erreur lecture : " + e.getMessage());
        }
    }

    private void parseJson(String json) {
        // On extrait les blocs "date":[evenements] un par un
        int i = 1; // on saute le premier '{'
        while (i < json.length()) {
            // Cherche la prochaine clé date
            int debutCle = json.indexOf('"', i);
            if (debutCle == -1) break;
            int finCle = json.indexOf('"', debutCle + 1);
            String date = json.substring(debutCle + 1, finCle);

            // Cherche le tableau d'événements
            int debutTab = json.indexOf('[', finCle);
            int finTab   = json.indexOf(']', debutTab);
            String tableau = json.substring(debutTab + 1, finTab);

            List<EvenementTache> evts = parseEvenements(tableau, date);
            if (!evts.isEmpty()) historique.put(date, evts);

            i = finTab + 1;
        }
    }

    private List<EvenementTache> parseEvenements(String tableau, String date) {
        List<EvenementTache> result = new ArrayList<>();
        if (tableau.isBlank()) return result;

        // Chaque événement : {"tache":"...","horodatage":"...","enRetard":true/false}
        String[] blocs = tableau.split("\\},\\{");
        for (String bloc : blocs) {
            try {
                String nom       = extraireValeur(bloc, "tache");
                boolean enRetard = bloc.contains("\"enRetard\":true");
                result.add(new EvenementTache(nom, enRetard));
            } catch (Exception ignored) {}
        }
        return result;
    }

    private String extraireValeur(String json, String cle) {
        String cherche = "\"" + cle + "\":\"";
        int debut = json.indexOf(cherche) + cherche.length();
        int fin   = json.indexOf('"', debut);
        return json.substring(debut, fin);
    }
}