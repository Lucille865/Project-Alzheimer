import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class InterfaceLucien extends Application {

    private HistoriqueManager historiqueManager;
    private DashboardServeur  dashboardServeur;

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final String C_FOND    = "#0d1117";
    private static final String C_CARTE   = "#161b22";
    private static final String C_BORDURE = "#30363d";
    private static final String C_TEXTE   = "#e6edf3";
    private static final String C_SUBTIL  = "#8b949e";
    private static final String C_VERT    = "#3fb950";
    private static final String C_ROUGE   = "#f85149";
    private static final String C_ORANGE  = "#d29922";
    private static final String C_GRIS    = "#484f58";
    private static final String C_BLEU    = "#388bfd";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── État ─────────────────────────────────────────────────────────────────
    private TacheManager tacheManager;
    private Tache        tacheEnCours = null;
    private long         offsetMinutes = 0; // décalage de simulation en minutes

    // ── Composants UI ─────────────────────────────────────────────────────────
    private Label       labelHeure;
    private Label       labelTache;
    private Label       labelPlage;
    private Label       labelStatut;
    private Label       labelProgres;
    private Label       labelOffsetInfo;
    private ProgressBar progressBar;
    private Button      btnOui;

    @Override
    public void start(Stage stage) {
        tacheManager = new TacheManager();
        historiqueManager = new HistoriqueManager();
        dashboardServeur  = new DashboardServeur(tacheManager, historiqueManager);
        dashboardServeur.demarrer();

        // ── Header ────────────────────────────────────────────────────────────
        labelHeure = creerLabel("00:00:00", 60, true, C_TEXTE);

        Label salutation = creerLabel("Bonjour Lucien 👋", 24, true, C_TEXTE);

        // Phrase de rappel pour Lucien (alzheimer)
        Label rappelAppli = creerLabel(
                "Cette application t'aide à te souvenir\n" +
                        "de tes activités importantes de la journée.\n" +
                        "Appuie sur OUI quand tu as fait la tâche affichée.",
                16, false, C_SUBTIL
        );
        rappelAppli.setWrapText(true);
        rappelAppli.setTextAlignment(TextAlignment.CENTER);
        rappelAppli.setMaxWidth(380);

        VBox header = new VBox(8, labelHeure, salutation, rappelAppli);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(36, 24, 16, 24));

        Separator sep1 = new Separator();
        sep1.setMaxWidth(380);
        sep1.setStyle("-fx-background-color: " + C_BORDURE + ";");

        // ── Carte tâche principale ────────────────────────────────────────────
        labelTache = creerLabel("Chargement…", 36, true, C_TEXTE);
        labelTache.setWrapText(true);
        labelTache.setMaxWidth(380);
        labelTache.setTextAlignment(TextAlignment.CENTER);
        labelTache.setAlignment(Pos.CENTER);

        labelPlage = creerLabel("", 18, false, C_SUBTIL);

        labelStatut = creerLabel("", 20, false, C_SUBTIL);

        VBox carteTache = new VBox(10, labelTache, labelPlage, labelStatut);
        carteTache.setAlignment(Pos.CENTER);
        carteTache.setMinHeight(165);
        carteTache.setMaxWidth(420);
        carteTache.setStyle(
                "-fx-background-color: " + C_CARTE + ";" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: " + C_BORDURE + ";" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 28;"
        );

        // ── Bouton OUI ────────────────────────────────────────────────────────
        btnOui = new Button("OUI  ✓");
        btnOui.setPrefSize(340, 140);
        appliquerStyleBouton(btnOui, C_VERT);

        btnOui.setOnAction(e -> actionValidation());

        // ── Progression ───────────────────────────────────────────────────────
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(380);
        progressBar.setPrefHeight(10);
        progressBar.setStyle("-fx-accent: " + C_VERT + ";");

        labelProgres = creerLabel("0 / 8 tâches effectuées", 15, false, C_SUBTIL);

        VBox progressBox = new VBox(6, progressBar, labelProgres);
        progressBox.setAlignment(Pos.CENTER);

        Separator sep2 = new Separator();
        sep2.setMaxWidth(380);
        sep2.setStyle("-fx-background-color: " + C_BORDURE + ";");

        // ── Panneau de simulation ─────────────────────────────────────────────
        Label titreSim = creerLabel("🧪 Simulation", 14, true, C_SUBTIL);

        labelOffsetInfo = creerLabel("Heure simulée : aucun décalage", 14, false, C_BLEU);

        Button btn15  = creerBoutonSim("+15 min", () -> offsetMinutes += 15);
        Button btn1h  = creerBoutonSim("+1 heure", () -> offsetMinutes += 60);
        Button btn3h  = creerBoutonSim("+3 heures", () -> offsetMinutes += 180);
        Button btnReset = creerBoutonSim("↺ Reset", () -> {
            offsetMinutes = 0;
            // Réinitialise toutes les tâches pour repartir proprement
            tacheManager.getTaches().forEach(t -> {
                if (t.isEstValidee()) {
                    // Force un reset en appelant verifierReset avec une heure très tardive
                    t.verifierReset(LocalTime.of(23, 59));
                }
            });
        });

        HBox boutonsSim = new HBox(10, btn15, btn1h, btn3h, btnReset);
        boutonsSim.setAlignment(Pos.CENTER);

        VBox panneauSim = new VBox(8, titreSim, labelOffsetInfo, boutonsSim);
        panneauSim.setAlignment(Pos.CENTER);
        panneauSim.setPadding(new Insets(12, 16, 20, 16));
        panneauSim.setStyle(
                "-fx-background-color: #0d1f2d;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: " + C_BLEU + "44;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 1;"
        );
        panneauSim.setMaxWidth(420);

        // ── Racine ────────────────────────────────────────────────────────────
        VBox root = new VBox(20,
                header,
                sep1,
                carteTache,
                btnOui,
                progressBox,
                sep2,
                panneauSim
        );
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(0, 20, 0, 20));
        root.setStyle("-fx-background-color: " + C_FOND + ";");

        Scene scene = new Scene(root, 460, 900);
        stage.setTitle("MémoGuide – Lucien");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();

        demarrerHorloge();
    }

    // ── Horloge ───────────────────────────────────────────────────────────────

    private void demarrerHorloge() {
        Timeline horloge = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            // Heure simulée = heure réelle + décalage choisi
            LocalTime now = LocalTime.now().plusMinutes(offsetMinutes);

            // Heure affichée
            labelHeure.setText(now.format(FMT));

            // Info décalage
            if (offsetMinutes == 0) {
                labelOffsetInfo.setText("Heure simulée : aucun décalage");
            } else {
                long h = offsetMinutes / 60;
                long m = offsetMinutes % 60;
                String decalage = (h > 0 ? h + "h " : "") + (m > 0 ? m + "min" : "");
                labelOffsetInfo.setText("Décalage actif : +" + decalage
                        + "  →  heure simulée : " + now.format(DateTimeFormatter.ofPattern("HH:mm")));
            }

            // Backend
            tacheManager.mettreAJour(now);

            // Tâche active
            var active = tacheManager.getTacheActive(now);

            if (active.isPresent()) {
                Tache t = active.get();
                tacheEnCours = t;

                labelTache.setText(t.getNom());
                labelPlage.setText("🕐 " + t.getPlageHoraire());

                if (t.isEstValidee()) {
                    afficherEtatValide();
                } else {
                    afficherEtatAFaire(t, now);
                    if (t.doitDeclenchemerRappel(now)) {
                        java.awt.Toolkit.getDefaultToolkit().beep();
                        System.out.println("[🔔 RAPPEL] " + t.getNom() + " non validée depuis 30 min !");
                    }
                }
            } else {
                tacheEnCours = null;
                labelTache.setText("Pas de tâche\npour l'instant 😊");
                labelPlage.setText("");
                labelStatut.setText("Profite de ton temps libre !");
                labelStatut.setStyle(styleLabelStatut(C_SUBTIL));
                appliquerStyleBouton(btnOui, C_GRIS);
                btnOui.setDisable(true);
            }

            // Progression
            long validees = tacheManager.getNbValidees();
            int  total    = tacheManager.getNbTotal();
            progressBar.setProgress((double) validees / total);
            labelProgres.setText(validees + " / " + total + " tâches effectuées");
        }));

        horloge.setCycleCount(Timeline.INDEFINITE);
        horloge.play();
    }

    // ── États visuels ─────────────────────────────────────────────────────────

    private void afficherEtatValide() {
        labelStatut.setText("✅ Bravo Lucien, c'est fait !");
        labelStatut.setStyle(styleLabelStatut(C_VERT));
        appliquerStyleBouton(btnOui, C_GRIS);
        btnOui.setDisable(true);
    }

    private void afficherEtatAFaire(Tache t, LocalTime now) {
        long min = java.time.temporal.ChronoUnit.MINUTES.between(now, t.getHeureReset());
        String urgence;
        String couleur;

        if (min <= 15) {
            urgence = "⚠️ Urgent ! Encore " + min + " min";
            couleur = C_ROUGE;
        } else if (min <= 45) {
            urgence = "🕐 Plus que " + min + " min";
            couleur = C_ORANGE;
        } else {
            urgence = "À faire avant " + t.getHeureReset();
            couleur = C_SUBTIL;
        }

        labelStatut.setText(urgence);
        labelStatut.setStyle(styleLabelStatut(couleur));
        appliquerStyleBouton(btnOui, min <= 15 ? C_ROUGE : min <= 45 ? C_ORANGE : C_VERT);
        btnOui.setDisable(false);
    }

    // ── Action bouton OUI ─────────────────────────────────────────────────────

    private void actionValidation() {
        if (tacheEnCours == null || tacheEnCours.isEstValidee()) return;
        tacheEnCours.valider();
        boolean enRetard = tacheEnCours.doitDeclenchemerRappel(LocalTime.now().plusMinutes(offsetMinutes));
        historiqueManager.enregistrerValidation(tacheEnCours, enRetard);

        ScaleTransition pulse = new ScaleTransition(Duration.millis(140), btnOui);
        pulse.setFromX(1.0); pulse.setFromY(1.0);
        pulse.setToX(1.08);  pulse.setToY(1.08);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.setOnFinished(e -> afficherEtatValide());
        pulse.play();
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

    private Button creerBoutonSim(String texte, Runnable action) {
        Button btn = new Button(texte);
        btn.setStyle(
                "-fx-background-color: #21262d;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 13px;" +
                        "-fx-text-fill: " + C_BLEU + ";" +
                        "-fx-border-color: " + C_BLEU + "66;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 6 12 6 12;"
        );
        btn.setOnAction(e -> action.run());
        return btn;
    }

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
                        "-fx-background-radius: 18;" +
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

    @Override
    public void stop() {
        if (dashboardServeur != null) dashboardServeur.arreter();
    }
}