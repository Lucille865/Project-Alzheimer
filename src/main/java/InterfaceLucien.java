import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── État ─────────────────────────────────────────────────────────────────
    private TacheManager tacheManager;

    // ── Composants réutilisés ────────────────────────────────────────────────
    private Label      labelHeure;
    private Label      labelProgres;
    private ProgressBar progressBar;
    private VBox       listeTachesBox;  // contiendra les cartes dynamiques

    @Override
    public void start(Stage stage) {
        tacheManager = new TacheManager();

        // ── Header ───────────────────────────────────────────────────────────
        labelHeure = creerLabel("00:00:00", 54, true, C_TEXTE);
        Label salutation = creerLabel("Bonjour Lucien 👋", 20, false, C_SUBTIL);
        VBox header = new VBox(4, labelHeure, salutation);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(36, 20, 8, 20));

        // ── Titre section ────────────────────────────────────────────────────
        Label titreListe = creerLabel("Prochaines tâches", 17, false, C_SUBTIL);
        titreListe.setPadding(new Insets(0, 20, 0, 20));

        // ── Liste dynamique des tâches ────────────────────────────────────────
        listeTachesBox = new VBox(12);
        listeTachesBox.setPadding(new Insets(0, 20, 0, 20));

        ScrollPane scroll = new ScrollPane(listeTachesBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: " + C_FOND + "; -fx-background-color: " + C_FOND + ";");
        VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);

        // ── Footer : progression ─────────────────────────────────────────────
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(370);
        progressBar.setPrefHeight(10);
        progressBar.setStyle("-fx-accent: " + C_VERT + ";");

        labelProgres = creerLabel("0 / 8 tâches effectuées", 14, false, C_SUBTIL);

        VBox footer = new VBox(6, progressBar, labelProgres);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(12, 20, 32, 20));

        Separator sep = new Separator();
        sep.setMaxWidth(370);

        // ── Racine ───────────────────────────────────────────────────────────
        VBox root = new VBox(16, header, titreListe, scroll, sep, footer);
        root.setStyle("-fx-background-color: " + C_FOND + ";");

        Scene scene = new Scene(root, 460, 820);
        stage.setTitle("MémoGuide – Lucien");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();

        demarrerHorloge();
    }

    // ── Horloge ──────────────────────────────────────────────────────────────

    private void demarrerHorloge() {
        Timeline horloge = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalTime now = LocalTime.now();
            labelHeure.setText(now.format(FMT));

            tacheManager.mettreAJour();
            rafraichirListeTaches(now);

            long validees = tacheManager.getNbValidees();
            int  total    = tacheManager.getNbTotal();
            progressBar.setProgress((double) validees / total);
            labelProgres.setText(validees + " / " + total + " tâches effectuées");
        }));
        horloge.setCycleCount(Timeline.INDEFINITE);
        horloge.play();
    }

    // ── Construction dynamique des cartes ─────────────────────────────────────

    private void rafraichirListeTaches(LocalTime now) {
        List<Tache> prochaines = tacheManager.getTachesProchaines4h();
        listeTachesBox.getChildren().clear();

        if (prochaines.isEmpty()) {
            Label vide = creerLabel("Aucune tâche dans les 4 prochaines heures 😊", 18, false, C_SUBTIL);
            vide.setWrapText(true);
            vide.setMaxWidth(400);
            listeTachesBox.getChildren().add(vide);
            return;
        }

        for (Tache t : prochaines) {
            listeTachesBox.getChildren().add(construireCarte(t, now));
        }
    }

    /**
     * Construit une carte pour une tâche.
     * – Active + non validée → bouton OUI coloré selon urgence
     * – Active + validée     → carte verte, bouton désactivé
     * – À venir              → carte grisée, bouton désactivé
     */
    private HBox construireCarte(Tache t, LocalTime now) {
        boolean active   = t.estActive(now);
        boolean validee  = t.isEstValidee();
        boolean aVenir   = !active;

        // ── Indicateur coloré à gauche ────────────────────────────────────
        String couleurIndicateur;
        if (aVenir)       couleurIndicateur = C_GRIS;
        else if (validee) couleurIndicateur = C_VERT;
        else {
            long min = java.time.temporal.ChronoUnit.MINUTES.between(now, t.getHeureReset());
            couleurIndicateur = min <= 15 ? C_ROUGE : min <= 45 ? C_ORANGE : C_VERT;
        }

        Region indicateur = new Region();
        indicateur.setPrefWidth(5);
        indicateur.setMinHeight(80);
        indicateur.setStyle("-fx-background-color: " + couleurIndicateur +
                "; -fx-background-radius: 4 0 0 4;");

        // ── Textes ────────────────────────────────────────────────────────
        Label nomLabel = creerLabel(t.getNom(), 20, true,
                aVenir ? C_SUBTIL : C_TEXTE);
        nomLabel.setWrapText(true);
        nomLabel.setMaxWidth(230);

        String plageStr = "🕐 " + t.getPlageHoraire();
        if (aVenir) plageStr += "  (à venir)";
        Label plageLabel = creerLabel(plageStr, 14, false, C_SUBTIL);

        String statutStr;
        if      (validee) statutStr = "✅ Fait !";
        else if (aVenir)  statutStr = "⏳ En attente";
        else {
            long min = java.time.temporal.ChronoUnit.MINUTES.between(now, t.getHeureReset());
            statutStr = min <= 15 ? "⚠️ Urgent ! " + min + " min restantes"
                    : min <= 45 ? "🕐 " + min + " min restantes"
                    :             "À faire";
        }
        Label statutLabel = creerLabel(statutStr, 14, false, couleurIndicateur);

        VBox textes = new VBox(5, nomLabel, plageLabel, statutLabel);
        textes.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textes, Priority.ALWAYS);

        // ── Bouton OUI ────────────────────────────────────────────────────
        Button btnOui = new Button(validee ? "✓" : "OUI");
        btnOui.setPrefSize(80, 70);
        btnOui.setStyle(
                "-fx-background-color: " + (validee || aVenir ? C_GRIS : couleurIndicateur) + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;"
        );
        btnOui.setDisable(validee || aVenir);

        btnOui.setOnAction(e -> {
            t.valider();
            // Animation pulse
            ScaleTransition pulse = new ScaleTransition(Duration.millis(120), btnOui);
            pulse.setFromX(1.0); pulse.setFromY(1.0);
            pulse.setToX(1.12);  pulse.setToY(1.12);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(2);
            pulse.play();
            // Rappel sonore FS5 si validée avec retard
            if (t.doitDeclenchemerRappel(LocalTime.now())) {
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        });

        // ── Assemblage ────────────────────────────────────────────────────
        HBox contenu = new HBox(16, textes, btnOui);
        contenu.setAlignment(Pos.CENTER_LEFT);
        contenu.setPadding(new Insets(16, 16, 16, 16));

        HBox carte = new HBox(indicateur, contenu);
        carte.setAlignment(Pos.CENTER_LEFT);
        carte.setStyle(
                "-fx-background-color: " + C_CARTE + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: " + couleurIndicateur + "22;" +  // bordure très légère
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 1;"
        );
        carte.setMaxWidth(Double.MAX_VALUE);

        return carte;
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

    public static void main(String[] args) { launch(args); }
}