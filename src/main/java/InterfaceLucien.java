import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class InterfaceLucien extends Application {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final String C_FOND      = "#0d1117";
    private static final String C_CARTE     = "#161b22";
    private static final String C_BORDURE   = "#30363d";
    private static final String C_TEXTE     = "#e6edf3";
    private static final String C_SUBTIL    = "#8b949e";
    private static final String C_VERT      = "#3fb950";
    private static final String C_ROUGE     = "#f85149";
    private static final String C_ORANGE    = "#d29922";
    private static final String C_GRIS      = "#484f58";

    // ── État ─────────────────────────────────────────────────────────────────
    private TacheManager tacheManager;
    private Tache tacheEnCours = null;

    // ── Composants UI ────────────────────────────────────────────────────────
    private Label labelHeure;
    private Label labelTache;
    private Label labelPlage;
    private Label labelStatut;
    private Label labelProgres;
    private ProgressBar progressBar;
    private Button btnOui;

    // ── Formatter ────────────────────────────────────────────────────────────
    private static final DateTimeFormatter FMT_HEURE =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void start(Stage stage) {
        tacheManager = new TacheManager();

        // ── Header ───────────────────────────────────────────────────────────
        labelHeure = creerLabel("00:00:00", 56, true, C_TEXTE);

        Label salutation = creerLabel("Bonjour Lucien 👋", 22, false, C_SUBTIL);

        VBox header = new VBox(4, labelHeure, salutation);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(36, 20, 16, 20));

        // ── Carte tâche ──────────────────────────────────────────────────────
        labelTache = creerLabel("Chargement…", 34, true, C_TEXTE);
        labelTache.setWrapText(true);
        labelTache.setMaxWidth(390);
        labelTache.setAlignment(Pos.CENTER);
        labelTache.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        labelPlage = creerLabel("", 17, false, C_SUBTIL);

        labelStatut = creerLabel("", 19, false, C_SUBTIL);

        VBox carteTache = new VBox(12, labelTache, labelPlage, labelStatut);
        carteTache.setAlignment(Pos.CENTER);
        carteTache.setMinHeight(175);
        carteTache.setMaxWidth(420);
        carteTache.setStyle(
                "-fx-background-color: " + C_CARTE + ";" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: " + C_BORDURE + ";" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 28;"
        );

        // ── Bouton OUI ───────────────────────────────────────────────────────
        btnOui = new Button("OUI  ✓");
        btnOui.setPrefSize(340, 150);
        appliquerStyleBouton(btnOui, C_VERT);
        btnOui.setOnAction(e -> actionValidation());

        // ── Pied de page : progression ───────────────────────────────────────
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(370);
        progressBar.setPrefHeight(12);
        progressBar.setStyle("-fx-accent: " + C_VERT + ";");

        labelProgres = creerLabel("0 / 8 tâches effectuées", 15, false, C_SUBTIL);

        VBox footer = new VBox(8, progressBar, labelProgres);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(8, 20, 36, 20));

        // ── Séparateur ───────────────────────────────────────────────────────
        Separator sep = new Separator();
        sep.setMaxWidth(370);
        sep.setStyle("-fx-background-color: " + C_BORDURE + ";");

        // ── Racine ───────────────────────────────────────────────────────────
        VBox root = new VBox(24, header, carteTache, btnOui, sep, footer);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + C_FOND + ";");

        Scene scene = new Scene(root, 460, 760);
        stage.setTitle("MémoGuide – Lucien");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();

        demarrerHorloge();
    }

    // ── Logique horloge ──────────────────────────────────────────────────────

    private void demarrerHorloge() {
        Timeline horloge = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalTime now = LocalTime.now();

            // 1. Heure en direct
            labelHeure.setText(now.format(FMT_HEURE));

            // 2. Resets automatiques
            tacheManager.mettreAJour();

            // 3. Tâche active
            Optional<Tache> active = tacheManager.getTacheActive();

            if (active.isPresent()) {
                Tache t = active.get();
                tacheEnCours = t;

                labelTache.setText(t.getNom());
                labelPlage.setText("🕐 " + t.getPlageHoraire());

                if (t.isEstValidee()) {
                    afficherEtatValide();
                } else {
                    afficherEtatAFaire(t, now);
                    // FS5 : rappel sonore si 30 min dépassées
                    if (t.doitDeclenchemerRappel(now)) {
                        declencherRappelSonore(t);
                    }
                }
            } else {
                tacheEnCours = null;
                labelTache.setText("Pas de tâche pour l'instant 😊");
                labelPlage.setText("");
                labelStatut.setText("Profite de ton temps libre !");
                labelStatut.setStyle(styleLabelStatut(C_SUBTIL));
                appliquerStyleBouton(btnOui, C_GRIS);
                btnOui.setDisable(true);
            }

            // 4. Progression
            long validees = tacheManager.getNbValidees();
            int  total    = tacheManager.getNbTotal();
            progressBar.setProgress((double) validees / total);
            labelProgres.setText(validees + " / " + total + " tâches effectuées");
        }));

        horloge.setCycleCount(Timeline.INDEFINITE);
        horloge.play();
    }

    private void afficherEtatValide() {
        labelStatut.setText("✅ Bravo Lucien, c'est fait !");
        labelStatut.setStyle(styleLabelStatut(C_VERT));
        appliquerStyleBouton(btnOui, C_GRIS);
        btnOui.setDisable(true);
    }

    private void afficherEtatAFaire(Tache t, LocalTime now) {
        // Calcul du temps restant
        long minutesRestantes = java.time.temporal.ChronoUnit.MINUTES.between(now, t.getHeureReset());
        String urgence;
        String couleurStatut;

        if (minutesRestantes <= 15) {
            urgence       = "⚠️ Urgent ! Encore " + minutesRestantes + " min";
            couleurStatut = C_ROUGE;
            appliquerStyleBouton(btnOui, C_ROUGE);
        } else if (minutesRestantes <= 45) {
            urgence       = "🕐 Plus que " + minutesRestantes + " min";
            couleurStatut = C_ORANGE;
            appliquerStyleBouton(btnOui, C_ORANGE);
        } else {
            urgence       = "À faire avant " + t.getHeureReset();
            couleurStatut = C_SUBTIL;
            appliquerStyleBouton(btnOui, C_VERT);
        }

        labelStatut.setText(urgence);
        labelStatut.setStyle(styleLabelStatut(couleurStatut));
        btnOui.setDisable(false);
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private void actionValidation() {
        if (tacheEnCours == null || tacheEnCours.isEstValidee()) return;

        tacheEnCours.valider();

        // Animation "pulse" sur le bouton
        ScaleTransition pulse = new ScaleTransition(Duration.millis(150), btnOui);
        pulse.setFromX(1.0); pulse.setFromY(1.0);
        pulse.setToX(1.08);  pulse.setToY(1.08);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.setOnFinished(e -> afficherEtatValide());
        pulse.play();
    }

    /** FS5 : alerte sonore (bip système). Remplacer par MediaPlayer si besoin. */
    private void declencherRappelSonore(Tache t) {
        System.out.println("[🔔 RAPPEL] " + t.getNom() + " non validée depuis 30 min !");
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    // ── Helpers UI ───────────────────────────────────────────────────────────

    private Label creerLabel(String texte, int taille, boolean bold, String couleur) {
        Label l = new Label(texte);
        l.setStyle(
                "-fx-font-size: " + taille + "px;" +
                        (bold ? "-fx-font-weight: bold;" : "") +
                        "-fx-text-fill: " + couleur + ";"
        );
        return l;
    }

    private void appliquerStyleBouton(Button btn, String couleur) {
        btn.setStyle(
                "-fx-background-color: " + couleur + ";" +
                        "-fx-background-radius: 16;" +
                        "-fx-font-size: 44px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;"
        );
    }

    private String styleLabelStatut(String couleur) {
        return "-fx-font-size: 19px; -fx-text-fill: " + couleur + ";";
    }

    public static void main(String[] args) { launch(args); }
}