package controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.example.utils.MyDatabase;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class HomeController {

    @FXML private Label lblBienvenue;
    @FXML private Label lblSousTitre;
    @FXML private Label lblEmoji;
    @FXML private Label lblTotalProduits;
    @FXML private Label lblTotalCommandes;
    @FXML private Label lblTotalStocks;
    @FXML private VBox timelineCommandes;
    @FXML private VBox timelineAlertes;   // ✅ Sera le chat IA
    @FXML private VBox timelineProduits;  // ✅ Sera les messages IA

    private static final String TEXTE_BIENVENUE = "Bienvenue, Fournisseur !";
    private static final String TEXTE_SOUS_TITRE = "Votre catalogue et vos commandes";
    private static final String WEATHER_API_KEY  = "c711cac8ab64b3ee5f16ef32e855562a";
    private static final String COHERE_API_KEY   = ""; // ✅ Collez votre clé ici
    private static final String VILLE = "Ariana,TN";
    private Connection conn;

    // Chat state
    private VBox chatMessagesBox = null;
    private TextField chatInput   = null;

    @FXML
    public void initialize() {
        try {
            conn = MyDatabase.getInstance().getConnection();
        } catch (Exception e) {
            System.err.println("❌ DB: " + e.getMessage());
        }

        animerTexteLettre(lblBienvenue, TEXTE_BIENVENUE, 0);
        animerTexteLettre(lblSousTitre, TEXTE_SOUS_TITRE, 1200);
        animerEmoji();

        chargerStats();
        chargerMeteo();
        construireChatIA();      // ✅ Chat IA à la place des alertes
        analyseAutomatique();    // ✅ Analyse auto au démarrage
    }

    // ═══════════════════════════════════════════════════════
    // ✅ CONSTRUCTION DU CHAT IA
    // ═══════════════════════════════════════════════════════

    private void construireChatIA() {
        // ── Zone messages (dans timelineAlertes) ──
        chatMessagesBox = new VBox(10);
        chatMessagesBox.setStyle("-fx-padding: 5 0;");

        ScrollPane scrollMessages = new ScrollPane(chatMessagesBox);
        scrollMessages.setFitToWidth(true);
        scrollMessages.setPrefHeight(280);
        scrollMessages.setStyle(
                "-fx-background-color: transparent; -fx-background: transparent;" +
                        "-fx-border-color: transparent;"
        );
        scrollMessages.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Message de bienvenue
        ajouterMessageIA("👋 Bonjour ! Je suis votre assistant AgriConnect.\n" +
                "Je peux analyser vos ventes, stocks et commandes.\n" +
                "Posez-moi une question ou cliquez sur une suggestion ! 🌾");

        timelineAlertes.getChildren().add(scrollMessages);

        // ── Zone saisie + bouton (dans timelineProduits) ──
        VBox inputZone = new VBox(10);
        inputZone.setStyle("-fx-padding: 5 0;");

        // Suggestions rapides
        HBox suggestions = new HBox(8);
        suggestions.setAlignment(Pos.CENTER_LEFT);

        String[] suggestionsTextes = {
                "📊 Analyser ventes",
                "⚠️ Stocks critiques",
                "💡 Recommandations"
        };

        for (String suggestion : suggestionsTextes) {
            Button btnSug = new Button(suggestion);
            btnSug.setStyle(
                    "-fx-background-color: #e8f5e9; -fx-text-fill: #4a7c3a;" +
                            "-fx-font-size: 11px; -fx-font-weight: bold;" +
                            "-fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 5 10;"
            );
            btnSug.setOnAction(e -> envoyerMessage(suggestion));
            suggestions.getChildren().add(btnSug);
        }

        // Input + bouton envoyer
        HBox inputRow = new HBox(8);
        inputRow.setAlignment(Pos.CENTER);

        chatInput = new TextField();
        chatInput.setPromptText("Posez votre question à l'IA...");
        chatInput.setStyle(
                "-fx-background-color: #f9f9f9; -fx-border-color: #4a7c3a;" +
                        "-fx-border-radius: 20; -fx-background-radius: 20;" +
                        "-fx-padding: 8 15; -fx-font-size: 13px;"
        );
        HBox.setHgrow(chatInput, Priority.ALWAYS);
        chatInput.setOnAction(e -> envoyerMessage(chatInput.getText()));

        Button btnEnvoyer = new Button("📤");
        btnEnvoyer.setStyle(
                "-fx-background-color: linear-gradient(to right, #4a7c3a, #66bb6a);" +
                        "-fx-text-fill: white; -fx-font-size: 16px;" +
                        "-fx-background-radius: 50; -fx-pref-width: 40; -fx-pref-height: 40;" +
                        "-fx-cursor: hand;"
        );
        btnEnvoyer.setOnAction(e -> envoyerMessage(chatInput.getText()));

        inputRow.getChildren().addAll(chatInput, btnEnvoyer);
        inputZone.getChildren().addAll(suggestions, inputRow);

        timelineProduits.getChildren().add(inputZone);
    }

    // ═══════════════════════════════════════════════════════
    // ✅ ENVOI MESSAGE
    // ═══════════════════════════════════════════════════════

    private void envoyerMessage(String texte) {
        if (texte == null || texte.trim().isEmpty()) return;

        // Afficher message utilisateur
        ajouterMessageUtilisateur(texte);
        if (chatInput != null) chatInput.clear();

        // Afficher "en train d'écrire..."
        Label typing = new Label("🤖 En train d'analyser...");
        typing.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: #888; -fx-font-style: italic;" +
                        "-fx-padding: 5 10;"
        );
        chatMessagesBox.getChildren().add(typing);

        // Récupérer données BD pour le contexte
        String contexteBD = recupererContexteBD();

        // Appel Cohere dans thread séparé
        Thread thread = new Thread(() -> {
            try {
                String prompt = buildPrompt(texte, contexteBD);
                String reponse = appelCohere(prompt);

                Platform.runLater(() -> {
                    chatMessagesBox.getChildren().remove(typing);
                    ajouterMessageIA(reponse);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    chatMessagesBox.getChildren().remove(typing);
                    ajouterMessageIA("❌ Erreur : " + e.getMessage());
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    // ═══════════════════════════════════════════════════════
    // ✅ ANALYSE AUTOMATIQUE AU DÉMARRAGE
    // ═══════════════════════════════════════════════════════

    private void analyseAutomatique() {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(2000); // Attendre chargement
                String contexteBD = recupererContexteBD();
                String prompt = buildPrompt(
                        "Fais une analyse rapide de mes données agricoles et donne 2-3 recommandations courtes.",
                        contexteBD
                );
                String reponse = appelCohere(prompt);

                Platform.runLater(() -> ajouterMessageIA("📊 Analyse automatique :\n" + reponse));
            } catch (Exception e) {
                System.err.println("❌ Analyse auto: " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    // ═══════════════════════════════════════════════════════
    // ✅ CONTEXTE BASE DE DONNÉES
    // ═══════════════════════════════════════════════════════

    private String recupererContexteBD() {
        StringBuilder ctx = new StringBuilder();
        if (conn == null) return "Base de données non connectée.";

        try {
            // Nombre produits
            ResultSet r1 = conn.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM produit WHERE actif=1");
            if (r1.next()) ctx.append("Produits actifs: ").append(r1.getInt(1)).append("\n");

            // Nombre commandes
            ResultSet r2 = conn.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM commande");
            if (r2.next()) ctx.append("Total commandes: ").append(r2.getInt(1)).append("\n");

            // Commandes par statut
            ResultSet r3 = conn.createStatement()
                    .executeQuery("SELECT statut, COUNT(*) as nb FROM commande GROUP BY statut");
            ctx.append("Commandes par statut:\n");
            while (r3.next())
                ctx.append("  - ").append(r3.getString("statut"))
                        .append(": ").append(r3.getInt("nb")).append("\n");

            // Top produits vendus
            ResultSet r4 = conn.createStatement().executeQuery("""
                SELECT p.nom, SUM(c.quantite_commandee) as total
                FROM commande c
                JOIN produit p ON p.id_produit = c.id_produit
                GROUP BY p.nom
                ORDER BY total DESC
                LIMIT 3""");
            ctx.append("Top produits vendus:\n");
            while (r4.next())
                ctx.append("  - ").append(r4.getString("nom"))
                        .append(": ").append(r4.getInt("total")).append(" unités\n");

            // Stocks critiques
            ResultSet r5 = conn.createStatement().executeQuery("""
                SELECT p.nom, s.quantite, s.seuilAlert
                FROM stock s
                JOIN produit p ON p.id_produit = s.idProduit
                WHERE s.quantite <= s.seuilAlert""");
            ctx.append("Stocks critiques:\n");
            boolean hasStock = false;
            while (r5.next()) {
                ctx.append("  - ").append(r5.getString("nom"))
                        .append(": ").append(r5.getInt("quantite"))
                        .append("/").append(r5.getInt("seuilAlert")).append("\n");
                hasStock = true;
            }
            if (!hasStock) ctx.append("  Aucun stock critique\n");

        } catch (SQLException e) {
            ctx.append("Erreur lecture BD: ").append(e.getMessage());
        }

        return ctx.toString();
    }

    // ═══════════════════════════════════════════════════════
    // ✅ BUILD PROMPT COHERE
    // ═══════════════════════════════════════════════════════

    private String buildPrompt(String question, String contexte) {
        return "Tu es un assistant agricole intelligent pour AgriConnect, " +
                "une plateforme de gestion pour fournisseurs agricoles en Tunisie. " +
                "Réponds en français, de façon courte et pratique (max 4 lignes). " +
                "Voici les données actuelles de l'application:\n\n" +
                contexte + "\n" +
                "Question: " + question;
    }

    // ═══════════════════════════════════════════════════════
    // ✅ APPEL API COHERE
    // ═══════════════════════════════════════════════════════

    private String appelCohere(String prompt) throws Exception {
        URL url = new URL("https://api.cohere.com/v2/chat");  // ✅ Nouvel endpoint
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + COHERE_API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        String body = "{"
                + "\"model\": \"command-a-03-2025\","
                + "\"messages\": [{\"role\": \"user\", \"content\": " + toJson(prompt) + "}]"
                + "}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        // Vérifier le code de réponse
        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();

        if (responseCode != 200) {
            throw new Exception("API Error " + responseCode + ": " + response.toString());
        }

        // Parse le nouveau format v2 : "text":"..."
        String json = response.toString();
        String text = parseString(json, "\"text\":\"", "\"");
        return text.isEmpty() ? "Réponse non disponible." : text.replace("\\n", "\n");
    }

    private String toJson(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }

    // ═══════════════════════════════════════════════════════
    // ✅ AFFICHAGE MESSAGES
    // ═══════════════════════════════════════════════════════

    private void ajouterMessageIA(String texte) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        Label msg = new Label("🤖 " + texte);
        msg.setWrapText(true);
        msg.setMaxWidth(300);
        msg.setStyle(
                "-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32;" +
                        "-fx-padding: 10 14; -fx-background-radius: 0 12 12 12;" +
                        "-fx-font-size: 12px; -fx-border-color: #c8e6c9;" +
                        "-fx-border-radius: 0 12 12 12; -fx-border-width: 1;"
        );

        row.getChildren().add(msg);

        FadeTransition fade = new FadeTransition(Duration.millis(400), row);
        fade.setFromValue(0); fade.setToValue(1); fade.play();

        chatMessagesBox.getChildren().add(row);
    }

    private void ajouterMessageUtilisateur(String texte) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);

        Label msg = new Label(texte + " 👤");
        msg.setWrapText(true);
        msg.setMaxWidth(280);
        msg.setStyle(
                "-fx-background-color: #4a7c3a; -fx-text-fill: white;" +
                        "-fx-padding: 10 14; -fx-background-radius: 12 0 12 12;" +
                        "-fx-font-size: 12px;"
        );

        row.getChildren().add(msg);
        chatMessagesBox.getChildren().add(row);
    }

    // ═══════════════════════════════════════════════════════
    // MÉTÉO
    // ═══════════════════════════════════════════════════════

    private void chargerMeteo() {
        Label lblChargement = new Label("⏳ Chargement météo...");
        lblChargement.setStyle("-fx-font-size: 13px; -fx-text-fill: #888;");
        timelineCommandes.getChildren().add(lblChargement);

        Thread thread = new Thread(() -> {
            try {
                String urlStr = "https://api.openweathermap.org/data/2.5/weather"
                        + "?q=" + VILLE
                        + "&appid=" + WEATHER_API_KEY
                        + "&units=metric&lang=fr";

                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                String json = response.toString();
                double temp       = parseDouble(json, "\"temp\":");
                double feelsLike  = parseDouble(json, "\"feels_like\":");
                int humidity      = parseInt(json, "\"humidity\":");
                double windSpeed  = parseDouble(json, "\"speed\":");
                String description = parseString(json, "\"description\":\"", "\"");
                String cityName   = parseString(json, "\"name\":\"", "\"");
                String weatherEmoji = getWeatherEmoji(description);

                Platform.runLater(() -> {
                    timelineCommandes.getChildren().clear();

                    VBox meteoCard = new VBox(15);
                    meteoCard.setStyle(
                            "-fx-background-color: linear-gradient(to bottom right, #4a7c3a, #66bb6a);" +
                                    "-fx-background-radius: 14; -fx-padding: 20;");
                    meteoCard.setMaxWidth(Double.MAX_VALUE);

                    Label lblVille = new Label("📍 " + cityName + ", Tunisie");
                    lblVille.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e8f5e9;");

                    HBox tempBox = new HBox(15);
                    tempBox.setAlignment(Pos.CENTER_LEFT);
                    Label lblEmojiMeteo = new Label(weatherEmoji);
                    lblEmojiMeteo.setStyle("-fx-font-size: 52px;");
                    VBox tempInfo = new VBox(4);
                    Label lblTemp = new Label(String.format("%.0f°C", temp));
                    lblTemp.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: white;");
                    Label lblDesc = new Label(capitalize(description));
                    lblDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #e8f5e9; -fx-font-style: italic;");
                    tempInfo.getChildren().addAll(lblTemp, lblDesc);
                    tempBox.getChildren().addAll(lblEmojiMeteo, tempInfo);

                    Region sep = new Region();
                    sep.setPrefHeight(1);
                    sep.setStyle("-fx-background-color: rgba(255,255,255,0.3);");

                    HBox detailsBox = new HBox(12);
                    detailsBox.setAlignment(Pos.CENTER_LEFT);
                    detailsBox.getChildren().addAll(
                            creerDetailMeteo("🌡️", "Ressenti", String.format("%.0f°C", feelsLike)),
                            creerDetailMeteo("💧", "Humidité", humidity + "%"),
                            creerDetailMeteo("🌬️", "Vent", String.format("%.0f km/h", windSpeed * 3.6))
                    );

                    Label lblConseil = new Label("🌾 " + getConseilAgricole(description, temp));
                    lblConseil.setStyle(
                            "-fx-font-size: 12px; -fx-text-fill: #2e7d32;" +
                                    "-fx-background-color: rgba(255,255,255,0.9);" +
                                    "-fx-padding: 10 14; -fx-background-radius: 8;" +
                                    "-fx-font-weight: bold;");
                    lblConseil.setWrapText(true);
                    lblConseil.setMaxWidth(Double.MAX_VALUE);

                    meteoCard.getChildren().addAll(lblVille, tempBox, sep, detailsBox, lblConseil);

                    meteoCard.setOpacity(0);
                    FadeTransition fade = new FadeTransition(Duration.millis(700), meteoCard);
                    fade.setFromValue(0); fade.setToValue(1); fade.play();
                    timelineCommandes.getChildren().add(meteoCard);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    timelineCommandes.getChildren().clear();
                    Label erreur = new Label("❌ Météo indisponible");
                    erreur.setStyle("-fx-font-size: 12px; -fx-text-fill: #d9534f;");
                    timelineCommandes.getChildren().add(erreur);
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private VBox creerDetailMeteo(String emoji, String label, String valeur) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-padding: 10 15; -fx-background-radius: 10;");
        Label lblEmoji = new Label(emoji); lblEmoji.setStyle("-fx-font-size: 18px;");
        Label lblValeur = new Label(valeur); lblValeur.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label lblLabel = new Label(label); lblLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #e8f5e9;");
        box.getChildren().addAll(lblEmoji, lblValeur, lblLabel);
        return box;
    }

    private String getWeatherEmoji(String d) {
        if (d == null) return "🌤️";
        d = d.toLowerCase();
        if (d.contains("pluie") || d.contains("rain"))    return "🌧️";
        if (d.contains("nuage") || d.contains("cloud"))   return "☁️";
        if (d.contains("orage") || d.contains("thunder")) return "⛈️";
        if (d.contains("neige") || d.contains("snow"))    return "❄️";
        if (d.contains("brume") || d.contains("fog"))     return "🌫️";
        if (d.contains("clear") || d.contains("soleil"))  return "☀️";
        return "🌤️";
    }

    private String getConseilAgricole(String d, double temp) {
        if (d.contains("pluie") || d.contains("rain"))    return "Pluie prévue — Évitez les traitements.";
        if (d.contains("orage") || d.contains("thunder")) return "Orage prévu — Protégez vos cultures.";
        if (temp > 35) return "Forte chaleur — Irriguez tôt le matin.";
        if (temp < 5)  return "Températures basses — Protégez les cultures.";
        if (d.contains("clear") || d.contains("soleil"))  return "Beau temps — Idéal pour la récolte !";
        return "Conditions correctes — Bonne journée agricole.";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    // ═══════════════════════════════════════════════════════
    // PARSE JSON
    // ═══════════════════════════════════════════════════════

    private double parseDouble(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx == -1) return 0;
            String sub = json.substring(idx + key.length()).trim();
            int end = sub.indexOf(','); if (end == -1) end = sub.indexOf('}');
            return Double.parseDouble(sub.substring(0, end).trim());
        } catch (Exception e) { return 0; }
    }

    private int parseInt(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx == -1) return 0;
            String sub = json.substring(idx + key.length()).trim();
            int end = sub.indexOf(','); if (end == -1) end = sub.indexOf('}');
            return Integer.parseInt(sub.substring(0, end).trim());
        } catch (Exception e) { return 0; }
    }

    private String parseString(String json, String startKey, String endKey) {
        try {
            int idx = json.indexOf(startKey);
            if (idx == -1) return "";
            int start = idx + startKey.length();
            int end = json.indexOf(endKey, start);
            return json.substring(start, end);
        } catch (Exception e) { return ""; }
    }

    // ═══════════════════════════════════════════════════════
    // STATS
    // ═══════════════════════════════════════════════════════

    private void chargerStats() {
        if (conn == null) return;
        try {
            Statement st = conn.createStatement();
            ResultSet r1 = st.executeQuery("SELECT COUNT(*) FROM produit WHERE actif=1");
            if (r1.next()) lblTotalProduits.setText(String.valueOf(r1.getInt(1)));
            ResultSet r2 = st.executeQuery("SELECT COUNT(*) FROM commande");
            if (r2.next()) lblTotalCommandes.setText(String.valueOf(r2.getInt(1)));
            ResultSet r3 = st.executeQuery("SELECT COUNT(*) FROM stock");
            if (r3.next()) lblTotalStocks.setText(String.valueOf(r3.getInt(1)));
        } catch (SQLException e) {
            System.err.println("❌ Stats: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    // ANIMATIONS
    // ═══════════════════════════════════════════════════════

    private void animerTexteLettre(Label label, String texte, int delayMs) {
        label.setText(""); label.setOpacity(1);
        Timeline timeline = new Timeline();
        for (int i = 0; i <= texte.length(); i++) {
            final int index = i;
            timeline.getKeyFrames().add(new KeyFrame(
                    Duration.millis(delayMs + index * 60),
                    e -> label.setText(texte.substring(0, index))
            ));
        }
        timeline.getKeyFrames().add(new KeyFrame(
                Duration.millis(delayMs + texte.length() * 60 + 200), e -> label.setText(texte + " |")));
        timeline.getKeyFrames().add(new KeyFrame(
                Duration.millis(delayMs + texte.length() * 60 + 600), e -> label.setText(texte)));
        timeline.setCycleCount(1); timeline.play();
    }

    private void animerEmoji() {
        if (lblEmoji == null) return;
        ScaleTransition scale = new ScaleTransition(Duration.millis(1000), lblEmoji);
        scale.setFromX(0.8); scale.setFromY(0.8); scale.setToX(1.2); scale.setToY(1.2);
        scale.setAutoReverse(true); scale.setCycleCount(Animation.INDEFINITE); scale.play();
        RotateTransition rotate = new RotateTransition(Duration.millis(3000), lblEmoji);
        rotate.setFromAngle(-10); rotate.setToAngle(10);
        rotate.setAutoReverse(true); rotate.setCycleCount(Animation.INDEFINITE); rotate.play();
    }

    // ═══════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════

    @FXML private void goToProduits()   { openView("/views/produit.fxml",             "Gestion des Produits"); }
    @FXML private void goToStock()      { openView("/views/stockview.fxml",            "Gestion du Stock"); }
    @FXML private void goToCommandes()  { openView("/views/commande.fxml",             "Gestion des Commandes"); }
    @FXML private void voirHistorique() { openView("/views/historique-commandes.fxml", "Historique des Commandes"); }

    private void openView(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage newStage = new Stage();
            newStage.setScene(new Scene(root, 1600, 900));
            newStage.setTitle(title + " - AgriConnect");
            newStage.setMaximized(true);
            closeCurrentWindow();
            newStage.show();
        } catch (IOException e) { showError("Erreur", e.getMessage()); }
    }

    private void closeCurrentWindow() {
        for (Window window : Stage.getWindows()) {
            if (window instanceof Stage s && s.isShowing() && s.getScene() != null) {
                s.close(); break;
            }
        }
    }

    @FXML private void handleDeconnexion() {
        for (Window window : Stage.getWindows())
            if (window instanceof Stage) ((Stage) window).close();
    }

    @FXML private void showProfile() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Mon Profil"); a.setHeaderText("Profil Fournisseur");
        a.setContentText("Fonctionnalité en cours de développement..."); a.showAndWait();
    }

    @FXML private void onMouseEntered(javafx.scene.input.MouseEvent event) {
        if (event.getSource() instanceof Button btn) {
            String s = btn.getStyle();
            if (!s.contains("rgba(255,255,255,0.25)"))
                btn.setStyle(s + "-fx-background-color: rgba(255,255,255,0.2);");
        }
    }

    @FXML private void onMouseExited(javafx.scene.input.MouseEvent event) {
        if (event.getSource() instanceof Button btn)
            btn.setStyle(btn.getStyle().replace("-fx-background-color: rgba(255,255,255,0.2);", ""));
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText("Erreur"); a.setContentText(msg); a.showAndWait();
    }
}