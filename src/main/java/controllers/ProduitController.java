package controllers;

import entities.Produit;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import services.ProduitService;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

public class ProduitController {

    @FXML private TextField tfNom, tfDescription, tfPrix;
    @FXML private TextField tfImagePath;
    @FXML private Button btnParcourir;
    @FXML private ImageView imagePreview;
    @FXML private Label defaultImageIcon;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private Button btnAjouter, btnModifier, btnSupprimer, btnActualiser;
    @FXML private FlowPane cardsContainer;
    @FXML private Label lblStatus;

    private ProduitService produitService = new ProduitService();
    private ProduitData currentSelection = null;
    private VBox selectedCard = null;
    private String selectedImagePath = null;
    private static final String IMAGE_DIRECTORY = "images/produits/";
    private ObservableList<ProduitData> produitsList = FXCollections.observableArrayList();

    public static class ProduitData {
        public final LongProperty id = new SimpleLongProperty();
        public final StringProperty nom = new SimpleStringProperty();
        public final StringProperty description = new SimpleStringProperty();
        public final DoubleProperty prix = new SimpleDoubleProperty();
        public final StringProperty categorie = new SimpleStringProperty();
        public final StringProperty imagePath = new SimpleStringProperty();

        public ProduitData(long id, String nom, String description, double prix, String categorie, String imagePath) {
            this.id.set(id);
            this.nom.set(nom);
            this.description.set(description);
            this.prix.set(prix);
            this.categorie.set(categorie);
            this.imagePath.set(imagePath != null ? imagePath : "");
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
    }

    private VBox createCard(ProduitData p) {
        VBox card = new VBox(10);
        card.setPrefWidth(200);
        card.setMaxWidth(200);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);" +
                        "-fx-padding: 15;" +
                        "-fx-cursor: hand;"
        );

        // Image circulaire
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

        // Nom
        Label lblNom = new Label(p.nom.get());
        lblNom.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2d2d2d; -fx-wrap-text: true;");
        lblNom.setMaxWidth(170);
        lblNom.setWrapText(true);

        // Badge catégorie
        Label lblCat = new Label("🏷 " + p.categorie.get());
        lblCat.setStyle(
                "-fx-background-color: #e8f5e9;" +
                        "-fx-text-fill: #4a7c3a;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 3 8;" +
                        "-fx-background-radius: 10;"
        );

        // Description tronquée
        String desc = p.description.get() != null ? p.description.get() : "";
        Label lblDesc = new Label(desc.length() > 60 ? desc.substring(0, 57) + "..." : desc);
        lblDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #777; -fx-wrap-text: true;");
        lblDesc.setMaxWidth(170);
        lblDesc.setWrapText(true);

        // Prix
        Label lblPrix = new Label(String.format("%.2f DT", p.prix.get()));
        lblPrix.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #5a8c4a;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #e0e0e0;");

        card.getChildren().addAll(imgBox, lblNom, lblCat, lblDesc, sep, lblPrix);

        // Clic -> sélection
        card.setOnMouseClicked(e -> selectCard(p, card));

