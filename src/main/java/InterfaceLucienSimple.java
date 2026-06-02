import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javafx.scene.media.AudioClip;
import java.awt.Desktop;
import java.net.URI;

/**
 * Interface simplifiée pour la tablette de Lucien.
 * – Tâche courante en très grand
 * – Un seul bouton OUI (vert = à faire, gris = déjà fait)
 * – Touche Entrée = clic sur le bouton
 * – Rappel sonore automatique 30 min après le début si non validée
 */
public class InterfaceLucienSimple extends Application {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final String C_FOND   = "#0d1117";
    private static final String C_TEXTE  = "#e6edf3";
    private static final String C_SUBTIL = "#8b949e";
    private static final String C_VERT   = "#3fb950";
    private static final String C_ROUGE  = "#f85149";
    private static final String C_ORANGE = "#d29922";
    private static final String C_GRIS   = "#30363d";
    private static final String C_BLEU   = "#388bfd";  // Ajout pour le bouton aide

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── État ─────────────────────────────────────────────────────────────────
    private TacheManager    tacheManager;
    private HistoriqueManager historiqueManager;
    private DashboardServeur dashboardServeur;
    private Tache           tacheEnCours = null;

    // ── Composants ───────────────────────────────────────────────────────────
    private Label  labelHeure;
    private Label  labelNomTache;
    private Label  labelSousInfo;
    private Button btnOui;
    private Button btnAide;  // NOUVEAU : bouton d'aide
    private Region bandeauCouleur;

    @Override
    public void start(Stage stage) {
        tacheManager      = new TacheManager();
        historiqueManager = new HistoriqueManager();

        // Démarre le serveur dashboard
        dashboardServeur = new DashboardServeur(tacheManager, historiqueManager);
        dashboardServeur.demarrer();

        // ── Bandeau couleur en haut (indicateur d'urgence) ────────────────
        bandeauCouleur = new Region();
        bandeauCouleur.setPrefHeight(10);
        bandeauCouleur.setMaxWidth(Double.MAX_VALUE);
        bandeauCouleur.setStyle("-fx-background-color: " + C_GRIS + ";");

        // ── Heure (agrandie) ───────────────────────────────────────────────
        labelHeure = new Label("00:00");
        labelHeure.setStyle(
                "-fx-font-size: 96px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + C_TEXTE + ";" +
                        "-fx-font-family: 'Arial';"
        );
        labelHeure.setAlignment(Pos.CENTER);

        // ── Nom de la tâche ───────────────────────────────────────────────
        labelNomTache = creerLabel("Chargement…", 64, true, C_TEXTE);
        labelNomTache.setWrapText(true);
        labelNomTache.setMaxWidth(600);
        labelNomTache.setTextAlignment(TextAlignment.CENTER);
        labelNomTache.setAlignment(Pos.CENTER);

        // ── Sous-info (plage horaire ou statut) ───────────────────────────
        labelSousInfo = creerLabel("", 26, false, C_SUBTIL);

        // ── Bouton OUI ────────────────────────────────────────────────────
        btnOui = new Button("OUI  ✓");
        btnOui.setPrefSize(420, 200);
        stylesBouton(C_VERT);
        btnOui.setOnAction(e -> actionValidation());

        // ── NOUVEAU : Bouton Aide ─────────────────────────────────────────
        btnAide = new Button("❓ Aide");
        btnAide.setPrefSize(100, 50);
        btnAide.setStyle(
                "-fx-background-color: " + C_BLEU + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;"
        );
        btnAide.setOnAction(e -> afficherAide());

        // ── Barre du haut avec heure et bouton aide ───────────────────────
        HBox headerBar = new HBox();
        headerBar.setAlignment(Pos.CENTER_RIGHT);
        headerBar.setPadding(new Insets(20, 30, 0, 30));
        headerBar.getChildren().add(btnAide);

        // Conteneur pour l'heure centrée
        VBox heureContainer = new VBox(labelHeure);
        heureContainer.setAlignment(Pos.CENTER);
        VBox.setVgrow(heureContainer, Priority.ALWAYS);

        // ── Zone centrale ─────────────────────────────────────────────────
        VBox centre = new VBox(32, heureContainer, labelNomTache, labelSousInfo, btnOui);
        centre.setAlignment(Pos.CENTER);
        VBox.setVgrow(centre, Priority.ALWAYS);

        // ── Salutation en bas ─────────────────────────────────────────────
        Label pied = creerLabel(
                "Appuie sur le bouton quand tu as fait la tâche 😊",
                20, false, C_SUBTIL
        );
        pied.setPadding(new Insets(0, 0, 36, 0));

        // ── Racine ────────────────────────────────────────────────────────
        VBox root = new VBox(bandeauCouleur, headerBar, centre, pied);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + C_FOND + ";");
        VBox.setVgrow(centre, Priority.ALWAYS);

        Scene scene = new Scene(root, 700, 900);

