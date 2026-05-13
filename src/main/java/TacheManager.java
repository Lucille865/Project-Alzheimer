import java.io.*;
import java.nio.file.*;
import java.time.LocalTime;
import java.util.*;

public class TacheManager {

    private static final String FICHIER_TACHES = "taches.json";
    private final List<Tache> taches = new ArrayList<>();

    public TacheManager() {
        chargerTaches();
    }

    // ── Accès ─────────────────────────────────────────────────────────────────

    public Optional<Tache> getTacheActive(LocalTime now) {
        return taches.stream().filter(t -> t.estActive(now)).findFirst();
    }

    public void mettreAJour(LocalTime now) {
        taches.forEach(t -> t.verifierReset(now));
    }

    public long getNbValidees()    { return taches.stream().filter(Tache::isEstValidee).count(); }
    public int  getNbTotal()       { return taches.size(); }
    public List<Tache> getTaches() { return Collections.unmodifiableList(taches); }

    // ── CRUD tâches ───────────────────────────────────────────────────────────

    public void ajouterTache(String nom, String heureDebut, String heureReset) {
        taches.add(new Tache(nom, heureDebut, heureReset));
        taches.sort(Comparator.comparing(Tache::getHeureDebut));
        sauvegarderTaches();
    }

    public boolean modifierTache(int index, String nom, String heureDebut, String heureReset) {
        if (index < 0 || index >= taches.size()) return false;
        taches.set(index, new Tache(nom, heureDebut, heureReset));
        taches.sort(Comparator.comparing(Tache::getHeureDebut));
        sauvegarderTaches();
        return true;
    }

    public boolean supprimerTache(int index) {
        if (index < 0 || index >= taches.size()) return false;
        taches.remove(index);
        sauvegarderTaches();
        return true;
    }

    // ── Persistance ───────────────────────────────────────────────────────────

    public void sauvegarderTaches() {
        StringBuilder sb = new StringBuilder("[");
        StringJoiner joiner = new StringJoiner(",");
        for (Tache t : taches) {
            joiner.add(String.format(
                    "{\"nom\":\"%s\",\"debut\":\"%s\",\"reset\":\"%s\"}",
                    t.getNom(),
                    t.getHeureDebut().toString(),
                    t.getHeureReset().toString()
            ));
        }
        sb.append(joiner).append("]");

        try (PrintWriter pw = new PrintWriter(new FileWriter(FICHIER_TACHES))) {
            pw.print(sb);
        } catch (IOException e) {
            System.err.println("[TacheManager] Erreur sauvegarde : " + e.getMessage());
        }
    }

    private void chargerTaches() {
        File f = new File(FICHIER_TACHES);
        if (!f.exists()) {
            chargerTachesParDefaut();
            sauvegarderTaches();
            return;
        }
        try {
            String json = Files.readString(f.toPath());
            parseTaches(json);
            if (taches.isEmpty()) chargerTachesParDefaut();
        } catch (IOException e) {
            System.err.println("[TacheManager] Erreur lecture, tâches par défaut chargées.");
            chargerTachesParDefaut();
        }
    }

    private void chargerTachesParDefaut() {
        taches.clear();
        taches.add(new Tache("Prendre le petit-déjeuner",    "07:00", "10:30"));
        taches.add(new Tache("Se laver les dents",           "09:00", "10:00"));
        taches.add(new Tache("Faire de l'exercice physique", "10:30", "12:00"));
        taches.add(new Tache("Déjeuner",                     "12:00", "14:30"));
        taches.add(new Tache("Faire une sieste / Repos",     "14:00", "15:30"));
        taches.add(new Tache("Boire un verre d'eau",         "16:00", "17:00"));
        taches.add(new Tache("Prendre sa douche",            "18:00", "20:00"));
        taches.add(new Tache("Dîner",                        "19:30", "21:30"));
    }

    private void parseTaches(String json) {
        // Format connu : [{"nom":"...","debut":"HH:mm","reset":"HH:mm"}, ...]
        String inner = json.trim().replaceAll("^\\[|\\]$", "");
        if (inner.isBlank()) return;
        String[] blocs = inner.split("\\},\\{");
        for (String bloc : blocs) {
            try {
                String nom   = extraire(bloc, "nom");
                String debut = extraire(bloc, "debut");
                String reset = extraire(bloc, "reset");
                taches.add(new Tache(nom, debut, reset));
            } catch (Exception e) {
                System.err.println("[TacheManager] Bloc ignoré : " + bloc);
            }
        }
    }

    private String extraire(String json, String cle) {
        String cherche = "\"" + cle + "\":\"";
        int debut = json.indexOf(cherche) + cherche.length();
        int fin   = json.indexOf('"', debut);
        return json.substring(debut, fin);
    }
}