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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import javafx.application.Platform;
import com.sun.net.httpserver.HttpServer;

/**
 * Interface simplifiée pour la tablette de Lucien.
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
    private long offsetMinutes = 0;

    private HttpServer serveurWiFi;

    // Flag pour savoir si on est en mode validation
    private boolean enModeValidation = false;
    private String nomTacheValidee = "";

    // ── Composants UI ────────────────────────────────────────────────────────
    private Label labelHeure;
    private Label labelOffsetInfo;
    private Label labelNomTache;
    private Label labelSousInfo;
    private Button btnOui;
    private Region bandeauCouleur;

    @Override
    public void start(Stage stage) {
        tacheManager = new TacheManager();
        historiqueManager = new HistoriqueManager();

        dashboardServeur = new DashboardServeur(tacheManager, historiqueManager);
        dashboardServeur.demarrer();

        NodeUI ui = construireInterface();
        Scene scene = new Scene(ui.root, 700, 900);

        configurerRaccourcisClavier(scene);

        stage.setTitle("MémoGuide – Lucien");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();

        lancerServeurWiFi();

        demarrerHorloge();
    }

    private NodeUI construireInterface() {
        bandeauCouleur = new Region();
        bandeauCouleur.setPrefHeight(10);
        bandeauCouleur.setMaxWidth(Double.MAX_VALUE);
        bandeauCouleur.setStyle("-fx-background-color: " + C_GRIS + ";");

        labelHeure = new Label("00:00");
        labelHeure.setStyle(
                "-fx-font-size: 72px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + C_TEXTE + ";" +
                        "-fx-font-family: 'Arial';"
        );
        labelHeure.setAlignment(Pos.CENTER);

        labelOffsetInfo = new Label();
        labelOffsetInfo.setStyle("-fx-font-size: 16px; -fx-text-fill: " + C_SUBTIL + ";");
        labelOffsetInfo.setAlignment(Pos.CENTER);
        mettreAJourAffichageOffset();

        labelNomTache = creerLabel("Chargement…", 64, true, C_TEXTE);
        labelNomTache.setWrapText(true);
        labelNomTache.setMaxWidth(600);
        labelNomTache.setTextAlignment(TextAlignment.CENTER);
        labelNomTache.setAlignment(Pos.CENTER);

        labelSousInfo = creerLabel("", 26, false, C_SUBTIL);

        btnOui = new Button("OUI  ✓");
        btnOui.setPrefSize(420, 200);
        btnOui.setFocusTraversable(false);
        appliquerStyleBouton(btnOui, C_VERT);
        btnOui.setOnAction(e -> actionValidation());

        VBox centre = new VBox(20, labelHeure, labelOffsetInfo, labelNomTache, labelSousInfo, btnOui);
        centre.setAlignment(Pos.CENTER);
        VBox.setVgrow(centre, Priority.ALWAYS);

        Label pied = creerLabel(
                "Appuie sur le bouton quand tu as fait la tâche 😊\n\n" +
                        "← → : avancer/reculer de 15 min    ↑ : réinitialiser l'heure",
                16, false, C_SUBTIL
        );
        pied.setPadding(new Insets(0, 0, 36, 0));
        pied.setTextAlignment(TextAlignment.CENTER);

        // Barre des boutons (Valider et Aide)
        Button btnValider = new Button("✓ Oui");
        btnValider.setPrefSize(100, 50);
        btnValider.setFocusTraversable(false);
        appliquerStyleBouton(btnValider, C_VERT);
        btnValider.setOnAction(e -> actionValidation());

        Button btnAide = new Button("❓ Aide");
        btnAide.setPrefSize(100, 50);
        btnAide.setFocusTraversable(false);
        appliquerStyleBouton(btnAide, C_BLEU);
        btnAide.setOnAction(e -> afficherAide());

        HBox headerBar = new HBox(450);
        headerBar.setAlignment(Pos.CENTER_RIGHT);
        headerBar.setPadding(new Insets(20, 30, 0, 30));
        headerBar.getChildren().addAll(btnValider, btnAide);
        HBox.setMargin(btnAide, new Insets(0, 430, 0, 0));

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
        Timeline horloge = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            // Ne pas mettre à jour si on est en mode validation
            if (!enModeValidation) {
                mettreAJourTouteLInterface();
            }
        }));
        horloge.setCycleCount(Timeline.INDEFINITE);
        horloge.play();
    }

    // ── Mise à jour complète de l'interface ──────────────────────────────────
    private void mettreAJourTouteLInterface() {
        LocalTime now = LocalTime.now().plusMinutes(offsetMinutes);
        labelHeure.setText(now.format(FMT));

        tacheManager.mettreAJour(now);
        Optional<Tache> active = tacheManager.getTacheActive(now);

        // Cas 1 : Une tâche est actuellement en mode validation (déjà validée)
        if (enModeValidation && tacheEnCours != null && tacheEnCours.isEstValidee()) {
            if (now.isBefore(tacheEnCours.getHeureReset())) {
                // Reste en mode validation tant que l'heure de reset n'est pas dépassée
                afficherEtatValide(tacheEnCours.getNom(), tacheEnCours.getHeureReset());
                return;
            } else {
                // L'heure de reset est dépassée, on sort du mode validation
                enModeValidation = false;
                nomTacheValidee = "";
                tacheEnCours = null;
            }
        }

        // Cas 2 : Une tâche active est présente (non validée ou nouvelle)
        if (active.isPresent()) {
            Tache t = active.get();

            // Si cette tâche est déjà validée ET qu'on est encore dans son créneau
            if (t.isEstValidee() && now.isBefore(t.getHeureReset())) {
                tacheEnCours = t;
                afficherEtatValide(t.getNom(), t.getHeureReset());
                return;
            }

            // Détection nouvelle tâche (pour le son)
            boolean estNouvelleTache = (tacheEnCours == null || !tacheEnCours.getNom().equals(t.getNom()));
            if (estNouvelleTache && !t.isEstValidee()) {
                SonRappel.jouerSonActivation();
                System.out.println("🔊 Nouvelle tâche active: " + t.getNom());
            }

            // Mise à jour de la tâche courante
            tacheEnCours = t;
            labelNomTache.setText(t.getNom());

            // Affichage selon l'état (TOUJOURS réactiver le bouton ici)
            afficherEtatAFaire(t, now);  // Cette méthode doit réactiver btnOui

            // Son de rappel 30 minutes
            if (t.doitDeclenchemerRappel(now)) {
                SonRappel.jouerSonRappel();
                System.out.println("[🔔 RAPPEL] " + t.getNom());
            }

        } else {
            // Cas 3 : Aucune tâche active
            tacheEnCours = null;
            enModeValidation = false;
            nomTacheValidee = "";

            labelNomTache.setText("Rien à faire\npour l'instant 😊");
            labelSousInfo.setText("Profite de ton temps libre !");
            labelSousInfo.setStyle(styleLabel(C_SUBTIL));
            appliquerStyleBouton(btnOui, C_GRIS);
            bandeauCouleur.setStyle("-fx-background-color: " + C_GRIS + ";");
            btnOui.setDisable(false);
        }
    }

    // ── Affichage des états ──────────────────────────────────────────────────
    private void afficherEtatValide(String nomTache, LocalTime heureReset) {
        enModeValidation = true;
        nomTacheValidee = nomTache;

        btnOui.setText("✓  C'est fait !");
        btnOui.setDisable(false);
        appliquerStyleBouton(btnOui, C_VERT);
        bandeauCouleur.setStyle("-fx-background-color: " + C_VERT + ";");
        labelSousInfo.setText("Bravo Lucien ! 🎉");
        labelSousInfo.setStyle(styleLabel(C_VERT));

        // Garder le nom de la tâche validée affiché
        labelNomTache.setText(nomTache);

        // Calculer le temps restant jusqu'à l'heure de reset
        LocalTime now = LocalTime.now().plusMinutes(offsetMinutes);
        long minutesRestantes = java.time.temporal.ChronoUnit.MINUTES.between(now, heureReset);

        if (minutesRestantes > 0) {
            // Programmer la sortie du mode validation à l'heure de reset
            Timeline sortieValidation = new Timeline(new KeyFrame(Duration.minutes(minutesRestantes), e -> {
                enModeValidation = false;
                nomTacheValidee = "";
                // Forcer une mise à jour pour passer à la tâche suivante
                mettreAJourTouteLInterface();
            }));
            sortieValidation.setCycleCount(1);
            sortieValidation.play();
            System.out.println("✅ Tâche validée, reste " + minutesRestantes + " min avant la prochaine tâche");
        } else {
            // Si on est déjà après l'heure de reset, passer directement
            enModeValidation = false;
            nomTacheValidee = "";
            mettreAJourTouteLInterface();
        }
    }

    private void afficherEtatAFaire(Tache t, LocalTime now) {
        btnOui.setText("OUI  ✓");
        btnOui.setDisable(false);

        long minutesEcoulees = java.time.temporal.ChronoUnit.MINUTES.between(t.getHeureDebut(), now);
        long minutesRestantes = java.time.temporal.ChronoUnit.MINUTES.between(now, t.getHeureReset());
        String couleur;
        String info;

        // Si la tâche a dépassé 30 minutes depuis son début -> ROUGE
        if (minutesEcoulees >= 30 && !t.isEstValidee()) {
            couleur = C_ROUGE;
            info = " RAPPEL ! Tâche non faite depuis 30 minutes !";
        }
        // Si moins de 15 minutes restantes -> ROUGE aussi (urgence)
        else if (minutesRestantes <= 15) {
            couleur = C_ROUGE;
            info = "⚠Urgent ! Encore " + minutesRestantes + " minutes";
        }
        // Sinon, tâche en cours -> ORANGE
        else {
            couleur = C_ORANGE;
            info = " À faire : " + t.getPlageHoraire();
        }

        labelSousInfo.setText(info);
        labelSousInfo.setStyle(styleLabel(couleur));
        appliquerStyleBouton(btnOui, couleur);
        bandeauCouleur.setStyle("-fx-background-color: " + couleur + ";");
    }

    // ── Actions ──────────────────────────────────────────────────────────────
    private void actionValidation() {
        if (tacheEnCours == null || tacheEnCours.isEstValidee()) return;
        if (enModeValidation) return;

        String nomTache = tacheEnCours.getNom();
        LocalTime heureReset = tacheEnCours.getHeureReset();  // Récupérer l'heure de reset

        tacheEnCours.valider();

        boolean enRetard = java.time.temporal.ChronoUnit.MINUTES
                .between(tacheEnCours.getHeureDebut(), LocalTime.now().plusMinutes(offsetMinutes)) > 30;
        historiqueManager.enregistrerValidation(tacheEnCours, enRetard);

        // Afficher l'état validé avec l'heure de reset
        afficherEtatValide(nomTache, heureReset);
    }

    private void afficherAide() {
        /*Alert aide = new Alert(Alert.AlertType.INFORMATION);
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
        aide.showAndWait();*/
        SonRappel.jouerSonAide();

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

    // ── Serveur Wi-Fi (Bouton Physique Arduino) ────────────────────

    private void lancerServeurWiFi() {
        try {
            serveurWiFi = HttpServer.create(new InetSocketAddress(8082), 0);
            serveurWiFi.createContext("/bouton", exchange -> {
                String query = exchange.getRequestURI().getQuery();

                if (query != null) {
                    if (query.contains("action=valider")) {
                        Platform.runLater(this::actionValidation);
                        System.out.println("[WIFI] Validation reçue du bouton physique !");
                    }
                    // --- NOUVEAU : DETECTION DU BOUTON AIDE ---
                    else if (query.contains("action=aide")) {
                        Platform.runLater(this::afficherAide);
                        System.out.println("[WIFI] Demande d'aide reçue du bouton physique !");
                    }
                }

                // Réponse standard pour les LEDs de l'Arduino
                String signal = "X";
                if (tacheEnCours != null) {
                    signal = tacheEnCours.isEstValidee() ? "V" : "R";
                }

                exchange.sendResponseHeaders(200, signal.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(signal.getBytes());
                }
            });
            serveurWiFi.setExecutor(null);
            serveurWiFi.start();
            System.out.println("[SERVEUR] Wi-Fi Arduino actif sur le port 8082 (Boutons: Valider & Aide)");
        } catch (IOException e) {
            System.err.println("[SERVEUR] Erreur Wi-Fi : " + e.getMessage());
        }
    }




    // ── Arrêt de l'application ───────────────────────────────────────────────
    @Override
    public void stop() {
        if (dashboardServeur != null) {
            dashboardServeur.arreter();
        }
        if (serveurWiFi != null) {
            serveurWiFi.stop(0);
            System.out.println("[MATÉRIEL] Serveur Wi-Fi Arduino arrêté.");
        }
    }

    // ── Point d'entrée ───────────────────────────────────────────────────────
    public static void main(String[] args) {
        launch(args);
    }
}