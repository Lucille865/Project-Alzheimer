import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.sun.net.httpserver.HttpServer;

/**
 * Interface unifiée MémoGuide – Lucien.
 *
 * Combine :
 *  – la version simple  (bandeau urgence, grands visuels, SonRappel, touche Entrée)
 *  – la version complète (dashboard HTTP, serveur Wi-Fi physique, simulation, barre de progression)
 *
 * Bugs corrigés par rapport aux versions originales :
 *  1. enRetard dans actionValidation() : utilise désormais un calcul direct de durée
 *     (l'ancienne version appelait doitDeclenchemerRappel() après valider(), ce qui
 *     renvoyait toujours false car estValidee était déjà true).
 *  2. Rappels sonores : remplace java.awt.Toolkit.beep() par SonRappel.bipRappel().
 *  3. Log serveur Wi-Fi : corrigé de "8080" → "8081".
 */
public class InterfaceLucien extends Application {

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

    private static final DateTimeFormatter FMT      = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FMT_HHMM = DateTimeFormatter.ofPattern("HH:mm");

    // ── État ─────────────────────────────────────────────────────────────────
    private TacheManager      tacheManager;
    private HistoriqueManager historiqueManager;
    private DashboardServeur  dashboardServeur;
    private HttpServer        serveurWiFi;

    private Tache tacheEnCours  = null;
    private long  offsetMinutes = 0; // décalage de simulation en minutes

