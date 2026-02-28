package org.example.controllers.User;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.example.entities.User;
import org.example.services.User.UserService;
import org.example.utils.ValidationUtils;


public class EditProfilAdminController {
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField ancienMdpField;
    @FXML private PasswordField nouveauMdpField;
    @FXML private PasswordField confirmMdpField;
    @FXML private Label messageLabel;
    @FXML private Label themeModeIcon;
    @FXML private Label themeModeText;
    @FXML private AnchorPane sidebar;
    @FXML private ToggleButton themeToggle;

    private boolean isDarkMode = false;
    private User currentUser;
    private UserService userService;
    private AnchorPane contentPane;

    public void setContentPane(AnchorPane contentPane) {
        this.contentPane = contentPane;
    }
    private DashboardAdminController dashboardController;

    public void setDashboardController(DashboardAdminController controller) {
        this.dashboardController = controller;
    }
    @FXML
    public void initialize() {
        userService = new UserService();
        if (messageLabel != null) ValidationUtils.clearMessage(messageLabel);
        setupRealTimeValidation();
        // ✅ Supprimer le Platform.runLater avec setMaximized
    }

    private void setupRealTimeValidation() {
        if (nomField != null) {
            nomField.textProperty().addListener((obs, old, newVal) -> {
                if (!newVal.trim().isEmpty()) {
                    if (ValidationUtils.isValidName(newVal)) {
                        ValidationUtils.setFieldSuccess(nomField);
                    } else {
                        ValidationUtils.setFieldError(nomField);
                    }
                } else {
                    ValidationUtils.resetFieldStyle(nomField);
                }
            });
        }

        if (emailField != null) {
            emailField.textProperty().addListener((obs, old, newVal) -> {
                if (!newVal.trim().isEmpty()) {
                    if (ValidationUtils.isValidEmail(newVal)) {
                        ValidationUtils.setFieldSuccess(emailField);
                    } else {
                        ValidationUtils.setFieldError(emailField);
                    }
                } else {
                    ValidationUtils.resetFieldStyle(emailField);
                }
            });
        }

        if (nouveauMdpField != null) {
            nouveauMdpField.textProperty().addListener((obs, old, newVal) -> {
                if (!newVal.isEmpty()) {
                    if (ValidationUtils.hasMinLength(newVal, 6)) {
                        ValidationUtils.setFieldSuccess(nouveauMdpField);
                    } else {
                        ValidationUtils.setFieldError(nouveauMdpField);
                    }
                } else {
                    ValidationUtils.resetFieldStyle(nouveauMdpField);
                }
            });
        }

        if (confirmMdpField != null) {
            confirmMdpField.textProperty().addListener((obs, old, newVal) -> {
                if (!newVal.isEmpty()) {
                    if (newVal.equals(nouveauMdpField.getText())) {
                        ValidationUtils.setFieldSuccess(confirmMdpField);
                    } else {
                        ValidationUtils.setFieldError(confirmMdpField);
                    }
                } else {
                    ValidationUtils.resetFieldStyle(confirmMdpField);
                }
            });
        }
    }

    public void setUser(User user) {
        this.currentUser = user;
        if (nomField != null) nomField.setText(user.getNom());
        if (emailField != null) emailField.setText(user.getEmail());
    }

    @FXML
    private void handleEnregistrer(ActionEvent event) {

        String nouveauNom = ValidationUtils.sanitize(nomField.getText());
        String nouvelEmail = ValidationUtils.sanitize(emailField.getText());

        if (!ValidationUtils.isNotEmpty(nouveauNom) || !ValidationUtils.isNotEmpty(nouvelEmail)) {
            ValidationUtils.showError(messageLabel, "Veuillez remplir tous les champs obligatoires");
            return;
        }

        if (!ValidationUtils.isValidName(nouveauNom)) {
            ValidationUtils.showError(messageLabel, "Format de nom invalide");
            ValidationUtils.setFieldError(nomField);
            return;
        }

        if (!ValidationUtils.isValidEmail(nouvelEmail)) {
            ValidationUtils.showError(messageLabel, "Format d'email invalide");
            ValidationUtils.setFieldError(emailField);
            return;
        }

        try {

            currentUser.setNom(nouveauNom);
            currentUser.setEmail(nouvelEmail);
            userService.modifier(currentUser);

            ValidationUtils.showSuccess(messageLabel, "Profil modifié avec succès !");

            // petit délai
            new Thread(() -> {
                try {
                    Thread.sleep(1200);

                    Platform.runLater(() -> {
                        try {

                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profilAdmin.fxml"));
                            Parent root = loader.load();

                            ProfilAdminController controller = loader.getController();
                            controller.setUser(currentUser);
                            controller.setDashboardController(dashboardController);

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
                    });

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            ValidationUtils.showError(messageLabel, "Erreur lors de la modification");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAnnuler(ActionEvent event) {
        if (dashboardController != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/profilAdmin.fxml"));
                Parent root = loader.load();
                ProfilAdminController controller = loader.getController();
                controller.setUser(currentUser);
                controller.setDashboardController(dashboardController);

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
    private void handleBackToDashboard(MouseEvent event) {
        if (dashboardController != null) {
            dashboardController.reloadDashboardContent(); // ✅ recharge le contenu dans le même stage
        }
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true); // ✅
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleToggleTheme() {
        isDarkMode = !isDarkMode;

        if (isDarkMode) {
            sidebar.setStyle("-fx-background-color: #1a1a1a;");

            if (themeModeIcon != null) themeModeIcon.setText("🌙");
            if (themeModeText != null) themeModeText.setText("Sombre");
            if (themeToggle != null) {
                themeToggle.setText("☀️");
                themeToggle.setStyle("-fx-background-color: #424242; -fx-background-radius: 15; -fx-cursor: hand; -fx-font-size: 14px; -fx-text-fill: white;");
            }
        } else {
            sidebar.setStyle("-fx-background-color: #388e3c;");

            if (themeModeIcon != null) themeModeIcon.setText("☀️");
            if (themeModeText != null) themeModeText.setText("Clair");
            if (themeToggle != null) {
                themeToggle.setText("🌙");
                themeToggle.setStyle("-fx-background-color: #81c784; -fx-background-radius: 15; -fx-cursor: hand; -fx-font-size: 14px; -fx-text-fill: #388e3c;");
            }
        }
    }


    // ================= OUVRIR GESTION UTILISATEURS =================
    @FXML
    private void openManageUsers(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/manageUsers.fxml"));
            Parent root = loader.load();
            ManageUsersController controller = loader.getController();
            controller.setUser(currentUser);
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Gestion des utilisateurs");
            stage.setMaximized(true); // ✅
        } catch (Exception e) {
            showError("Impossible de charger la gestion des utilisateurs");
            e.printStackTrace();
        }
    }
    // ================= MESSAGES =================
    private void showError(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        }
    }
}
