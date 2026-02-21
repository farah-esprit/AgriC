package controller;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import service.PasswordResetService;
import service.UserService;
import utils.ValidationUtils;
public class ResetPasswordController {
    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Label errorLabel;

    private UserService userService;
    private PasswordResetService resetService;
    private StackPane contentPane;

    public void setContentPane(StackPane contentPane) {
        this.contentPane = contentPane;
    }
    @FXML
    public void initialize() {
        userService = new UserService();
        resetService = new PasswordResetService();
        ValidationUtils.clearMessage(messageLabel);
        setupRealTimeValidation();

        Platform.runLater(() -> {
            Stage stage = (Stage) messageLabel.getScene().getWindow();
            stage.setMaximized(true);
            Scene scene = stage.getScene();
            if (scene.getRoot() instanceof Region r) {
                r.prefWidthProperty().bind(scene.widthProperty());
                r.prefHeightProperty().bind(scene.heightProperty());
            }
        });
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
            // ✅ CHARGER LE FXML COMPLET
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load(); // C'est un HBox

            // ✅ RÉCUPÉRER LE CONTROLLER
            LoginController controller = loader.getController();

            // ✅ EXTRAIRE LE VBOX DU FORMULAIRE (qui est dans le 2ème enfant du HBox)
            if (root instanceof javafx.scene.layout.HBox hbox) {
                // Le HBox contient 2 StackPane : [0] = vert, [1] = blanc
                if (hbox.getChildren().size() >= 2) {
                    javafx.scene.layout.StackPane whitePane = (javafx.scene.layout.StackPane) hbox.getChildren().get(1);

                    // Le StackPane blanc contient le VBox du formulaire
                    if (whitePane.getChildren().size() > 0) {
                        javafx.scene.Node loginForm = whitePane.getChildren().get(0);

                        // ✅ RETIRER LE FORMULAIRE DU STACKPANE BLANC
                        whitePane.getChildren().remove(loginForm);

                        // ✅ METTRE LE FORMULAIRE DANS NOTRE CONTENTPANE
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(loginForm);

                        // ✅ PASSER LE CONTENTPANE AU CONTROLLER
                        controller.contentPane = this.contentPane;

                        System.out.println("✅ Formulaire de login extrait et affiché");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (errorLabel != null) {
                errorLabel.setText("Erreur lors du retour au login");
            }
        }
    }
}