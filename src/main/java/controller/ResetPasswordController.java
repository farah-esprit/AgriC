package controller;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import service.PasswordResetService;
import service.UserService;
import utils.ValidationUtils;
public class ResetPasswordController {
    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    private UserService userService;
    private PasswordResetService resetService;

    @FXML
    public void initialize() {
        userService = new UserService();
        resetService = new PasswordResetService();
        ValidationUtils.clearMessage(messageLabel);
        setupRealTimeValidation();
    }

    // ================= VALIDATION EN TEMPS RÉEL =================
    private void setupRealTimeValidation() {
        codeField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty()) {
                if (ValidationUtils.isValidCode(newVal, 6)) {
                    ValidationUtils.setFieldSuccess(codeField);
                } else {
                    ValidationUtils.setFieldError(codeField);
                }
            } else {
                ValidationUtils.resetFieldStyle(codeField);
            }
        });

        newPasswordField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty()) {
                if (ValidationUtils.hasMinLength(newVal, 4)) {
                    ValidationUtils.setFieldSuccess(newPasswordField);
                } else {
                    ValidationUtils.setFieldError(newPasswordField);
                }
            } else {
                ValidationUtils.resetFieldStyle(newPasswordField);
            }
        });

        confirmPasswordField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty()) {
                if (newVal.equals(newPasswordField.getText())) {
                    ValidationUtils.setFieldSuccess(confirmPasswordField);
                } else {
                    ValidationUtils.setFieldError(confirmPasswordField);
                }
            } else {
                ValidationUtils.resetFieldStyle(confirmPasswordField);
            }
        });
    }

    @FXML
    private void handleResetPassword() {
        String code = codeField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validation des champs vides
        if (!ValidationUtils.isNotEmpty(code) || !ValidationUtils.isNotEmpty(newPassword) || !ValidationUtils.isNotEmpty(confirmPassword)) {
            ValidationUtils.showError(messageLabel, "Veuillez remplir tous les champs");
            return;
        }

        // Validation du code
        if (!ValidationUtils.isValidCode(code, 6)) {
            ValidationUtils.showError(messageLabel, "Le code doit contenir 6 chiffres");
            ValidationUtils.setFieldError(codeField);
            return;
        }

        // Validation du mot de passe
        if (!ValidationUtils.hasMinLength(newPassword, 4)) {
            ValidationUtils.showError(messageLabel, "Le mot de passe doit contenir au moins 4 caractères");
            ValidationUtils.setFieldError(newPasswordField);
            return;
        }

        // Vérification de la confirmation
        if (!newPassword.equals(confirmPassword)) {
            ValidationUtils.showError(messageLabel, "Les mots de passe ne correspondent pas");
            ValidationUtils.setFieldError(confirmPasswordField);
            return;
        }

        // Vérifier le code
        Integer userId = resetService.verifyResetCode(code);

        if (userId == null) {
            ValidationUtils.showError(messageLabel, "Code invalide ou expiré");
            ValidationUtils.setFieldError(codeField);
            return;
        }

        // Mettre à jour le mot de passe
        boolean success = userService.updatePassword(userId, newPassword);

        if (success) {
            resetService.markTokenAsUsed(code);
            ValidationUtils.showSuccess(messageLabel, "Mot de passe réinitialisé avec succès !");

            // Redirection après 2 secondes
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(() -> {
                        try {
                            handleBackToLogin();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } else {
            ValidationUtils.showError(messageLabel, "Erreur lors de la réinitialisation");
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Connexion");

        } catch (Exception e) {
            System.err.println("❌ Erreur retour login");
            e.printStackTrace();
        }
    }
}