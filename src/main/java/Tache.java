import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Tache {

    private final String nom;
    private final LocalTime heureDebut;
    private final LocalTime heureReset;
    private boolean estValidee;
    private boolean rappelDeclenche; // FS5 : évite de spammer le rappel

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    public Tache(String nom, String heureDebut, String heureReset) {
        this.nom       = nom;
        this.heureDebut = LocalTime.parse(heureDebut);
        this.heureReset = LocalTime.parse(heureReset);
        this.estValidee = false;
        this.rappelDeclenche = false;
    }

    /** Valide manuellement la tâche (appui bouton OUI). */
    public void valider() {
        this.estValidee = true;
        System.out.println("[✓] Tâche validée : " + nom);
    }

    /**
     * Appelée chaque seconde par l'horloge.
     * Remet la tâche à "à faire" une fois passé heureReset.
     */
    public void verifierReset(LocalTime maintenant) {
        if (estValidee && maintenant.isAfter(heureReset)) {
            this.estValidee = false;
            this.rappelDeclenche = false;
            System.out.println("[↺] Reset automatique : " + nom);
        }
    }

    /** True si l'heure actuelle est dans le créneau [heureDebut, heureReset]. */
    public boolean estActive(LocalTime maintenant) {
        return !maintenant.isBefore(heureDebut) && maintenant.isBefore(heureReset);
    }

    /**
     * True si la tâche est active, non validée, et que 30 min se sont écoulées.
     * Marque rappelDeclenche pour ne déclencher l'alerte qu'une seule fois.
     */
    public boolean doitDeclenchemerRappel(LocalTime maintenant) {
        if (!estValidee
                && estActive(maintenant)
                && maintenant.isAfter(heureDebut.plusMinutes(30))
                && !rappelDeclenche) {
            rappelDeclenche = true;
            return true;
        }
        return false;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getNom()            { return nom; }
    public LocalTime getHeureDebut()  { return heureDebut; }
    public LocalTime getHeureReset()  { return heureReset; }
    public boolean isEstValidee()     { return estValidee; }

    public String getPlageHoraire() {
        return heureDebut.format(FMT) + " – " + heureReset.format(FMT);
    }
}