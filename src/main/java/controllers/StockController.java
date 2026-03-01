package controllers;

import entities.Produit;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.utils.MyDatabase;
import services.BrevoEmailService;
import services.GeminiService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

public class StockController {

    @FXML private ComboBox<Produit> cbProduit;
    @FXML private TextField tfQuantite, tfSeuilAlerte;
    @FXML private Button btnRetourAccueil;
    @FXML private FlowPane cardsContainer;
    @FXML private Label lblStatus;

    private Connection conn;
    private StockData currentSelection = null;
    private VBox selectedCard = null;
    private ObservableList<StockData> stockList = FXCollections.observableArrayList();

    // ═══════════════════════════════════════════════════════
    // STOCK DATA
    // ═══════════════════════════════════════════════════════

    public static class StockData {
        public final IntegerProperty idStock = new SimpleIntegerProperty();
        public final IntegerProperty idProduit = new SimpleIntegerProperty();
        public final StringProperty produit = new SimpleStringProperty();
        public final IntegerProperty quantite = new SimpleIntegerProperty();
        public final IntegerProperty seuilAlerte = new SimpleIntegerProperty();
        public final StringProperty imagePath = new SimpleStringProperty(); // ✅

        public StockData(int idStock, int idProduit, String produit, int quantite, int seuilAlerte, String imagePath) {
            this.idStock.set(idStock);
            this.idProduit.set(idProduit);
            this.produit.set(produit);
            this.quantite.set(quantite);
            this.seuilAlerte.set(seuilAlerte);
            this.imagePath.set(imagePath != null ? imagePath : "");
        }
    }

    @FXML
    public void initialize() throws SQLException {
        conn = MyDatabase.getInstance().getConnection();
        chargerProduits();
        chargerStock();
    }

    // ═══════════════════════════════════════════════════════
    // CARDS AVEC IMAGE
    // ═══════════════════════════════════════════════════════

    private VBox createCard(StockData s) {
        VBox card = new VBox(10);
        card.setPrefWidth(210);
        card.setMaxWidth(210);

        int qte = s.quantite.get();
        int seuil = s.seuilAlerte.get();
        String statut, couleurBadge, emoji;
        if (qte <= 0) {
            statut = "Rupture"; couleurBadge = "#d9534f"; emoji = "🔴";
        } else if (qte <= seuil) {
            statut = "Stock bas"; couleurBadge = "#f0ad4e"; emoji = "🟠";
        } else {
            statut = "En stock"; couleurBadge = "#5a8c4a"; emoji = "🟢";
        }

        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12; -fx-border-radius: 12;" +
                        "-fx-border-color: " + couleurBadge + "; -fx-border-width: 2;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);" +
                        "-fx-padding: 15; -fx-cursor: hand;"
        );

        // ✅ IMAGE CIRCULAIRE
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(90, 90);
        imageContainer.setMaxSize(90, 90);

        Circle bgCircle = new Circle(45);
        bgCircle.setStyle("-fx-fill: #e8f5e9; -fx-stroke: " + couleurBadge + "; -fx-stroke-width: 2.5;");

        ImageView iv = new ImageView();
        iv.setFitWidth(86);
        iv.setFitHeight(86);
        iv.setPreserveRatio(false);
        Circle clip = new Circle(43, 43, 43);
        iv.setClip(clip);

        Label iconFallback = new Label("🌿");
        iconFallback.setStyle("-fx-font-size: 26px;");

        // Badge emoji statut
        Label lblEmoji = new Label(emoji);
        lblEmoji.setStyle("-fx-font-size: 16px;");
        StackPane.setAlignment(lblEmoji, Pos.TOP_RIGHT);

        // Charger image produit
        String imgPath = s.imagePath.get();
        if (imgPath != null && !imgPath.isEmpty()) {
            File f = new File(imgPath);
            if (f.exists()) {
                try {
                    Image img = new Image(f.toURI().toString(), 86, 86, false, true);
                    iv.setImage(img);
                    iconFallback.setVisible(false);
                } catch (Exception e) {
                    iv.setImage(null);
                }
            }
        }

        imageContainer.getChildren().addAll(bgCircle, iv, iconFallback, lblEmoji);
        HBox imgBox = new HBox(imageContainer);
        imgBox.setStyle("-fx-alignment: center;");

        // Nom produit
        Label lblNom = new Label(s.produit.get());
        lblNom.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2d2d2d; -fx-wrap-text: true;");
        lblNom.setMaxWidth(185);
        lblNom.setWrapText(true);