    // ── Composants UI ────────────────────────────────────────────────────────
    private Region     bandeauCouleur;  // bande colorée en haut – indicateur d'urgence (version Simple)
    private Label      labelHeure;
    private Label      labelTache;
    private Label      labelPlage;
    private Label      labelStatut;
    private Label      labelProgres;
    private Label      labelOffsetInfo;
    private ProgressBar progressBar;
    private Button     btnOui;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void start(Stage stage) {
        tacheManager      = new TacheManager();
        historiqueManager = new HistoriqueManager();
        dashboardServeur  = new DashboardServeur(tacheManager, historiqueManager);
        dashboardServeur.demarrer();

        // ── Bandeau urgence (hérité de la version simple) ─────────────────
        bandeauCouleur = new Region();
        bandeauCouleur.setPrefHeight(10);
        bandeauCouleur.setMaxWidth(Double.MAX_VALUE);
        bandeauCouleur.setStyle("-fx-background-color: " + C_GRIS + ";");

        // ── Header ────────────────────────────────────────────────────────
        labelHeure = creerLabel("00:00:00", 60, true, C_TEXTE);

        Label salutation = creerLabel("Bonjour Lucien 👋", 24, true, C_TEXTE);

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
        header.setPadding(new Insets(24, 24, 12, 24));

        Separator sep1 = new Separator();
        sep1.setMaxWidth(380);
        sep1.setStyle("-fx-background-color: " + C_BORDURE + ";");

        // ── Carte tâche principale ────────────────────────────────────────
        labelTache = creerLabel("Chargement…", 36, true, C_TEXTE);
        labelTache.setWrapText(true);
        labelTache.setMaxWidth(380);
        labelTache.setTextAlignment(TextAlignment.CENTER);
        labelTache.setAlignment(Pos.CENTER);

        labelPlage  = creerLabel("", 18, false, C_SUBTIL);
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

        // ── Bouton OUI ────────────────────────────────────────────────────
        btnOui = new Button("OUI  ✓");
        btnOui.setPrefSize(340, 140);
        stylesBouton(C_VERT);
        btnOui.setOnAction(e -> actionValidation());

        // ── Progression ───────────────────────────────────────────────────
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(380);
        progressBar.setPrefHeight(10);
        progressBar.setStyle("-fx-accent: " + C_VERT + ";");

        labelProgres = creerLabel("0 / 0 tâches effectuées", 15, false, C_SUBTIL);

        VBox progressBox = new VBox(6, progressBar, labelProgres);
        progressBox.setAlignment(Pos.CENTER);

        Separator sep2 = new Separator();
        sep2.setMaxWidth(380);
        sep2.setStyle("-fx-background-color: " + C_BORDURE + ";");

        // ── Panneau de simulation ─────────────────────────────────────────
        Label titreSim = creerLabel("🧪 Simulation", 14, true, C_SUBTIL);
        labelOffsetInfo = creerLabel("Heure simulée : aucun décalage", 14, false, C_BLEU);

        Button btn15    = creerBoutonSim("+15 min",   () -> offsetMinutes += 15);
        Button btn1h    = creerBoutonSim("+1 heure",  () -> offsetMinutes += 60);
        Button btn3h    = creerBoutonSim("+3 heures", () -> offsetMinutes += 180);
        Button btnReset = creerBoutonSim("↺ Reset", () -> {
            offsetMinutes = 0;
            tacheManager.getTaches().forEach(t -> t.verifierReset(LocalTime.of(23, 59)));
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

        // ── Salutation pied de page (hérité de la version simple) ─────────
        Label pied = creerLabel(
                "Appuie sur le bouton quand tu as fait la tâche 😊",
                14, false, C_SUBTIL
        );
        pied.setPadding(new Insets(0, 0, 12, 0));

        // ── Racine ────────────────────────────────────────────────────────
        VBox contenu = new VBox(16,
                header, sep1,
                carteTache, btnOui,
                progressBox, sep2,
                panneauSim, pied
        );
        contenu.setAlignment(Pos.CENTER);
        contenu.setPadding(new Insets(0, 20, 0, 20));
        VBox.setVgrow(contenu, Priority.ALWAYS);

        VBox root = new VBox(bandeauCouleur, contenu);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + C_FOND + ";");

        Scene scene = new Scene(root, 460, 800);

        // ── Touche Entrée = clic OUI (hérité de la version simple) ────────
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !btnOui.isDisabled()) {
                btnOui.fire();
            }
        });

        stage.setTitle("MémoGuide – Lucien");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setOnCloseRequest(e -> {
            stop();
            Platform.exit();
        });
        stage.show();

        lancerServeurWiFi();
        demarrerHorloge();
    }

    // ── Horloge ───────────────────────────────────────────────────────────────

    private void demarrerHorloge() {
        Timeline horloge = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalTime now = LocalTime.now().plusMinutes(offsetMinutes);

            // Affichage heure
            labelHeure.setText(now.format(FMT));

            // Info décalage
            if (offsetMinutes == 0) {
                labelOffsetInfo.setText("Heure simulée : aucun décalage");
            } else {
                long h = offsetMinutes / 60;
                long m = offsetMinutes % 60;
                String dec = (h > 0 ? h + "h " : "") + (m > 0 ? m + "min" : "");
                labelOffsetInfo.setText("Décalage actif : +" + dec
                        + "  →  heure simulée : " + now.format(FMT_HHMM));
            }

            // Backend
            tacheManager.mettreAJour(now);
            Optional<Tache> active = tacheManager.getTacheActive(now);

            if (active.isPresent()) {
                Tache t = active.get();
                tacheEnCours = t;
                labelTache.setText(t.getNom());
                labelPlage.setText("🕐 " + t.getPlageHoraire());

                if (t.isEstValidee()) {
                    afficherEtatValide();
                } else {
                    afficherEtatAFaire(t, now);
                    // Rappel sonore 30 min après le début si non validée
                    // CORRECTION : utilise SonRappel au lieu de Toolkit.beep()
                    if (t.doitDeclenchemerRappel(now)) {
                        SonRappel.bipRappel();
                        System.out.println("[🔔 RAPPEL] " + t.getNom() + " non validée depuis 30 min !");
                    }
                }
            } else {
                tacheEnCours = null;
                labelTache.setText("Pas de tâche\npour l'instant 😊");
                labelPlage.setText("");
                labelStatut.setText("Profite de ton temps libre !");
                labelStatut.setStyle(styleLabelStatut(C_SUBTIL));
                stylesBouton(C_GRIS);
                bandeauCouleur.setStyle("-fx-background-color: " + C_GRIS + ";");
                btnOui.setDisable(true);
            }

            // Progression
            long validees = tacheManager.getNbValidees();
            int  total    = tacheManager.getNbTotal();
            progressBar.setProgress(total == 0 ? 0 : (double) validees / total);
            labelProgres.setText(validees + " / " + total + " tâches effectuées");
        }));

        horloge.setCycleCount(Timeline.INDEFINITE);
        horloge.play();
    }

    // ── États visuels ─────────────────────────────────────────────────────────

    private void afficherEtatValide() {
        btnOui.setText("✓  C'est fait !");
        labelStatut.setText("✅ Bravo Lucien, c'est fait !");
        labelStatut.setStyle(styleLabelStatut(C_VERT));
        stylesBouton(C_GRIS);
        bandeauCouleur.setStyle("-fx-background-color: " + C_VERT + ";");
        btnOui.setDisable(true);
    }

    private void afficherEtatAFaire(Tache t, LocalTime now) {
        btnOui.setText("OUI  ✓");
        btnOui.setDisable(false);

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
            urgence = "À faire avant " + t.getHeureReset().format(FMT_HHMM);
            couleur = C_SUBTIL;
        }

        labelStatut.setText(urgence);
        labelStatut.setStyle(styleLabelStatut(couleur));
        stylesBouton(min <= 15 ? C_ROUGE : min <= 45 ? C_ORANGE : C_VERT);
        bandeauCouleur.setStyle("-fx-background-color: " + couleur + ";");
    }

    // ── Action bouton OUI ─────────────────────────────────────────────────────

    private void actionValidation() {
        if (tacheEnCours == null || tacheEnCours.isEstValidee()) return;

        // CORRECTION : calcul enRetard AVANT d'appeler valider()
        // L'ancienne version appelait doitDeclenchemerRappel() après valider(),
        // ce qui renvoyait toujours false (estValidee == true à ce moment-là).
        LocalTime maintenant = LocalTime.now().plusMinutes(offsetMinutes);
        boolean enRetard = java.time.temporal.ChronoUnit.MINUTES
                .between(tacheEnCours.getHeureDebut(), maintenant) > 30;

        tacheEnCours.valider();
        historiqueManager.enregistrerValidation(tacheEnCours, enRetard);

        // Son de confirmation (hérité de la version simple)
        SonRappel.bipValidation();

        ScaleTransition pulse = new ScaleTransition(Duration.millis(140), btnOui);
        pulse.setFromX(1.0); pulse.setFromY(1.0);
        pulse.setToX(1.08);  pulse.setToY(1.08);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.setOnFinished(e -> afficherEtatValide());
        pulse.play();
    }

    // ── Serveur Wi-Fi (bouton physique Arduino) ───────────────────────────────

    private void lancerServeurWiFi() {
        try {
            serveurWiFi = HttpServer.create(new InetSocketAddress(8081), 0);
            serveurWiFi.createContext("/bouton", exchange -> {
                String query = exchange.getRequestURI().getQuery();

                if (query != null && query.contains("action=valider")) {
                    Platform.runLater(this::actionValidation);
                    System.out.println("[WIFI] Validation reçue du bouton physique !");
                }

                // Renvoie l'état LED : V = validé, R = à faire, X = aucune tâche
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
            // CORRECTION : le message indiquait "8080" alors que le port est 8081
            System.out.println("[SERVEUR] Wi-Fi actif sur le port 8081");
        } catch (IOException e) {
            System.err.println("[Serveur Wi-Fi] Erreur démarrage : " + e.getMessage());
        }
    }

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    @Override
    public void stop() {
        if (dashboardServeur != null) dashboardServeur.arreter();
        if (serveurWiFi != null) {
            serveurWiFi.stop(0);
            System.out.println("[Matériel] Serveur Wi-Fi arrêté.");
        }
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

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

    public static void main(String[] args) { launch(args); }
}