        // ── Touche Entrée = clic OUI ──────────────────────────────────────
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (!btnOui.isDisabled()) btnOui.fire();
            }
            // Nouveau : touche F1 ou A pour ouvrir l'aide
            if (event.getCode() == KeyCode.F1 || event.getCode() == KeyCode.A) {
                afficherAide();
            }
        });

        stage.setTitle("MémoGuide – Lucien");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();

        demarrerHorloge();
    }

    // ── NOUVELLE MÉTHODE : Afficher l'aide ───────────────────────────────────

    private void afficherAide() {
        Alert aide = new Alert(Alert.AlertType.INFORMATION);
        aide.setTitle("Aide - MémoGuide");
        aide.setHeaderText("📱 Comment utiliser MémoGuide ?");

        // Texte avec des retours à la ligne explicites
        String message =
                "Bienvenue Lucien ! 🌟\n\n" +
                        "MémoGuide t'aide à te souvenir des activités importantes de ta journée.\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "📌 COMMENT ÇA MARCHE :\n\n" +
                        "   • Une tâche s'affiche en grand au centre de l'écran\n" +
                        "   • Quand tu as terminé la tâche, appuie sur le bouton OUI\n" +
                        "   • Tu peux aussi utiliser la touche ENTREE du clavier\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "⏰ LES RAPPELS :\n\n" +
                        "   • La bande colorée en haut change selon l'urgence :\n" +
                        "       - 🟢 VERT = tranquille, il reste du temps\n" +
                        "       - 🟠 ORANGE = bientôt l'heure limite\n" +
                        "       - 🔴 ROUGE = très urgent, plus que 15 minutes !\n" +
                        "   • Un bip sonore te rappelle si tu as oublié après 30 minutes\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "💡 CONSEIL :\n\n" +
                        "   N'hésite pas à demander de l'aide à un proche si besoin !";

        aide.setContentText(message);

        // Agrandir la taille de l'alerte
        aide.getDialogPane().setMinHeight(600);
        aide.getDialogPane().setMinWidth(550);
        aide.getDialogPane().setStyle("-fx-font-size: 13px;");

        aide.showAndWait();
    }

    // ── Horloge ───────────────────────────────────────────────────────────────

    private void demarrerHorloge() {
        Timeline horloge = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalTime now = LocalTime.now();
            labelHeure.setText(now.format(FMT));

            tacheManager.mettreAJour(now);
            Optional<Tache> active = tacheManager.getTacheActive(now);

            if (active.isPresent()) {
                Tache t = active.get();
                tacheEnCours = t;
                labelNomTache.setText(t.getNom());

                if (t.isEstValidee()) {
                    afficherValide();
                } else {
                    afficherAFaire(t, now);

                    // Rappel sonore 30 min après le début si non validée
                    if (t.doitDeclenchemerRappel(now)) {
                        SonRappel.bipRappel();
                        System.out.println("[🔔 RAPPEL] " + t.getNom());
                    }
                }
            } else {
                tacheEnCours = null;
                labelNomTache.setText("Rien à faire\npour l'instant 😊");
                labelSousInfo.setText("Profite de ton temps libre !");
                labelSousInfo.setStyle(styleLabel(C_SUBTIL));
                stylesBouton(C_GRIS);
                bandeauCouleur.setStyle("-fx-background-color: " + C_GRIS + ";");
                btnOui.setDisable(true);
            }
        }));

        horloge.setCycleCount(Timeline.INDEFINITE);
        horloge.play();
    }

    // ── États visuels ─────────────────────────────────────────────────────────

    private void afficherValide() {
        btnOui.setText("✓  C'est fait !");
        stylesBouton(C_GRIS);
        bandeauCouleur.setStyle("-fx-background-color: " + C_VERT + ";");
        labelSousInfo.setText("Bravo Lucien ! 🎉");
        labelSousInfo.setStyle(styleLabel(C_VERT));
        btnOui.setDisable(true);
    }

    private void afficherAFaire(Tache t, LocalTime now) {
        btnOui.setText("OUI  ✓");
        btnOui.setDisable(false);

        long min = java.time.temporal.ChronoUnit.MINUTES.between(now, t.getHeureReset());
        String couleur;
        String info;

        if (min <= 15) {
            couleur = C_ROUGE;
            info    = "⚠️ Urgent ! Encore " + min + " minutes";
        } else if (min <= 45) {
            couleur = C_ORANGE;
            info    = "🕐 Plus que " + min + " minutes";
        } else {
            couleur = C_VERT;
            info    = "🕐 " + t.getPlageHoraire();
        }

        labelSousInfo.setText(info);
        labelSousInfo.setStyle(styleLabel(couleur));
        stylesBouton(couleur);
        bandeauCouleur.setStyle("-fx-background-color: " + couleur + ";");
    }

    // ── Action OUI ────────────────────────────────────────────────────────────

    private void actionValidation() {
        if (tacheEnCours == null || tacheEnCours.isEstValidee()) return;
        tacheEnCours.valider();

        boolean enRetard = java.time.temporal.ChronoUnit.MINUTES
                .between(tacheEnCours.getHeureDebut(), LocalTime.now()) > 30;
        historiqueManager.enregistrerValidation(tacheEnCours, enRetard);

        SonRappel.bipValidation();

        ScaleTransition pulse = new ScaleTransition(Duration.millis(140), btnOui);
        pulse.setFromX(1.0); pulse.setFromY(1.0);
        pulse.setToX(1.06);  pulse.setToY(1.06);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.setOnFinished(e -> afficherValide());
        pulse.play();
    }

    @Override
    public void stop() {
        if (dashboardServeur != null) {
            dashboardServeur.arreter();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Label creerLabel(String texte, int taille, boolean bold, String couleur) {
        Label l = new Label(texte);
        l.setStyle(
                "-fx-font-size: " + taille + "px;" +
                        (bold ? "-fx-font-weight: bold;" : "") +
                        "-fx-text-fill: " + couleur + ";"
        );
        return l;
    }

    private void stylesBouton(String couleur) {
        btnOui.setStyle(
                "-fx-background-color: " + couleur + ";" +
                        "-fx-background-radius: 24;" +
                        "-fx-font-size: 52px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;"
        );
    }

    private String styleLabel(String couleur) {
        return "-fx-font-size: 26px; -fx-text-fill: " + couleur + ";";
    }

    public static void main(String[] args) {
        launch(args);
    }
}