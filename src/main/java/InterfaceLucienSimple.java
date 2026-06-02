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

/**
 * Interface simplifiée pour la tablette de Lucien.
 * – Tâche courante en très grand
 * – Touche Entrée = clic sur le bouton
 * – Rappel sonore automatique 30 min après le début si non validée
 * – Flèches directionnelles pour simuler l'avancement du temps
 */
public class InterfaceLucienSimple extends Application {

    // ── Palette de couleurs ──────────────────────────────────────────────────
    private static final String C_FOND   = "#0d1117";
    private static final String C_TEXTE  = "#e6edf3";
    private static final String C_SUBTIL = "#8b949e";
    private static final String C_VERT   = "#3fb950";
    private static final String C_ROUGE  = "#f85149";
    private static final String C_ORANGE = "#d29922";
    private static final String C_GRIS   = "#30363d";
    private static final String C_BLEU   = "#388bfd";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── État de l'application ────────────────────────────────────────────────
    private TacheManager tacheManager;
    private HistoriqueManager historiqueManager;
    private DashboardServeur dashboardServeur;
    private Tache tacheEnCours = null;
    private long offsetMinutes = 0;  // Décalage horaire pour la simulation

    // ── Composants UI ────────────────────────────────────────────────────────
    private Label labelHeure;
    private Label labelOffsetInfo;
    private Label labelNomTache;
    private Label labelSousInfo;
    private Button btnOui;
    private Region bandeauCouleur;

    // ── Initialisation ───────────────────────────────────────────────────────
    @Override
    public void start(Stage stage) {
        // Initialisation des managers
        tacheManager = new TacheManager();
        historiqueManager = new HistoriqueManager();

        // Démarrage du serveur dashboard
        dashboardServeur = new DashboardServeur(tacheManager, historiqueManager);
        dashboardServeur.demarrer();

        // Construction de l'interface
        NodeUI ui = construireInterface();
        Scene scene = new Scene(ui.root, 700, 900);

        // Configuration des raccourcis clavier
        configurerRaccourcisClavier(scene);

        // Configuration de la scène
        stage.setTitle("MémoGuide – Lucien");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();

        // Démarrage de l'horloge
        demarrerHorloge();
    }

    // ── Construction de l'interface ──────────────────────────────────────────
    private NodeUI construireInterface() {
        // Bandeau couleur (indicateur d'urgence)
        bandeauCouleur = new Region();
        bandeauCouleur.setPrefHeight(10);
        bandeauCouleur.setMaxWidth(Double.MAX_VALUE);
        bandeauCouleur.setStyle("-fx-background-color: " + C_GRIS + ";");

        // Heure
        labelHeure = new Label("00:00");
        labelHeure.setStyle(
                "-fx-font-size: 72px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + C_TEXTE + ";" +
                        "-fx-font-family: 'Arial';"
        );
        labelHeure.setAlignment(Pos.CENTER);

        // Info décalage horaire
        labelOffsetInfo = new Label();
        labelOffsetInfo.setStyle("-fx-font-size: 16px; -fx-text-fill: " + C_SUBTIL + ";");
        labelOffsetInfo.setAlignment(Pos.CENTER);
        mettreAJourAffichageOffset();

        // Nom de la tâche
        labelNomTache = creerLabel("Chargement…", 64, true, C_TEXTE);
        labelNomTache.setWrapText(true);
        labelNomTache.setMaxWidth(600);
        labelNomTache.setTextAlignment(TextAlignment.CENTER);
        labelNomTache.setAlignment(Pos.CENTER);

        // Sous-info
        labelSousInfo = creerLabel("", 26, false, C_SUBTIL);

        // Bouton OUI principal
        btnOui = new Button("OUI  ✓");
        btnOui.setPrefSize(420, 200);
        btnOui.setFocusTraversable(false);
        appliquerStyleBouton(btnOui, C_VERT);
        btnOui.setOnAction(e -> actionValidation());

        // Zone centrale
        VBox centre = new VBox(20, labelHeure, labelOffsetInfo, labelNomTache, labelSousInfo, btnOui);
        centre.setAlignment(Pos.CENTER);
        VBox.setVgrow(centre, Priority.ALWAYS);

        // Pied de page
        Label pied = creerLabel(
                "Appuie sur le bouton quand tu as fait la tâche 😊\n\n" +
                        "← → : avancer/reculer de 15 min    ↑ : réinitialiser l'heure",
                16, false, C_SUBTIL
        );
        pied.setPadding(new Insets(0, 0, 36, 0));
        pied.setTextAlignment(TextAlignment.CENTER);

        // Barre des boutons (Valider et Aide)
        Button btnValider = new Button("✓ Valider");
        btnValider.setPrefSize(100, 50);
        btnValider.setFocusTraversable(false);
        appliquerStyleBouton(btnValider, C_VERT);
        btnValider.setOnAction(e -> actionValidation());

        Button btnAide = new Button("❓ Aide");
        btnAide.setPrefSize(100, 50);
        btnAide.setFocusTraversable(false);
        appliquerStyleBouton(btnAide, C_BLEU);
        btnAide.setOnAction(e -> afficherAide());

        HBox headerBar = new HBox(230);
        headerBar.setAlignment(Pos.CENTER_RIGHT);
        headerBar.setPadding(new Insets(20, 30, 0, 30));
        headerBar.getChildren().addAll(btnValider, btnAide);
        HBox.setMargin(btnAide, new Insets(0, 500, 0, 0));

        // Racine
        VBox root = new VBox(bandeauCouleur, headerBar, centre, pied);
        root.setPadding(new Insets(50, 0, 0, 0));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: " + C_FOND + ";");
        VBox.setVgrow(centre, Priority.ALWAYS);

        return new NodeUI(root);
    }