        // Hover
        card.setOnMouseEntered(e -> {
            if (currentSelection == null || currentSelection.id.get() != p.id.get()) {
                card.setStyle(
                        "-fx-background-color: #f0faf0;" +
                                "-fx-background-radius: 12;" +
                                "-fx-border-radius: 12;" +
                                "-fx-border-color: #5a8c4a;" +
                                "-fx-border-width: 1.5;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 14, 0, 0, 5);" +
                                "-fx-padding: 15;" +
                                "-fx-cursor: hand;"
                );
            }
        });
        card.setOnMouseExited(e -> {
            if (currentSelection == null || currentSelection.id.get() != p.id.get()) {
                card.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-background-radius: 12;" +
                                "-fx-border-radius: 12;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);" +
                                "-fx-padding: 15;" +
                                "-fx-cursor: hand;"
                );
            }
        });

        return card;
    }

    private void selectCard(ProduitData p, VBox card) {
        // Désélectionner l'ancienne card
        if (selectedCard != null) {
            selectedCard.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 12;" +
                            "-fx-border-radius: 12;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);" +
                            "-fx-padding: 15;" +
                            "-fx-cursor: hand;"
            );
        }

        // Sélectionner la nouvelle
        card.setStyle(
                "-fx-background-color: #e8f5e9;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-color: #4a7c3a;" +
                        "-fx-border-width: 2.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(74,124,58,0.3), 14, 0, 0, 5);" +
                        "-fx-padding: 15;" +
                        "-fx-cursor: hand;"
        );
        selectedCard = card;
        currentSelection = p;

        // Remplir le formulaire
        tfNom.setText(p.nom.get());
        tfDescription.setText(p.description.get());
        tfPrix.setText(String.format(java.util.Locale.US, "%.2f", p.prix.get()));
        cbCategorie.setValue(p.categorie.get());

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
                showErrorAlert("Erreur de copie", "Impossible de copier l'image : " + e.getMessage());
            }
        }
    }

    private void createImageDirectory() {
        try {
            Path path = Paths.get(IMAGE_DIRECTORY);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur création dossier images : " + e.getMessage());
        }
    }

    private void loadImagePreview(String imagePath) {
        if (imagePreview == null) return;
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString(), 174, 174, false, true);
                    imagePreview.setImage(image);
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

    @FXML
    private void handleAjouter() {
        if (!validateInput()) return;
        try {
            Produit p = new Produit(
                    tfNom.getText().trim(),
                    tfDescription.getText().trim(),
                    Double.parseDouble(tfPrix.getText().trim()),
                    cbCategorie.getValue(),
                    selectedImagePath
            );
            produitService.ajouter(p);
            showSuccessAlert("Produit ajouté avec succès !", "Le produit a bien été enregistré.");
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
            currentSelection.nom.set(tfNom.getText().trim());
            currentSelection.description.set(tfDescription.getText().trim());
            currentSelection.prix.set(Double.parseDouble(tfPrix.getText().trim()));
            currentSelection.categorie.set(cbCategorie.getValue());
            currentSelection.imagePath.set(selectedImagePath);

            Produit p = new Produit(
                    currentSelection.id.get(),
                    currentSelection.nom.get(),
                    currentSelection.description.get(),
                    currentSelection.prix.get(),
                    currentSelection.categorie.get(),
                    true,
                    selectedImagePath
            );
            produitService.modifier(p);
            showSuccessAlert("Produit modifié avec succès !", "Les modifications ont été enregistrées.");
            chargerProduits();
        } catch (Exception e) {
            showErrorAlert("Erreur lors de la modification", e.getMessage());
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

        DialogPane dialogPane = confirm.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialogPane.getStyleClass().add("delete-confirmation-alert");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == btnOui) {
            try {
                produitService.supprimer(currentSelection.id.get());
                clearInputs();
                chargerProduits();
                showSuccessAlert("Supprimé avec succès", "Le produit a été supprimé de la base.");
                lblStatus.setText("Produit supprimé");
            } catch (Exception e) {
                showErrorAlert("Erreur suppression", e.getMessage());
            }
        }
    }

    @FXML
    private void handleActualiser() {
        clearInputs();
        chargerProduits();
        showInfoAlert("Liste actualisée",
                "La liste des produits a été rafraîchie.\n" + produitsList.size() + " produit(s) trouvé(s).");
        lblStatus.setText("Actualisé : " + produitsList.size() + " produits affichés");
    }

    @FXML
    private void exporterCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les produits en CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
        fileChooser.setInitialFileName("produits_" + LocalDate.now() + ".csv");

        File file = fileChooser.showSaveDialog(cardsContainer.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.append("ID,Nom,Description,Prix (DT),Catégorie,Image\n");
                for (ProduitData p : produitsList) {
                    writer.append(String.format("%d,\"%s\",\"%s\",%.2f,\"%s\",\"%s\"\n",
                            p.id.get(),
                            escapeCsv(p.nom.get()),
                            escapeCsv(p.description.get()),
                            p.prix.get(),
                            escapeCsv(p.categorie.get()),
                            escapeCsv(p.imagePath.get())));
                }
                showSuccessAlert("Export réussi", "Fichier sauvegardé :\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showErrorAlert("Erreur d'export", "Impossible de sauvegarder :\n" + e.getMessage());
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
            Stage currentStage = (Stage) tfNom.getScene().getWindow();
            currentStage.close();
            homeStage.show();
        } catch (IOException e) {
            showErrorAlert("Erreur de navigation", "Impossible de retourner à l'accueil : " + e.getMessage());
        }
    }

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
        else if (nom.length() < 3 || nom.length() > 50) errors.append("• Le nom doit contenir entre 3 et 50 caractères.\n");

        String desc = tfDescription.getText().trim();
        if (desc.length() > 250) errors.append("• La description ne peut pas dépasser 250 caractères.\n");

        String prixText = tfPrix.getText().trim();
        if (prixText.isEmpty()) errors.append("• Le prix est obligatoire.\n");
        else {
            try {
                double prix = Double.parseDouble(prixText);
                if (prix <= 0) errors.append("• Le prix doit être supérieur à 0.\n");
            } catch (NumberFormatException e) {
                errors.append("• Le prix doit être un nombre valide (ex: 28.50).\n");
            }
        }

        if (cbCategorie.getValue() == null) errors.append("• La catégorie est obligatoire.\n");

        if (selectedImagePath == null || selectedImagePath.isEmpty()) {
            errors.append("• L'image est recommandée pour une meilleure présentation.\n");
        }

        if (errors.length() > 0) {
            showErrorAlert("Champs incomplets ou invalides", errors.toString());
            return false;
        }
        return true;
    }

    private void clearInputs() {
        tfNom.clear();
        tfDescription.clear();
        tfPrix.clear();
        cbCategorie.setValue(null);
        tfImagePath.clear();
        selectedImagePath = null;
        if (imagePreview != null) imagePreview.setImage(null);
        if (defaultImageIcon != null) defaultImageIcon.setVisible(true);
        currentSelection = null;
        selectedCard = null;
    }

    private void chargerProduits() {
        produitsList.clear();
        cardsContainer.getChildren().clear();

        for (Produit p : produitService.getAllProduits()) {
            ProduitData data = new ProduitData(
                    p.getIdProduit(),
                    p.getNom(),
                    p.getDescription(),
                    p.getPrix(),
                    p.getCategorie(),
                    p.getImagePath()
            );
            produitsList.add(data);
            cardsContainer.getChildren().add(createCard(data));
        }

        lblStatus.setText("Liste chargée : " + produitsList.size() + " produits");
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(title);
        alert.setContentText(content);
        DialogPane dp = alert.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dp.getStyleClass().add("success-alert");
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(title);
        alert.setContentText(content);
        DialogPane dp = alert.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dp.getStyleClass().add("error-alert");
        alert.showAndWait();
    }

    private void showWarningAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(title);
        alert.setContentText(content);
        DialogPane dp = alert.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dp.getStyleClass().add("warning-alert");
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}