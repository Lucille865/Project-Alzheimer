import java.io.*;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.*;

public class TacheManager {
    private List<Tache> taches;
    private List<TacheChangeListener> listeners = new ArrayList<>();
    private static final String FICHIER_TACHES = "taches.json";

    // Interface pour notifier les changements (pour l'interface JavaFX)
    public interface TacheChangeListener {
        void onTachesChanged();
    }

    public void addListener(TacheChangeListener listener) {
        listeners.add(listener);
    }

    private void notifierChangement() {
        for (TacheChangeListener listener : listeners) {
            listener.onTachesChanged();
        }
    }

    public TacheManager() {
        taches = new ArrayList<>();
        chargerTaches();  // Charge depuis le fichier JSON
    }

    // ── CRUD tâches (MODIFIÉ pour utiliser votre persistance) ─────────────────

    public boolean ajouterTache(String nom, String heureDebut, String heureReset) {
        // Vérifier les doublons
        for (Tache t : taches) {
            if (t.getNom().equalsIgnoreCase(nom)) {
                return false;
            }
        }

        Tache nouvelleTache = new Tache(nom, heureDebut, heureReset);
        taches.add(nouvelleTache);
        taches.sort(Comparator.comparing(Tache::getHeureDebut));
        sauvegarderTaches();  // Sauvegarde dans le fichier JSON
        notifierChangement();
        return true;
    }

    public boolean modifierTache(int index, String nom, String heureDebut, String heureReset) {
        if (index < 0 || index >= taches.size()) return false;

        // Conserver l'état de validation si le nom n'a pas changé
        boolean etaitValidee = taches.get(index).isEstValidee();
        Tache nouvelleTache = new Tache(nom, heureDebut, heureReset);
        if (etaitValidee && taches.get(index).getNom().equals(nom)) {
            nouvelleTache.valider();
        }

        taches.set(index, nouvelleTache);
        taches.sort(Comparator.comparing(Tache::getHeureDebut));
        sauvegarderTaches();
        notifierChangement();
        return true;
    }

    public boolean modifierTacheParNom(String ancienNom, String nouveauNom, String heureDebut, String heureReset) {
        for (int i = 0; i < taches.size(); i++) {
            if (taches.get(i).getNom().equalsIgnoreCase(ancienNom)) {
                return modifierTache(i, nouveauNom, heureDebut, heureReset);
            }
        }
        return false;
    }

    public boolean supprimerTache(int index) {
        if (index < 0 || index >= taches.size()) return false;
        taches.remove(index);
        sauvegarderTaches();
        notifierChangement();
        return true;
    }

    public boolean supprimerTacheParNom(String nom) {
        for (int i = 0; i < taches.size(); i++) {
            if (taches.get(i).getNom().equalsIgnoreCase(nom)) {
                return supprimerTache(i);
            }
        }
        return false;
    }

    public List<Tache> getTaches() {
        return new ArrayList<>(taches); // Retourne une copie immuable
    }

    public int getNbTotal() {
        return taches.size();
    }

    public long getNbValidees() {
        return taches.stream().filter(Tache::isEstValidee).count();
    }

    public void mettreAJour(LocalTime now) {
        for (Tache t : taches) {
            t.verifierReset(now);
        }
    }

    public Optional<Tache> getTacheActive(LocalTime now) {
        return taches.stream()
                .filter(t -> !t.isEstValidee())
                .filter(t -> !now.isBefore(t.getHeureDebut()))
                .filter(t -> now.isBefore(t.getHeureReset()))
                .findFirst();
    }

    // ── Persistance (VOTRE CODE, inchangé) ───────────────────────────────────

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
            System.out.println("💾 Tâches sauvegardées dans " + FICHIER_TACHES);
        } catch (IOException e) {
            System.err.println("[TacheManager] Erreur sauvegarde : " + e.getMessage());
        }
    }

    private void chargerTaches() {
        File f = new File(FICHIER_TACHES);
        if (!f.exists()) {
            System.out.println("📁 Fichier " + FICHIER_TACHES + " introuvable, chargement des tâches par défaut");
            chargerTachesParDefaut();
            sauvegarderTaches();
            return;
        }
        try {
            String json = Files.readString(f.toPath());
            parseTaches(json);
            if (taches.isEmpty()) {
                System.out.println("📁 Fichier vide, chargement des tâches par défaut");
                chargerTachesParDefaut();
                sauvegarderTaches();
            } else {
                System.out.println("📋 " + taches.size() + " tâches chargées depuis " + FICHIER_TACHES);
            }
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
        System.out.println("📋 Tâches par défaut chargées");
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