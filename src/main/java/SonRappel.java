import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

/**
 * Gestionnaire des sons pour l'application MémoGuide
 */
public class SonRappel {

    private static AudioClip sonActivation;
    private static AudioClip sonRappel;
    private static boolean sonsActives = true;

    // Initialisation statique des sons
    static {
        chargerSons();
    }

    /**
     * Charge les fichiers sons depuis le dossier resources/sounds/
     */
    private static void chargerSons() {
        try {
            // Son d'activation (quand une tâche devient active)
            URL urlActivation = SonRappel.class.getResource("/sounds/Audio-petite-fille-1.wav");
            if (urlActivation != null) {
                sonActivation = new AudioClip(urlActivation.toString());
                System.out.println("✅ Son d'activation chargé");
            } else {
                System.err.println("❌ Fichier activation.wav introuvable");
            }

            // Son de rappel (30 minutes)
            URL urlRappel = SonRappel.class.getResource("/sounds/Audio-petite-fille-2.wav");
            if (urlRappel != null) {
                sonRappel = new AudioClip(urlRappel.toString());
                System.out.println("✅ Son de rappel chargé");
            } else {
                System.err.println("❌ Fichier rappel.wav introuvable");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement des sons: " + e.getMessage());
        }
    }

    /**
     * Joue le son d'activation (quand une tâche devient active)
     */
    public static void jouerSonActivation() {
        if (!sonsActives) return;

        if (sonActivation != null) {
            sonActivation.play();
            System.out.println("🔊 Son d'activation joué");
        } else {
            System.out.println("[SON] Activation (fichier manquant)");
        }
    }

    /**
     * Joue le son de rappel (30 minutes après le début)
     */
    public static void jouerSonRappel() {
        if (!sonsActives) return;

        if (sonRappel != null) {
            sonRappel.play();
            System.out.println("🔊 Son de rappel joué");
        } else {
            System.out.println("[🔔 RAPPEL] 30 minutes écoulées !");
        }
    }

    /**
     * Joue le son de validation (quand on appuie sur OUI)
     * Optionnel : vous pouvez ajouter un troisième son
     */
    public static void jouerSonValidation() {
        // Vous pouvez ajouter un son validation.wav si vous voulez
        System.out.println("✓ Validation effectuée");
    }

    /**
     * Active ou désactive tous les sons
     */
    public static void setSonsActives(boolean actives) {
        sonsActives = actives;
        System.out.println("Sons " + (actives ? "activés" : "désactivés"));
    }

    /**
     * Vérifie si les sons sont chargés correctement
     */
    public static boolean sontSonsCharges() {
        return sonActivation != null && sonRappel != null;
    }
}