    // ── Raccourcis clavier ───────────────────────────────────────────────────
    private void configurerRaccourcisClavier(Scene scene) {
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();

            switch (code) {
                case RIGHT:
                    offsetMinutes += 15;
                    mettreAJourAffichageOffset();
                    mettreAJourTouteLInterface();
                    System.out.println("⏩ +15 min : décalage = " + offsetMinutes + " min");
                    event.consume();
                    break;
                case LEFT:
                    offsetMinutes -= 15;
                    if (offsetMinutes < 0) offsetMinutes = 0;
                    mettreAJourAffichageOffset();
                    mettreAJourTouteLInterface();
                    System.out.println("⏪ -15 min : décalage = " + offsetMinutes + " min");
                    event.consume();
                    break;
                case UP:
                    offsetMinutes = 0;
                    mettreAJourAffichageOffset();
                    mettreAJourTouteLInterface();
                    System.out.println("🔄 Réinitialisation de l'heure");
                    event.consume();
                    break;
                case ENTER:
                    if (!btnOui.isDisabled()) {
                        actionValidation();
                    }
                    event.consume();
                    break;
                case F1:
                case A:
                    afficherAide();
                    event.consume();
                    break;
                default:
                    // Ignorer les autres touches
                    break;
            }
        });
    }

    // ── Horloge (mise à jour automatique) ────────────────────────────────────
    private void demarrerHorloge() {
        Timeline horloge = new Timeline(new KeyFrame(Duration.seconds(1), e -> mettreAJourTouteLInterface()));
        horloge.setCycleCount(Timeline.INDEFINITE);
        horloge.play();
    }

    // ── Mise à jour complète de l'interface ──────────────────────────────────
    private void mettreAJourTouteLInterface() {
        LocalTime now = LocalTime.now().plusMinutes(offsetMinutes);
        labelHeure.setText(now.format(FMT));

        tacheManager.mettreAJour(now);
        Optional<Tache> active = tacheManager.getTacheActive(now);

        if (active.isPresent()) {
            Tache t = active.get();

            // Détection du changement de tâche pour le son d'activation
            boolean estNouvelleTache = (tacheEnCours == null || !tacheEnCours.getNom().equals(t.getNom()));

            if (estNouvelleTache && !t.isEstValidee()) {
                // Petite pause pour éviter le chevauchement
                javafx.application.Platform.runLater(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}
                    SonRappel.jouerSonActivation();
                });
                System.out.println("🔊 Nouvelle tâche active: " + t.getNom());
            }

            tacheEnCours = t;
            labelNomTache.setText(t.getNom());

            if (t.isEstValidee()) {
                afficherEtatValide();
            } else {
                afficherEtatAFaire(t, now);

                // Son de rappel (30 minutes) - éviter si c'est une nouvelle tâche
                if (!estNouvelleTache && t.doitDeclenchemerRappel(now)) {
                    SonRappel.jouerSonRappel();
                    System.out.println("[🔔 RAPPEL] " + t.getNom());
                }
            }
        } else {
            tacheEnCours = null;
            labelNomTache.setText("Rien à faire\npour l'instant 😊");
            labelSousInfo.setText("Profite de ton temps libre !");
            labelSousInfo.setStyle(styleLabel(C_SUBTIL));
            appliquerStyleBouton(btnOui, C_GRIS);
            bandeauCouleur.setStyle("-fx-background-color: " + C_GRIS + ";");
            btnOui.setDisable(true);
        }
    }

    // ── Affichage des états ──────────────────────────────────────────────────
    private void afficherEtatValide() {
        btnOui.setText("✓  C'est fait !");
        appliquerStyleBouton(btnOui, C_GRIS);
        bandeauCouleur.setStyle("-fx-background-color: " + C_VERT + ";");
        labelSousInfo.setText("Bravo Lucien ! 🎉");
        labelSousInfo.setStyle(styleLabel(C_VERT));
        btnOui.setDisable(true);
    }

    private void afficherEtatAFaire(Tache t, LocalTime now) {
        btnOui.setText("OUI  ✓");
        btnOui.setDisable(false);

        long minRestantes = java.time.temporal.ChronoUnit.MINUTES.between(now, t.getHeureReset());
        String couleur;
        String info;

        if (minRestantes <= 15) {
            couleur = C_ROUGE;
            info = "⚠️ Urgent ! Encore " + minRestantes + " minutes";
        } else if (minRestantes <= 45) {
            couleur = C_ORANGE;
            info = "🕐 Plus que " + minRestantes + " minutes";
        } else {
            couleur = C_VERT;
            info = "🕐 " + t.getPlageHoraire();
        }

        labelSousInfo.setText(info);
        labelSousInfo.setStyle(styleLabel(couleur));
        appliquerStyleBouton(btnOui, couleur);
        bandeauCouleur.setStyle("-fx-background-color: " + couleur + ";");
    }

    // ── Actions ──────────────────────────────────────────────────────────────
    private void actionValidation() {
        if (tacheEnCours == null || tacheEnCours.isEstValidee()) return;

        tacheEnCours.valider();

        boolean enRetard = java.time.temporal.ChronoUnit.MINUTES
                .between(tacheEnCours.getHeureDebut(), LocalTime.now().plusMinutes(offsetMinutes)) > 30;
        historiqueManager.enregistrerValidation(tacheEnCours, enRetard);

        SonRappel.jouerSonValidation();

        // Animation du bouton
        ScaleTransition pulse = new ScaleTransition(Duration.millis(140), btnOui);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.06);
        pulse.setToY(1.06);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.setOnFinished(e -> afficherEtatValide());
        pulse.play();
    }

    private void afficherAide() {
        Alert aide = new Alert(Alert.AlertType.INFORMATION);
        aide.setTitle("Aide - MémoGuide");
        aide.setHeaderText("📱 Comment utiliser MémoGuide ?");

        aide.setContentText(
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
                        "   N'hésite pas à demander de l'aide à un proche si besoin !"
        );

        aide.getDialogPane().setMinHeight(600);
        aide.getDialogPane().setMinWidth(550);
        aide.getDialogPane().setStyle("-fx-font-size: 13px;");
        aide.showAndWait();
    }

    // ── Méthodes utilitaires ─────────────────────────────────────────────────
    private void mettreAJourAffichageOffset() {
        if (offsetMinutes == 0) {
            labelOffsetInfo.setText("🕐 Heure réelle");
            labelOffsetInfo.setStyle("-fx-font-size: 16px; -fx-text-fill: " + C_SUBTIL + ";");
        } else {
            long heures = offsetMinutes / 60;
            long minutes = offsetMinutes % 60;
            String decalage = (heures > 0 ? heures + "h " : "") + (minutes > 0 ? minutes + "min" : "");
            labelOffsetInfo.setText("⏰ Simulation : +" + decalage + " (← → pour ajuster, ↑ pour reset)");
            labelOffsetInfo.setStyle("-fx-font-size: 16px; -fx-text-fill: " + C_ORANGE + ";");
        }
    }

    private Label creerLabel(String texte, int taille, boolean bold, String couleur) {
        Label label = new Label(texte);
        label.setStyle(
                "-fx-font-size: " + taille + "px;" +
                        (bold ? "-fx-font-weight: bold;" : "") +
                        "-fx-text-fill: " + couleur + ";"
        );
        return label;
    }

    private void appliquerStyleBouton(Button btn, String couleur) {
        btn.setStyle(
                "-fx-background-color: " + couleur + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;"
        );
    }

    private String styleLabel(String couleur) {
        return "-fx-font-size: 26px; -fx-text-fill: " + couleur + ";";
    }

    // ── Classe interne pour le retour de l'UI ────────────────────────────────
    private record NodeUI(VBox root) {}

    // ── Arrêt de l'application ───────────────────────────────────────────────
    @Override
    public void stop() {
        if (dashboardServeur != null) {
            dashboardServeur.arreter();
        }
    }

    // ── Point d'entrée ───────────────────────────────────────────────────────
    public static void main(String[] args) {
        launch(args);
    }
}