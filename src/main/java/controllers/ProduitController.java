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
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import services.ProduitService;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.utils.QRCodeGenerator;
import services.WhatsAppService;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ProduitController {

    @FXML private TextField tfNom, tfDescription, tfPrix;
    @FXML private TextField tfImagePath;
    @FXML private TextField tfRecherche;
    @FXML private Button btnParcourir;
    @FXML private Button btnTri;
    @FXML private ImageView imagePreview;
    @FXML private Label defaultImageIcon;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private Button btnAjouter, btnModifier, btnSupprimer, btnActualiser;
    @FXML private FlowPane cardsContainer;
    @FXML private Label lblStatus;
    @FXML private CheckBox cbPromo;
    @FXML private TextField tfRemise;

    private ProduitService produitService = new ProduitService();
    private ProduitData currentSelection = null;
    private VBox selectedCard = null;
    private String selectedImagePath = null;
    private static final String IMAGE_DIRECTORY = "images/produits/";
    private ObservableList<ProduitData> produitsList = FXCollections.observableArrayList();
    private ObservableList<ProduitData> tousLesProduits = FXCollections.observableArrayList();

    private boolean triAZ = true;

    // ═══════════════════════════════════════════════════════
    // PRODUIT DATA
    // ═══════════════════════════════════════════════════════

    public static class ProduitData {
        public final LongProperty id = new SimpleLongProperty();
        public final StringProperty nom = new SimpleStringProperty();
        public final StringProperty description = new SimpleStringProperty();
        public final DoubleProperty prix = new SimpleDoubleProperty();
        public final StringProperty categorie = new SimpleStringProperty();
        public final StringProperty imagePath = new SimpleStringProperty();
        public final BooleanProperty promo = new SimpleBooleanProperty(); // ✅

        public ProduitData(long id, String nom, String description, double prix,
                           String categorie, String imagePath, boolean promo) {
            this.id.set(id);
            this.nom.set(nom);
            this.description.set(description);
            this.prix.set(prix);
            this.categorie.set(categorie);
            this.imagePath.set(imagePath != null ? imagePath : "");
            this.promo.set(promo); // ✅
        }
    }




    @FXML
    public void initialize() {
        createImageDirectory();
        cbCategorie.setItems(FXCollections.observableArrayList(
                "Légumes", "Fruits", "Céréales", "Huiles", "Épices",
                "Produits laitiers", "Miel", "Viandes", "Autres"
        ));
        chargerProduits();

        if (tfRecherche != null) {
            tfRecherche.textProperty().addListener((obs, old, newVal) -> filtrerProduits(newVal));
        }

        if (cbPromo != null && tfRemise != null) {
            tfRemise.setVisible(false);
            tfRemise.setManaged(false);
            cbPromo.selectedProperty().addListener((obs, old, selected) -> {
                tfRemise.setVisible(selected);
                tfRemise.setManaged(selected);
            });
        }
    }

    // ═══════════════════════════════════════════════════════
    // RECHERCHE ET TRI
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleTri() {
        triAZ = !triAZ;
        btnTri.setText(triAZ ? "🔤 A → Z" : "🔤 Z → A");
        btnTri.setStyle(
                "-fx-background-color: " + (triAZ ? "#5a8c4a" : "#7b68ee") + "; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12px;"
        );
        filtrerProduits(tfRecherche != null ? tfRecherche.getText() : "");
    }

    private void filtrerProduits(String recherche) {
        List<ProduitData> filtres = tousLesProduits.stream()
                .filter(p -> recherche == null || recherche.isEmpty() ||
                        p.nom.get().toLowerCase().startsWith(recherche.toLowerCase()))
                .sorted((a, b) -> triAZ
                        ? a.nom.get().compareToIgnoreCase(b.nom.get())
                        : b.nom.get().compareToIgnoreCase(a.nom.get()))
                .collect(Collectors.toList());

        cardsContainer.getChildren().clear();
        for (ProduitData p : filtres) {
            cardsContainer.getChildren().add(createCard(p));
        }

        if (lblStatus != null)
            lblStatus.setText(filtres.size() + " produit(s) affiché(s)");
    }


    private VBox createCard(ProduitData p) {
        VBox card = new VBox(10);
        card.setPrefWidth(200);
        card.setMaxWidth(200);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12; -fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);" +
                        "-fx-padding: 15; -fx-cursor: hand;"
        );

        // ✅ Badge PROMO en haut de la carte
        if (p.promo.get()) {
            HBox badgeBox = new HBox();
            badgeBox.setAlignment(Pos.CENTER_RIGHT);
            Label lblPromo = new Label("🔥 PROMO");
            lblPromo.setStyle(
                    "-fx-background-color: #d9534f; -fx-text-fill: white;" +
                            "-fx-font-size: 11px; -fx-font-weight: bold;" +
                            "-fx-padding: 3 10; -fx-background-radius: 10;"
            );
            badgeBox.getChildren().add(lblPromo);
            card.getChildren().add(badgeBox);
        }

        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(100, 100);
        imageContainer.setMaxSize(100, 100);

        Circle bgCircle = new Circle(50);
        bgCircle.setStyle("-fx-fill: #e8f5e9; -fx-stroke: #4a7c3a; -fx-stroke-width: 2;");

        ImageView iv = new ImageView();
        iv.setFitWidth(96);
        iv.setFitHeight(96);
        iv.setPreserveRatio(false);
        Circle clip = new Circle(48, 48, 48);
        iv.setClip(clip);

        Label iconFallback = new Label("🌿");
        iconFallback.setStyle("-fx-font-size: 28px;");

        String imgPath = p.imagePath.get();
        if (imgPath != null && !imgPath.isEmpty()) {
            File f = new File(imgPath);
            if (f.exists()) {
                try {
                    Image img = new Image(f.toURI().toString(), 96, 96, false, true);
                    iv.setImage(img);
                    iconFallback.setVisible(false);
                } catch (Exception e) {
                    iv.setImage(null);
                }
            }
        }

        imageContainer.getChildren().addAll(bgCircle, iv, iconFallback);
        HBox imgBox = new HBox(imageContainer);
        imgBox.setStyle("-fx-alignment: center;");

        Label lblNom = new Label(p.nom.get());
        lblNom.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2d2d2d;");
        lblNom.setMaxWidth(170);
        lblNom.setWrapText(true);

        Label lblCat = new Label("🏷 " + p.categorie.get());
        lblCat.setStyle(
                "-fx-background-color: #e8f5e9; -fx-text-fill: #4a7c3a;" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-padding: 3 8; -fx-background-radius: 10;"
        );

        String desc = p.description.get() != null ? p.description.get() : "";
        Label lblDesc = new Label(desc.length() > 60 ? desc.substring(0, 57) + "..." : desc);
        lblDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #777; -fx-wrap-text: true;");
        lblDesc.setMaxWidth(170);
        lblDesc.setWrapText(true);

        Label lblPrix = new Label(String.format("%.2f DT", p.prix.get()));
        lblPrix.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #5a8c4a;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #e0e0e0;");

        card.getChildren().addAll(imgBox, lblNom, lblCat, lblDesc, sep, lblPrix);

       
        if (p.promo.get()) {
            card.setStyle(
                    "-fx-background-color: #fff8f0;" +
                            "-fx-background-radius: 12; -fx-border-radius: 12;" +
                            "-fx-border-color: #d9534f; -fx-border-width: 2;" +
                            "-fx-effect: dropshadow(gaussian, rgba(217,83,79,0.3), 10, 0, 0, 3);" +
                            "-fx-padding: 15; -fx-cursor: hand;"
            );
        }

        card.setOnMouseClicked(e -> selectCard(p, card));
        card.setOnMouseEntered(e -> {
            if (currentSelection == null || currentSelection.id.get() != p.id.get()) {
                card.setStyle(
                        "-fx-background-color: #f0faf0;" +
                                "-fx-background-radius: 12; -fx-border-radius: 12;" +
                                "-fx-border-color: #5a8c4a; -fx-border-width: 1.5;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 14, 0, 0, 5);" +
                                "-fx-padding: 15; -fx-cursor: hand;"
                );
            }
        });
        card.setOnMouseExited(e -> {
            if (currentSelection == null || currentSelection.id.get() != p.id.get()) {
                if (p.promo.get()) {
                    card.setStyle(
                            "-fx-background-color: #fff8f0;" +
                                    "-fx-background-radius: 12; -fx-border-radius: 12;" +
                                    "-fx-border-color: #d9534f; -fx-border-width: 2;" +
                                    "-fx-effect: dropshadow(gaussian, rgba(217,83,79,0.3), 10, 0, 0, 3);" +
                                    "-fx-padding: 15; -fx-cursor: hand;"
                    );
                } else {
                    card.setStyle(
                            "-fx-background-color: white;" +
                                    "-fx-background-radius: 12; -fx-border-radius: 12;" +
                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);" +
                                    "-fx-padding: 15; -fx-cursor: hand;"
                    );
                }
            }
        });

        return card;
    }

    private void selectCard(ProduitData p, VBox card) {
        if (selectedCard != null) {
            selectedCard.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 12; -fx-border-radius: 12;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);" +
                            "-fx-padding: 15; -fx-cursor: hand;"
            );
        }
        card.setStyle(
                "-fx-background-color: #e8f5e9;" +
                        "-fx-background-radius: 12; -fx-border-radius: 12;" +
                        "-fx-border-color: #4a7c3a; -fx-border-width: 2.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(74,124,58,0.3), 14, 0, 0, 5);" +
                        "-fx-padding: 15; -fx-cursor: hand;"
        );
        selectedCard = card;
        currentSelection = p;

        tfNom.setText(p.nom.get());
        tfDescription.setText(p.description.get());
        tfPrix.setText(String.format(java.util.Locale.US, "%.2f", p.prix.get()));
        cbCategorie.setValue(p.categorie.get());

        // ✅ Cocher la checkbox si le produit est en promo
        if (cbPromo != null) cbPromo.setSelected(p.promo.get());

        String imgPath = p.imagePath.get();
        if (imgPath != null && !imgPath.isEmpty()) {
            tfImagePath.setText(imgPath);
            selectedImagePath = imgPath;
            loadImagePreview(imgPath);
        } else {
            tfImagePath.setText("");
            selectedImagePath = null;
            if (imagePreview != null) imagePreview.setImage(null);
            if (defaultImageIcon != null) defaultImageIcon.setVisible(true);
        }
    }

    // ═══════════════════════════════════════════════════════
    // QR CODE
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleGenererQR() {
        if (currentSelection == null) {
            showWarningAlert("Aucune sélection", "Veuillez sélectionner un produit.");
            return;
        }
        try {
            String contenu = QRCodeGenerator.formatProduit(
                    currentSelection.nom.get(),
                    currentSelection.categorie.get(),
                    currentSelection.prix.get(),
                    currentSelection.description.get()
            );
            WritableImage qrImage = QRCodeGenerator.genererFX(contenu, 300, 300);
            afficherPopupQR(qrImage, currentSelection.nom.get());
        } catch (Exception e) {
            showErrorAlert("Erreur QR Code", e.getMessage());
        }
    }

    private void afficherPopupQR(WritableImage fxImage, String nomProduit) {
        Stage popupStage = new Stage();
        popupStage.setTitle("QR Code - " + nomProduit);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f5f5dc; -fx-padding: 30;");

        Label titre = new Label("📱 QR Code Produit");
        titre.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4a7c3a;");

        Label sousTitre = new Label(nomProduit);
        sousTitre.setStyle("-fx-font-size: 14px; -fx-text-fill: #666; -fx-font-style: italic;");

        ImageView qrView = new ImageView(fxImage);
        qrView.setFitWidth(280);
        qrView.setFitHeight(280);
        qrView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 3);");

        Label info = new Label("Scannez ce QR code pour voir les détails du produit");
        info.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        HBox boutons = new HBox(15);
        boutons.setAlignment(Pos.CENTER);

        Button btnSauvegarder = new Button("💾 Sauvegarder PNG");
        btnSauvegarder.setStyle(
                "-fx-background-color: #5a8c4a; -fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 10 20;"
        );
        btnSauvegarder.setOnAction(e -> sauvegarderQR(fxImage, nomProduit, popupStage));

        Button btnFermer = new Button("✕ Fermer");
        btnFermer.setStyle(
                "-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 10 20;"
        );
        btnFermer.setOnAction(e -> popupStage.close());

        boutons.getChildren().addAll(btnSauvegarder, btnFermer);
        root.getChildren().addAll(titre, sousTitre, qrView, info, boutons);

        popupStage.setScene(new Scene(root, 400, 480));
        popupStage.setResizable(false);
        popupStage.show();
    }

    private void sauvegarderQR(WritableImage image, String nomProduit, Stage parentStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder le QR Code");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image PNG", "*.png"));
        fileChooser.setInitialFileName("QR_" + nomProduit.replaceAll("[^a-zA-Z0-9]", "_") + ".png");
        File file = fileChooser.showSaveDialog(parentStage);
        if (file != null) {
            try {
                int w = (int) image.getWidth();
                int h = (int) image.getHeight();
                int[] pixels = new int[w * h];
                image.getPixelReader().getPixels(0, 0, w, h,
                        javafx.scene.image.PixelFormat.getIntArgbInstance(), pixels, 0, w);
                java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(
                        w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                bi.setRGB(0, 0, w, h, pixels, 0, w);
                ImageIO.write(bi, "PNG", file);
                showSuccessAlert("QR Code sauvegardé !", "Fichier : " + file.getAbsolutePath());
            } catch (IOException e) {
                showErrorAlert("Erreur sauvegarde", e.getMessage());
            }
        }
    }


    @FXML
    private void handleParcourir() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image du produit");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        File selectedFile = fileChooser.showOpenDialog(tfImagePath.getScene().getWindow());
        if (selectedFile != null) {
            try {
                String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
                Path destination = Paths.get(IMAGE_DIRECTORY + fileName);
                Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                selectedImagePath = IMAGE_DIRECTORY + fileName;
                tfImagePath.setText(selectedImagePath);
                loadImagePreview(selectedImagePath);
            } catch (IOException e) {
                showErrorAlert("Erreur de copie", e.getMessage());
            }
        }
    }

    private void createImageDirectory() {
        try {
            Path path = Paths.get(IMAGE_DIRECTORY);
            if (!Files.exists(path)) Files.createDirectories(path);
        } catch (IOException e) {
            System.err.println("❌ Erreur création dossier : " + e.getMessage());
        }
    }

    private void loadImagePreview(String imagePath) {
        if (imagePreview == null) return;
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    imagePreview.setImage(new Image(file.toURI().toString(), 174, 174, false, true));
                    if (defaultImageIcon != null) defaultImageIcon.setVisible(false);
                } else {
                    imagePreview.setImage(null);
                    if (defaultImageIcon != null) defaultImageIcon.setVisible(true);
                }
            } catch (Exception e) {
                imagePreview.setImage(null);
                if (defaultImageIcon != null) defaultImageIcon.setVisible(true);
            }
        } else {
            imagePreview.setImage(null);
            if (defaultImageIcon != null) defaultImageIcon.setVisible(true);
        }
    }

    // ═══════════════════════════════════════════════════════
    // CRUD
    // ═══════════════════════════════════════════════════════

    @FXML
    private void handleAjouter() {
        if (!validateInput()) return;
        try {
            boolean isPromo = cbPromo != null && cbPromo.isSelected();

            Produit p = new Produit(
                    tfNom.getText().trim(), tfDescription.getText().trim(),
                    Double.parseDouble(tfPrix.getText().trim()),
                    cbCategorie.getValue(), selectedImagePath, isPromo // ✅
            );
            produitService.ajouter(p);

            // ✅ Envoi WhatsApp si promo cochée
            if (isPromo) {
                double remise = 0;
                if (tfRemise != null && !tfRemise.getText().trim().isEmpty()) {
                    try { remise = Double.parseDouble(tfRemise.getText().trim()); }
                    catch (NumberFormatException ignored) {}
                }
                WhatsAppService.envoyerPromo(
                        tfNom.getText().trim(),
                        Double.parseDouble(tfPrix.getText().trim()),
                        remise
                );
                showInfoAlert("WhatsApp envoyé !", "🔥 Promo envoyée via WhatsApp avec succès !");
            }

            showSuccessAlert("Produit ajouté !", "Le produit a bien été enregistré.");
            clearInputs();
            chargerProduits();
        } catch (Exception e) {
            showErrorAlert("Erreur lors de l'ajout", e.getMessage());
        }
    }

    @FXML
    private void handleModifier() {
        if (currentSelection == null) {
            showWarningAlert("Aucune sélection", "Veuillez sélectionner un produit.");
            return;
        }
        if (!validateInput()) return;
        try {
            boolean isPromo = cbPromo != null && cbPromo.isSelected();

            Produit p = new Produit(
                    currentSelection.id.get(), tfNom.getText().trim(),
                    tfDescription.getText().trim(),
                    Double.parseDouble(tfPrix.getText().trim()),
                    cbCategorie.getValue(), true, selectedImagePath, isPromo // ✅
            );
            produitService.modifier(p);

            // ✅ Envoi WhatsApp si promo cochée
            if (isPromo) {
                double remise = 0;
                if (tfRemise != null && !tfRemise.getText().trim().isEmpty()) {
                    try { remise = Double.parseDouble(tfRemise.getText().trim()); }
                    catch (NumberFormatException ignored) {}
                }
                WhatsAppService.envoyerPromo(
                        tfNom.getText().trim(),
                        Double.parseDouble(tfPrix.getText().trim()),
                        remise
                );
                showInfoAlert("WhatsApp envoyé !", "🔥 Promo mise à jour envoyée via WhatsApp !");
            }

            showSuccessAlert("Produit modifié !", "Modifications enregistrées.");
            clearInputs();
            chargerProduits();
        } catch (Exception e) {
            showErrorAlert("Erreur modification", e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        if (currentSelection == null) {
            showWarningAlert("Aucune sélection", "Veuillez sélectionner un produit.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer ce produit ?");
        confirm.setContentText(
                "Produit : " + currentSelection.nom.get() + "\n" +
                        "Catégorie : " + currentSelection.categorie.get() + "\n" +
                        "Cette action est irréversible."
        );
        ButtonType btnOui = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.YES);
        ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnOui, btnNon);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == btnOui) {
            try {
                produitService.supprimer(currentSelection.id.get());
                clearInputs();
                chargerProduits();
                showSuccessAlert("Supprimé !", "Le produit a été supprimé.");
            } catch (Exception e) {
                showErrorAlert("Erreur suppression", e.getMessage());
            }
        }
    }

    @FXML
    private void handleActualiser() {
        clearInputs();
        chargerProduits();
    }

    @FXML
    private void exporterCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter en CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fileChooser.setInitialFileName("produits_" + LocalDate.now() + ".csv");
        File file = fileChooser.showSaveDialog(cardsContainer.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.append("Nom,Description,Prix (DT),Catégorie,Promo\n");
                for (ProduitData p : tousLesProduits) {
                    writer.append(String.format("\"%s\",\"%s\",%.2f,\"%s\",\"%s\"\n",
                            escapeCsv(p.nom.get()), escapeCsv(p.description.get()),
                            p.prix.get(), escapeCsv(p.categorie.get()),
                            p.promo.get() ? "Oui" : "Non"));
                }
                showSuccessAlert("Export réussi", "Fichier : " + file.getAbsolutePath());
            } catch (IOException e) {
                showErrorAlert("Erreur export", e.getMessage());
            }
        }
    }

    @FXML
    private void retourAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/home.fxml"));
            Parent root = loader.load();
            Stage homeStage = new Stage();
            homeStage.setScene(new Scene(root, 1400, 850));
            homeStage.setTitle("Accueil - AgriConnect");
            homeStage.setMaximized(true);
            Stage currentStage = (Stage) tfNom.getScene().getWindow();
            currentStage.close();
            homeStage.show();
        } catch (IOException e) {
            showErrorAlert("Erreur de navigation", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════════════════

    private String escapeCsv(String s) {
        if (s == null) return "";
        s = s.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) return "\"" + s + "\"";
        return s;
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();
        String nom = tfNom.getText().trim();
        if (nom.isEmpty()) errors.append("• Le nom est obligatoire.\n");
        else if (nom.length() < 3 || nom.length() > 50)
            errors.append("• Le nom doit contenir entre 3 et 50 caractères.\n");
        String prixText = tfPrix.getText().trim();
        if (prixText.isEmpty()) errors.append("• Le prix est obligatoire.\n");
        else {
            try {
                double prix = Double.parseDouble(prixText);
                if (prix <= 0) errors.append("• Le prix doit être supérieur à 0.\n");
            } catch (NumberFormatException e) {
                errors.append("• Le prix doit être un nombre valide.\n");
            }
        }
        if (cbCategorie.getValue() == null) errors.append("• La catégorie est obligatoire.\n");
        if (cbPromo != null && cbPromo.isSelected() && tfRemise != null
                && !tfRemise.getText().trim().isEmpty()) {
            try {
                double remise = Double.parseDouble(tfRemise.getText().trim());
                if (remise < 0 || remise > 100)
                    errors.append("• La remise doit être entre 0 et 100%.\n");
            } catch (NumberFormatException e) {
                errors.append("• La remise doit être un nombre valide.\n");
            }
        }
        if (errors.length() > 0) {
            showErrorAlert("Champs invalides", errors.toString());
            return false;
        }
        return true;
    }

    private void clearInputs() {
        tfNom.clear(); tfDescription.clear(); tfPrix.clear();
        cbCategorie.setValue(null); tfImagePath.clear();
        selectedImagePath = null;
        if (imagePreview != null) imagePreview.setImage(null);
        if (defaultImageIcon != null) defaultImageIcon.setVisible(true);
        if (cbPromo != null) cbPromo.setSelected(false);
        if (tfRemise != null) { tfRemise.clear(); tfRemise.setVisible(false); tfRemise.setManaged(false); }
        currentSelection = null;
        selectedCard = null;
    }

    private void chargerProduits() {
        produitsList.clear();
        tousLesProduits.clear();
        cardsContainer.getChildren().clear();

        for (Produit p : produitService.getAllProduits()) {
            ProduitData data = new ProduitData(
                    p.getIdProduit(), p.getNom(), p.getDescription(),
                    p.getPrix(), p.getCategorie(), p.getImagePath(),
                    p.isPromo() // ✅
            );
            produitsList.add(data);
            tousLesProduits.add(data);
        }

        filtrerProduits(tfRecherche != null ? tfRecherche.getText() : "");

        if (lblStatus != null)
            lblStatus.setText("Liste chargée : " + tousLesProduits.size() + " produits");
    }

    // ═══════════════════════════════════════════════════════
    // ALERTES
    // ═══════════════════════════════════════════════════════

    private void showSuccessAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès"); a.setHeaderText(title); a.setContentText(content); a.showAndWait();
    }
    private void showErrorAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(title); a.setContentText(content); a.showAndWait();
    }
    private void showWarningAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Attention"); a.setHeaderText(title); a.setContentText(content); a.showAndWait();
    }
    private void showInfoAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info"); a.setHeaderText(title); a.setContentText(content); a.showAndWait();
    }
}