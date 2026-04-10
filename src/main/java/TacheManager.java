import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    /** Tâche active à l'heure donnée (réelle ou simulée). */
    public Optional<Tache> getTacheActive(LocalTime now) {
        return taches.stream()
                .filter(t -> t.estActive(now))
                .findFirst();
    }

    /** Resets automatiques à l'heure donnée. */
    public void mettreAJour(LocalTime now) {
        taches.forEach(t -> t.verifierReset(now));
    }

    public long getNbValidees()    { return taches.stream().filter(Tache::isEstValidee).count(); }
    public int  getNbTotal()       { return taches.size(); }
    public List<Tache> getTaches() { return taches; }
}