        // Badge statut
        Label lblStatut = new Label(statut);
        lblStatut.setStyle(
                "-fx-background-color: " + couleurBadge + "; -fx-text-fill: white;" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-padding: 3 10; -fx-background-radius: 10;"
        );

        Separator sep = new Separator();

        HBox qteBox = new HBox(8);
        Label qteLabel = new Label("Quantité :");
        qteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        Label qteVal = new Label(String.valueOf(qte));
        qteVal.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + couleurBadge + ";");
        qteBox.getChildren().addAll(qteLabel, qteVal);

        HBox seuilBox = new HBox(8);
        Label seuilLabel = new Label("Seuil alerte :");
        seuilLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        Label seuilVal = new Label(String.valueOf(seuil));
        seuilVal.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #888;");
        seuilBox.getChildren().addAll(seuilLabel, seuilVal);

        card.getChildren().addAll(imgBox, lblNom, lblStatut, sep, qteBox, seuilBox);

        card.setOnMouseClicked(e -> selectCard(s, card, couleurBadge));
        card.setOnMouseEntered(e -> {
            if (currentSelection == null || currentSelection.idStock.get() != s.idStock.get()) {
                card.setStyle(
                        "-fx-background-color: #f9fdf9;" +
                                "-fx-background-radius: 12; -fx-border-radius: 12;" +
                                "-fx-border-color: " + couleurBadge + "; -fx-border-width: 2.5;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 14, 0, 0, 5);" +
                                "-fx-padding: 15; -fx-cursor: hand;"
                );
            }
        });
        card.setOnMouseExited(e -> {
            if (currentSelection == null || currentSelection.idStock.get() != s.idStock.get()) {
                card.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-background-radius: 12; -fx-border-radius: 12;" +
                                "-fx-border-color: " + couleurBadge + "; -fx-border-width: 2;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);" +
                                "-fx-padding: 15; -fx-cursor: hand;"
                );
            }
        });

        return card;
    }

    private void selectCard(StockData s, VBox card, String couleurBadge) {
        if (selectedCard != null) {
            String oldCouleur = currentSelection != null ?
                    (currentSelection.quantite.get() <= 0 ? "#d9534f" :
                            currentSelection.quantite.get() <= currentSelection.seuilAlerte.get() ? "#f0ad4e" : "#5a8c4a")
                    : "#5a8c4a";
            selectedCard.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 12; -fx-border-radius: 12;" +
                            "-fx-border-color: " + oldCouleur + "; -fx-border-width: 2;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);" +
                            "-fx-padding: 15; -fx-cursor: hand;"
            );
        }
        card.setStyle(
                "-fx-background-color: #e8f5e9;" +
                        "-fx-background-radius: 12; -fx-border-radius: 12;" +
                        "-fx-border-color: " + couleurBadge + "; -fx-border-width: 3;" +
                        "-fx-effect: dropshadow(gaussian, rgba(74,124,58,0.35), 14, 0, 0, 5);" +
                        "-fx-padding: 15; -fx-cursor: hand;"
        );
        selectedCard = card;
        currentSelection = s;
        cbProduit.setValue(getProduitById(s.idProduit.get()));
        tfQuantite.setText(String.valueOf(s.quantite.get()));
        tfSeuilAlerte.setText(String.valueOf(s.seuilAlerte.get()));
    }

    // ═══════════════════════════════════════════════════════
    // 🤖 ASSISTANT IA
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleAssistantIA() {
        StringBuilder donnees = new StringBuilder();
        for (StockData s : stockList) {
            String etat = s.quantite.get() <= 0 ? "RUPTURE" :
                    s.quantite.get() <= s.seuilAlerte.get() ? "BAS" : "OK";
            donnees.append(String.format(
                    "- %s : %d unités (seuil: %d) [%s]\n",
                    s.produit.get(), s.quantite.get(), s.seuilAlerte.get(), etat
            ));
        }

        Stage iaStage = new Stage();
        iaStage.setTitle("🤖 Assistant IA - Analyse Stock AgriConnect");

        VBox root = new VBox(15);
        root.setStyle("-fx-background-color: #f5f5dc; -fx-padding: 25;");

        HBox titreBox = new HBox(10);
        titreBox.setAlignment(Pos.CENTER_LEFT);
        titreBox.setStyle(
                "-fx-background-color: white; -fx-padding: 15;" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);"
        );
        Label titre = new Label("🤖 Assistant Agricole IA");
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4a7c3a;");
        Label sousTitre = new Label("Propulsé par Cohere AI");
        sousTitre.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        VBox titreVBox = new VBox(2, titre, sousTitre);
        titreBox.getChildren().add(titreVBox);

        TextArea chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setPrefHeight(350);
        chatArea.setWrapText(true);
        chatArea.setStyle(
                "-fx-background-color: white; -fx-border-color: #c8e6c9;" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-font-size: 13px; -fx-padding: 10;"
        );
        chatArea.setText("🌾 Bonjour ! Je suis votre assistant agricole IA.\n" +
                "Analyse de votre stock en cours...\n\n");

        HBox inputRow = new HBox(10);
        TextField tfQuestion = new TextField();
        tfQuestion.setPromptText("Posez une question sur votre stock...");
        tfQuestion.setPrefHeight(42);
        HBox.setHgrow(tfQuestion, Priority.ALWAYS);
        tfQuestion.setStyle(
                "-fx-background-color: white; -fx-border-color: #4a7c3a;" +
                        "-fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 13px;"
        );

        Button btnEnvoyer = new Button("📤 Envoyer");
        btnEnvoyer.setPrefHeight(42);
        btnEnvoyer.setStyle(
                "-fx-background-color: #5a8c4a; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 8;" +
                        "-fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 0 15;"
        );
        inputRow.getChildren().addAll(tfQuestion, btnEnvoyer);

        HBox boutons = new HBox(10);
        boutons.setAlignment(Pos.CENTER_LEFT);

        Button btnAnalyser = new Button("📊 Analyser mon stock");
        btnAnalyser.setPrefHeight(40);
        btnAnalyser.setStyle(
                "-fx-background-color: #5bc0de; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 8;" +
                        "-fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 0 15;"
        );

        Button btnConseils = new Button("💡 Conseils agricoles");
        btnConseils.setPrefHeight(40);
        btnConseils.setStyle(
                "-fx-background-color: #f0ad4e; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 8;" +
                        "-fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 0 15;"
        );

        Button btnFermer = new Button("✕ Fermer");
        btnFermer.setPrefHeight(40);
        btnFermer.setStyle(
                "-fx-background-color: #d9534f; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 8;" +
                        "-fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 0 15;"
        );
        btnFermer.setOnAction(e -> iaStage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        boutons.getChildren().addAll(btnAnalyser, btnConseils, spacer, btnFermer);

        Runnable envoyerQuestion = () -> {
            String question = tfQuestion.getText().trim();
            if (question.isEmpty()) return;
            chatArea.appendText("─────────────────────\n");
            chatArea.appendText("🧑 Vous : " + question + "\n\n");
            chatArea.appendText("🤖 IA : ⏳ Analyse en cours...\n");
            tfQuestion.clear();
            new Thread(() -> {
                String reponse = GeminiService.poserQuestion(
                        "Tu es un assistant agricole expert. " +
                                "Voici le stock actuel:\n" + donnees +
                                "\nQuestion: " + question +
                                "\nRéponds en français, de façon concise et utile."
                );
                javafx.application.Platform.runLater(() -> {
                    chatArea.setText(chatArea.getText().replace(
                            "🤖 IA : ⏳ Analyse en cours...\n",
                            "🤖 IA : " + reponse + "\n\n"
                    ));
                    chatArea.setScrollTop(Double.MAX_VALUE);
                });
            }).start();
        };

        btnEnvoyer.setOnAction(e -> envoyerQuestion.run());
        tfQuestion.setOnAction(e -> envoyerQuestion.run());

        btnAnalyser.setOnAction(e -> {
            chatArea.appendText("─────────────────────\n");
            chatArea.appendText("📊 Analyse complète demandée...\n\n");
            chatArea.appendText("🤖 IA : ⏳ Analyse en cours...\n");
            new Thread(() -> {
                String reponse = GeminiService.analyserStock(donnees.toString());
                javafx.application.Platform.runLater(() -> {
                    chatArea.setText(chatArea.getText().replace(
                            "🤖 IA : ⏳ Analyse en cours...\n",
                            "🤖 IA : " + reponse + "\n\n"
                    ));
                    chatArea.setScrollTop(Double.MAX_VALUE);
                });
            }).start();
        });

        btnConseils.setOnAction(e -> {
            chatArea.appendText("─────────────────────\n");
            chatArea.appendText("💡 Demande de conseils...\n\n");
            chatArea.appendText("🤖 IA : ⏳ Préparation des conseils...\n");
            new Thread(() -> {
                String reponse = GeminiService.poserQuestion(
                        "Tu es un expert en agriculture. " +
                                "Voici le stock actuel:\n" + donnees +
                                "\nDonne 3 conseils pratiques pour optimiser " +
                                "la gestion de ce stock agricole. Réponds en français."
                );
                javafx.application.Platform.runLater(() -> {
                    chatArea.setText(chatArea.getText().replace(
                            "🤖 IA : ⏳ Préparation des conseils...\n",
                            "🤖 IA : " + reponse + "\n\n"
                    ));
                    chatArea.setScrollTop(Double.MAX_VALUE);
                });
            }).start();
        });

        root.getChildren().addAll(titreBox, chatArea, inputRow, boutons);
        iaStage.setScene(new Scene(root, 580, 580));
        iaStage.setResizable(false);
        iaStage.show();

        new Thread(() -> {
            String reponse = GeminiService.analyserStock(donnees.toString());
            javafx.application.Platform.runLater(() -> {
                chatArea.appendText("🤖 IA : " + reponse + "\n\n");
                chatArea.setScrollTop(Double.MAX_VALUE);
            });
        }).start();
    }

    // ═══════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleRetourAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/home.fxml"));
            Parent root = loader.load();
            Stage homeStage = new Stage();
            homeStage.setScene(new Scene(root, 1920, 1080));
            homeStage.setTitle("Accueil - AgriConnect");
            homeStage.setMaximized(true);
            Stage currentStage = (Stage) tfQuantite.getScene().getWindow();
            currentStage.close();
            homeStage.show();
        } catch (IOException e) {
            showError("Erreur de navigation : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    // PRODUITS
    // ═══════════════════════════════════════════════════════

    private void chargerProduits() {
        ObservableList<Produit> produits = FXCollections.observableArrayList();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM produit WHERE actif=1")) {
            while (rs.next()) {
                produits.add(new Produit(
                        rs.getLong("id_produit"),
                        rs.getString("nom"),
                        rs.getString("description"),
                        rs.getDouble("prix"),
                        rs.getString("categorie"),
                        rs.getBoolean("actif")
                ));
            }
        } catch (SQLException e) {
            System.err.println("❌ SQL Produits: " + e.getMessage());
        }
        cbProduit.setItems(produits);
        cbProduit.setCellFactory(p -> new ListCell<>() {
            @Override protected void updateItem(Produit item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });
        cbProduit.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Produit item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Sélectionner un produit" : item.getNom());
            }
        });
    }

    private Produit getProduitById(int id) {
        for (Produit p : cbProduit.getItems())
            if (p.getIdProduit() == id) return p;
        return null;
    }

    // ═══════════════════════════════════════════════════════
    // CRUD
    // ═══════════════════════════════════════════════════════

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();
        if (cbProduit.getValue() == null) errors.append("• Le produit est obligatoire\n");
        if (tfQuantite.getText().trim().isEmpty()) {
            errors.append("• La quantité est obligatoire\n");
        } else {
            try {
                int q = Integer.parseInt(tfQuantite.getText().trim());
                if (q <= 0) errors.append("• La quantité doit être > 0\n");
            } catch (NumberFormatException e) {
                errors.append("• La quantité doit être un entier\n");
            }
        }
        if (tfSeuilAlerte.getText().trim().isEmpty()) {
            errors.append("• Le seuil est obligatoire\n");
        } else {
            try {
                int s = Integer.parseInt(tfSeuilAlerte.getText().trim());
                if (s < 0) errors.append("• Le seuil doit être ≥ 0\n");
            } catch (NumberFormatException e) {
                errors.append("• Le seuil doit être un entier\n");
            }
        }
        if (errors.length() > 0) {
            showErrorAlert("Champs invalides", errors.toString());
            return false;
        }
        return true;
    }

    @FXML
    private void handleAjouter() {
        if (!validateInput()) return;
        try {
            Produit produit = cbProduit.getValue();
            int quantite = Integer.parseInt(tfQuantite.getText().trim());
            int seuil = Integer.parseInt(tfSeuilAlerte.getText().trim());
            try (PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO stock (quantite, disponible, seuilAlert, idProduit) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                pst.setInt(1, quantite);
                pst.setInt(2, quantite);
                pst.setInt(3, seuil);
                pst.setLong(4, produit.getIdProduit());
                pst.executeUpdate();
                ResultSet rs = pst.getGeneratedKeys();
                int id = rs.next() ? rs.getInt(1) : 0;
                showSuccessAlert("Stock ajouté !", "ID: " + id);
                clearInputs();
                chargerStock();
                for (StockData s : stockList) {
                    if (s.quantite.get() <= s.seuilAlerte.get() && s.quantite.get() > 0) {
                        BrevoEmailService.envoyerAlerteStock(
                                "ton_email_admin@gmail.com",
                                s.produit.get(),
                                s.quantite.get(),
                                s.seuilAlerte.get()
                        );
                    }
                }
            }
        } catch (Exception e) {
            showErrorAlert("Erreur ajout", e.getMessage());
        }
    }

    @FXML
    private void handleModifier() {
        if (currentSelection == null) {
            showWarningAlert("Aucune sélection", "Veuillez sélectionner une card.");
            return;
        }
        if (!validateInput()) return;
        try {
            int quantite = Integer.parseInt(tfQuantite.getText().trim());
            int seuil = Integer.parseInt(tfSeuilAlerte.getText().trim());
            try (PreparedStatement pst = conn.prepareStatement(
                    "UPDATE stock SET quantite=?, seuilAlert=?, idProduit=? WHERE idStock=?")) {
                pst.setInt(1, quantite);
                pst.setInt(2, seuil);
                pst.setInt(3, (int) cbProduit.getValue().getIdProduit());
                pst.setInt(4, currentSelection.idStock.get());
                pst.executeUpdate();
            }
            showSuccessAlert("Stock modifié !", "Modifications enregistrées.");
            chargerStock();
        } catch (Exception e) {
            showErrorAlert("Erreur modification", e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        if (currentSelection == null) {
            showWarningAlert("Aucune sélection", "Veuillez sélectionner une card.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer ce stock ?");
        confirm.setContentText(
                "Produit : " + currentSelection.produit.get() + "\n" +
                        "Quantité : " + currentSelection.quantite.get() + "\n" +
                        "Cette action est irréversible."
        );
        ButtonType btnOui = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.YES);
        ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnOui, btnNon);
        if (confirm.showAndWait().orElse(btnNon) == btnOui) {
            try {
                try (PreparedStatement pst = conn.prepareStatement(
                        "DELETE FROM stock WHERE idStock=?")) {
                    pst.setInt(1, currentSelection.idStock.get());
                    pst.executeUpdate();
                }
                showSuccessAlert("Supprimé !", "Le stock a été supprimé.");
                clearInputs();
                chargerStock();
            } catch (Exception e) {
                showErrorAlert("Erreur suppression", e.getMessage());
            }
        }
    }

    @FXML
    public void handleActualiser() {
        chargerStock();
        showInfoAlert("Actualisé", stockList.size() + " stock(s) affiché(s).");
    }

    private void chargerStock() {
        stockList.clear();
        cardsContainer.getChildren().clear();
        String sql = """
            SELECT s.idStock, s.idProduit, s.quantite, s.seuilAlert, p.nom, p.imagePath
            FROM stock s LEFT JOIN produit p ON p.id_produit = s.idProduit
            ORDER BY s.idStock DESC""";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                StockData data = new StockData(
                        rs.getInt("idStock"),
                        rs.getInt("idProduit"),
                        rs.getString("nom") != null ? rs.getString("nom") : "Inconnu",
                        rs.getInt("quantite"),
                        rs.getInt("seuilAlert"),
                        rs.getString("imagePath") // ✅ IMAGE
                );
                stockList.add(data);
                cardsContainer.getChildren().add(createCard(data));
            }
        } catch (SQLException e) {
            System.err.println("❌ SQL: " + e.getMessage());
        }
        if (lblStatus != null)
            lblStatus.setText("Liste chargée : " + stockList.size() + " stock(s)");
    }

    private void clearInputs() {
        tfQuantite.clear();
        tfSeuilAlerte.clear();
        cbProduit.getSelectionModel().clearSelection();
        currentSelection = null;
        selectedCard = null;
    }

    @FXML
    private void exporterCSV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter les stocks en CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("stocks_" + LocalDate.now() + ".csv");
        File file = fc.showSaveDialog(cardsContainer.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Produit,Quantité,Seuil Alerte\n");
                for (StockData s : stockList)
                    writer.write(String.format("\"%s\",%d,%d\n",
                            s.produit.get(), s.quantite.get(), s.seuilAlerte.get()));
                showSuccessAlert("Export réussi", file.getAbsolutePath());
            } catch (IOException e) {
                showErrorAlert("Erreur export", e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // ALERTES
    // ═══════════════════════════════════════════════════════

    private void showSuccessAlert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès"); a.setHeaderText(t); a.setContentText(c); a.showAndWait();
    }
    private void showErrorAlert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(t); a.setContentText(c); a.showAndWait();
    }
    private void showWarningAlert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Attention"); a.setHeaderText(t); a.setContentText(c); a.showAndWait();
    }
    private void showInfoAlert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info"); a.setHeaderText(t); a.setContentText(c); a.showAndWait();
    }
    private void showSuccess(String msg) { showSuccessAlert("Succès", msg); }
    private void showError(String msg) { showErrorAlert("Erreur", msg); }
}