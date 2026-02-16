package controller;

import entities.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import service.UserService;
import utils.ValidationUtils;

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

    @FXML
    public void initialize() {
        userService = new UserService();
        ValidationUtils.clearMessage(messageLabel);
        setupRealTimeValidation();
    }

    // ================= VALIDATION EN TEMPS RÉEL =================
    private void setupRealTimeValidation() {
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

        currentUser.setNom(nouveauNom);
        currentUser.setEmail(nouvelEmail);

        String ancien = ancienMdpField.getText();
        String nouveau = nouveauMdpField.getText();
        String confirm = confirmMdpField.getText();

        if (!ancien.isEmpty() || !nouveau.isEmpty() || !confirm.isEmpty()) {
            if (!ValidationUtils.isNotEmpty(ancien) || !ValidationUtils.isNotEmpty(nouveau) || !ValidationUtils.isNotEmpty(confirm)) {
                ValidationUtils.showError(messageLabel, "Veuillez remplir tous les champs du mot de passe");
                return;
            }

            if (!nouveau.equals(confirm)) {
                ValidationUtils.showError(messageLabel, "Les mots de passe ne correspondent pas");
                ValidationUtils.setFieldError(confirmMdpField);
                return;
            }

            if (!ValidationUtils.hasMinLength(nouveau, 6)) {
                ValidationUtils.showError(messageLabel, "Le mot de passe doit contenir au moins 6 caractères");
                ValidationUtils.setFieldError(nouveauMdpField);
                return;
            }

            boolean mdpChange = userService.changerMotDePasse(currentUser.getId(), ancien, nouveau);
            if (!mdpChange) {
                ValidationUtils.showError(messageLabel, "Ancien mot de passe incorrect");
                ValidationUtils.setFieldError(ancienMdpField);
                return;
            }
        }

        userService.modifier(currentUser);
        ValidationUtils.showSuccess(messageLabel, "Profil modifié avec succès !");

        new Thread(() -> {
            try {
                Thread.sleep(1500);
                javafx.application.Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/profilAdmin.fxml"));
                        Parent root = loader.load();

                        ProfilAdminController controller = loader.getController();
                        controller.setUser(currentUser);

                        Stage stage = (Stage) nomField.getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.setTitle("AgriConnect - Mon Profil");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleAnnuler(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profilAdmin.fxml"));
            Parent root = loader.load();

            ProfilAdminController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToDashboard(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/adminDashboard.fxml"));
            Parent root = loader.load();

            DashboardAdminController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));

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
            System.out.println("📂 Chargement de manageUsers.fxml...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/manageUsers.fxml"));
            Parent root = loader.load();

            ManageUsersController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Gestion des utilisateurs");

            System.out.println("✅ Gestion des utilisateurs chargée !");

        } catch (Exception e) {
            System.err.println("❌ ERREUR : " + e.getMessage());
            e.printStackTrace();
            showError("Impossible de charger la gestion des utilisateurs");
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