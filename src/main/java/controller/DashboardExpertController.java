package controller;

import entities.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;

public class DashboardExpertController {

    @FXML private Label welcomeLabel;
    @FXML private AnchorPane contentPane;

    private User currentUser;

    // ✅ Sauvegarder le contenu original
    private List<Node> originalDashboardContent;

    @FXML
    public void initialize() {
        System.out.println("✅ Dashboard Expert initialisé");

        Platform.runLater(() -> {
            try {
                if (contentPane != null && contentPane.getScene() != null) {
                    Stage stage = (Stage) contentPane.getScene().getWindow();
                    stage.setMaximized(true);
                    Scene scene = contentPane.getScene();
                    if (scene.getRoot() instanceof Region r) {
                        r.prefWidthProperty().bind(scene.widthProperty());
                        r.prefHeightProperty().bind(scene.heightProperty());
                    }
                }
                // ✅ Sauvegarder le contenu original
                if (contentPane != null) {
                    originalDashboardContent = new ArrayList<>(contentPane.getChildren());
                    System.out.println("✅ Contenu original Expert sauvegardé");
                }
            } catch (Exception e) {
                System.err.println("⚠️ Erreur initialisation : " + e.getMessage());
            }
        });
    }

    public void setUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) welcomeLabel.setText("Bienvenue, " + user.getNom() + " !");
        System.out.println("✅ Utilisateur Expert : " + user.getNom());
    }

    // ✅ Charger profil dans contentPane
    @FXML
    private void handleGoToProfil(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil.fxml"));
            Parent root = loader.load();

            ProfilController controller = loader.getController();
            controller.setUser(currentUser);
            controller.setDashboardController(this); // ✅ passer ce controller

            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);
            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);

            System.out.println("✅ Profil Expert chargé dans contentPane");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement du profil");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) contentPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Login");
            stage.setMaximized(true);
            System.out.println("✅ Déconnexion réussie");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la déconnexion");
            e.printStackTrace();
        }
    }

    // ✅ Restaurer le contenu original du dashboard
    public void reloadDashboardContent() {
        if (contentPane != null && originalDashboardContent != null) {
            contentPane.getChildren().clear();
            contentPane.getChildren().addAll(originalDashboardContent);
            System.out.println("✅ Dashboard Expert restauré");
        }
    }

    public User getCurrentUser() { return currentUser; }
}