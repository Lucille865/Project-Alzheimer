import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gère la liste des tâches quotidiennes de Lucien.
 * Point central pour : accès, validation, resets, progression.
 */
public class TacheManager {

    private final List<Tache> taches = new ArrayList<>();

    public TacheManager() {
        taches.add(new Tache("Prendre le petit-déjeuner",    "07:00", "10:30"));
        taches.add(new Tache("Se laver les dents",           "09:00", "10:00"));
        taches.add(new Tache("Faire de l'exercice physique", "10:30", "12:00"));
        taches.add(new Tache("Déjeuner",                     "12:00", "14:30"));
        taches.add(new Tache("Faire une sieste / Repos",     "14:00", "15:30"));
        taches.add(new Tache("Boire un verre d'eau",         "16:00", "17:00"));
        taches.add(new Tache("Prendre sa douche",            "18:00", "20:00"));
        taches.add(new Tache("Dîner",                        "19:30", "21:30"));
    }

    /** Retourne la première tâche dont le créneau est actif, s'il y en a une. */
    public Optional<Tache> getTacheActive() {
        LocalTime now = LocalTime.now();
        return taches.stream()
                .filter(t -> t.estActive(now))
                .findFirst();
    }

    /** À appeler chaque seconde : vérifie et applique tous les resets. */
    public void mettreAJour() {
        LocalTime now = LocalTime.now();
        taches.forEach(t -> t.verifierReset(now));
    }

    /** Nombre de tâches validées ce jour. */
    public long getNbValidees() {
        return taches.stream().filter(Tache::isEstValidee).count();
    }

    public int getNbTotal()         { return taches.size(); }
    public List<Tache> getTaches()  { return taches; }
}
