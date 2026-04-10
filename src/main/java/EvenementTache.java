import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EvenementTache {

    private final String          nomTache;
    private final LocalDateTime   horodatage;
    private final boolean         enRetard; // validée après heureDebut + 30 min ?

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EvenementTache(String nomTache, boolean enRetard) {
        this.nomTache   = nomTache;
        this.horodatage = LocalDateTime.now();
        this.enRetard   = enRetard;
    }

    // Sérialisation manuelle en JSON (pas besoin de librairie externe)
    public String toJson() {
        return String.format(
                "{\"tache\":\"%s\",\"horodatage\":\"%s\",\"enRetard\":%b}",
                nomTache,
                horodatage.format(FMT),
                enRetard
        );
    }

    public String getNomTache()       { return nomTache; }
    public LocalDateTime getHorodatage() { return horodatage; }
    public boolean isEnRetard()       { return enRetard; }
}