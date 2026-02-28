package org.example.controllers.User;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import org.example.entities.User;
import org.example.services.User.UserService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ProfilAdminController {

    @FXML private Label nomLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private Label etatLabel;
    @FXML private Label dateCreationLabel;
    @FXML private Label messageLabel;

    private User currentUser;
    private UserService userService;
    private DashboardAdminController dashboardController;

    @FXML
    public void initialize() {
        userService = new UserService();

        Platform.runLater(() -> {
            try {
                Stage stage = (Stage) nomLabel.getScene().getWindow();
                stage.setMaximized(true);
                Scene scene = stage.getScene();
                if (scene.getRoot() instanceof Region r) {
                    r.prefWidthProperty().bind(scene.widthProperty());
                    r.prefHeightProperty().bind(scene.heightProperty());
                }
            } catch (Exception e) {}
        });
    }

    public void setDashboardController(DashboardAdminController controller) {
        this.dashboardController = controller;
    }

    public void setUser(User user) {
        this.currentUser = user;

        if (nomLabel != null) nomLabel.setText(user.getNom());
        if (emailLabel != null) emailLabel.setText(user.getEmail());
        if (roleLabel != null) roleLabel.setText(user.getRole().toString());
        if (etatLabel != null) {
            etatLabel.setText(user.getEtatCompte().toString());
            if (user.getEtatCompte().toString().equals("ACTIF")) {
                etatLabel.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #10b981; -fx-font-weight: bold; -fx-padding: 5 15; -fx-background-radius: 15;");
            } else {
                etatLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-padding: 5 15; -fx-background-radius: 15;");
            }
        }

        if (dateCreationLabel != null && user.getDateCreation() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
            dateCreationLabel.setText(user.getDateCreation().format(formatter));
        }
    }

    // ✅ Charger editProfilAdmin dans le contentPane du dashboard
    @FXML
    private void handleModifierProfil(ActionEvent event) {
        if (dashboardController != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/editProfilAdmin.fxml"));
                Parent root = loader.load();

                EditProfilAdminController controller = loader.getController();
                controller.setUser(currentUser);
                controller.setDashboardController(dashboardController); // ✅ passer le dashboard

                AnchorPane contentPane = dashboardController.getContentPane();
                contentPane.getChildren().clear();
                contentPane.getChildren().add(root);

                AnchorPane.setTopAnchor(root, 0.0);
                AnchorPane.setBottomAnchor(root, 0.0);
                AnchorPane.setLeftAnchor(root, 0.0);
                AnchorPane.setRightAnchor(root, 0.0);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ✅ Changer mot de passe via dialog
    @FXML
    private void handleChangePassword(ActionEvent event) {
        if (dashboardController != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/changePassword.fxml"));
                Parent root = loader.load();

                ChangePasswordController controller = loader.getController();
                controller.setUser(currentUser);
                controller.setDashboardController(dashboardController);
                controller.setContentPane(dashboardController.getContentPane());

                AnchorPane contentPane = dashboardController.getContentPane();
                contentPane.getChildren().clear();
                contentPane.getChildren().add(root);
                AnchorPane.setTopAnchor(root, 0.0);
                AnchorPane.setBottomAnchor(root, 0.0);
                AnchorPane.setLeftAnchor(root, 0.0);
                AnchorPane.setRightAnchor(root, 0.0);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @FXML
    private void handleSupprimerCompte(ActionEvent event) throws SQLException {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer votre compte ?");
        confirmation.setContentText("⚠️ Cette action est IRRÉVERSIBLE !");

        ButtonType buttonOui = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonNon = new ButtonType("Non, annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(buttonOui, buttonNon);

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == buttonOui) {
            userService.supprimer(currentUser.getId());
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
                Stage stage = (Stage) nomLabel.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("AgriConnect - Connexion");
                stage.setMaximized(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleBackToDashboard(MouseEvent event) {
        if (dashboardController != null) {
            dashboardController.reloadDashboardContent();
        }
    }

    private void showError(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        }
    }

    private void showSuccess(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
        }
    }
    private AnchorPane contentPane;

    public void setContentPane(AnchorPane contentPane) {
        this.contentPane = contentPane;
    }
}
