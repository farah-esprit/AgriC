package controller;
import entities.EtatCompte;
import entities.Role;
import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import service.EmailService;
import service.UserService;
import utils.ValidationUtils;

import static utils.ValidationUtils.showSuccess;

public class RegisterController {
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordTextField;
    @FXML private ChoiceBox<Role> roleChoice;
    @FXML private Label errorLabel;
    @FXML private Label passwordStrengthLabel;
    @FXML private Button registerButton;
    @FXML private Button togglePasswordBtn;
    @FXML private Button toggleConfirmPasswordBtn;
    private UserService userService;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private StackPane contentPane;

    public void setContentPane(StackPane contentPane) {
        this.contentPane = contentPane;
    }
    @FXML
    public void initialize() {
        userService = new UserService();
        roleChoice.getItems().addAll(Role.AGRICULTEUR, Role.EXPERT, Role.FOURNISSEUR);
        roleChoice.setValue(Role.AGRICULTEUR);
        bindPasswordFields();
        setupRealTimeValidation();

        Platform.runLater(() -> {
            Stage stage = (Stage) roleChoice.getScene().getWindow();
            stage.setMaximized(true);
            Scene scene = stage.getScene();
            if (scene.getRoot() instanceof Region r) {
                r.prefWidthProperty().bind(scene.widthProperty());
                r.prefHeightProperty().bind(scene.heightProperty());
            }
        });
    }
    // ================= SYNCHRONISATION MOT DE PASSE =================
    private void bindPasswordFields() {
        // Synchroniser passwordField et passwordTextField
        passwordField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.equals(passwordTextField.getText())) {
                passwordTextField.setText(newVal);
            }
        });

        passwordTextField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.equals(passwordField.getText())) {
                passwordField.setText(newVal);
            }
        });

        // Synchroniser confirmPasswordField et confirmPasswordTextField
        confirmPasswordField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.equals(confirmPasswordTextField.getText())) {
                confirmPasswordTextField.setText(newVal);
            }
        });

        confirmPasswordTextField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.equals(confirmPasswordField.getText())) {
                confirmPasswordField.setText(newVal);
            }
        });
    }

    // ================= TOGGLE VISIBILITÉ MOT DE PASSE =================
    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;

        if (isPasswordVisible) {
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordBtn.setText("🙈");
        } else {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
            togglePasswordBtn.setText("👁️");
        }
    }

    @FXML
    private void toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible;

        if (isConfirmPasswordVisible) {
            confirmPasswordTextField.setVisible(true);
            confirmPasswordTextField.setManaged(true);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
            toggleConfirmPasswordBtn.setText("🙈");
        } else {
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);
            confirmPasswordTextField.setVisible(false);
            confirmPasswordTextField.setManaged(false);
            toggleConfirmPasswordBtn.setText("👁️");
        }
    }

    // ================= VALIDATION EN TEMPS RÉEL =================
    private void setupRealTimeValidation() {
        // Validation du nom
        nomField.textProperty().addListener((obs, oldVal, newVal) -> {
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

        // Validation de l'email
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
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

        // Validation mot de passe (sur les deux champs)
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> validatePassword(newVal));
        passwordTextField.textProperty().addListener((obs, oldVal, newVal) -> validatePassword(newVal));

        // Validation confirmation
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validateConfirmPassword(newVal));
        confirmPasswordTextField.textProperty().addListener((obs, oldVal, newVal) -> validateConfirmPassword(newVal));
    }

    private void validatePassword(String password) {
        if (!password.isEmpty()) {
            if (ValidationUtils.isStrongPassword(password)) {
                ValidationUtils.setFieldSuccess(passwordField);
                ValidationUtils.setFieldSuccess(passwordTextField);
                showPasswordStrength("✅ Mot de passe fort", "#4caf50");
            } else if (ValidationUtils.isValidPassword(password)) {
                ValidationUtils.setFieldError(passwordField);
                ValidationUtils.setFieldError(passwordTextField);
                showPasswordStrength("⚠️ Mot de passe faible - Ajoutez majuscule, minuscule et chiffre", "#ff9800");
            } else {
                ValidationUtils.setFieldError(passwordField);
                ValidationUtils.setFieldError(passwordTextField);
                showPasswordStrength("❌ Minimum 8 caractères", "#d32f2f");
            }
        } else {
            ValidationUtils.resetFieldStyle(passwordField);
            ValidationUtils.resetFieldStyle(passwordTextField);
            hidePasswordStrength();
        }
    }

    private void validateConfirmPassword(String confirmPassword) {
        String password = passwordField.getText();
        if (!confirmPassword.isEmpty()) {
            if (confirmPassword.equals(password)) {
                ValidationUtils.setFieldSuccess(confirmPasswordField);
                ValidationUtils.setFieldSuccess(confirmPasswordTextField);
            } else {
                ValidationUtils.setFieldError(confirmPasswordField);
                ValidationUtils.setFieldError(confirmPasswordTextField);
            }
        } else {
            ValidationUtils.resetFieldStyle(confirmPasswordField);
            ValidationUtils.resetFieldStyle(confirmPasswordTextField);
        }
    }

    // ================= AFFICHER LA FORCE DU MOT DE PASSE =================
    private void showPasswordStrength(String message, String color) {
        if (passwordStrengthLabel != null) {
            passwordStrengthLabel.setText(message);
            passwordStrengthLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");
            passwordStrengthLabel.setVisible(true);
        }
    }

    private void hidePasswordStrength() {
        if (passwordStrengthLabel != null) {
            passwordStrengthLabel.setVisible(false);
        }
    }

    // ================= INSCRIPTION =================
    @FXML
    private void handleRegister() {
        String nom = ValidationUtils.sanitize(nomField.getText());
        String email = ValidationUtils.sanitize(emailField.getText());
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        Role role = roleChoice.getValue();

        // Validation des champs vides
        if (!ValidationUtils.isNotEmpty(nom) || !ValidationUtils.isNotEmpty(email) ||
                !ValidationUtils.isNotEmpty(password) || !ValidationUtils.isNotEmpty(confirmPassword)) {
            ValidationUtils.showError(errorLabel, "Veuillez remplir tous les champs");
            return;
        }

        // Validation du nom
        if (!ValidationUtils.isValidName(nom)) {
            ValidationUtils.showError(errorLabel, "Le nom doit contenir entre 2 et 50 caractères (lettres uniquement)");
            ValidationUtils.setFieldError(nomField);
            return;
        }

        // Validation de l'email
        if (!ValidationUtils.isValidEmail(email)) {
            ValidationUtils.showError(errorLabel, "Format d'email invalide");
            ValidationUtils.setFieldError(emailField);
            return;
        }

        // Vérifier si l'email existe
        if (userService.emailExiste(email)) {
            ValidationUtils.showError(errorLabel, "Cet email est déjà utilisé");
            ValidationUtils.setFieldError(emailField);
            return;
        }

        // Validation mot de passe minimum 8 caractères
        if (!ValidationUtils.isValidPassword(password)) {
            ValidationUtils.showError(errorLabel, "Le mot de passe doit contenir au moins 8 caractères");
            ValidationUtils.setFieldError(passwordField);
            ValidationUtils.setFieldError(passwordTextField);
            return;
        }

        // Vérification mot de passe fort (avertissement)
        if (!ValidationUtils.isStrongPassword(password)) {
            ValidationUtils.showWarning(errorLabel, "⚠️ Mot de passe faible : Ajoutez majuscule, minuscule et chiffre pour plus de sécurité");
        }

        // Vérification de la confirmation
        if (!password.equals(confirmPassword)) {
            ValidationUtils.showError(errorLabel, "Les mots de passe ne correspondent pas");
            ValidationUtils.setFieldError(confirmPasswordField);
            ValidationUtils.setFieldError(confirmPasswordTextField);
            return;
        }

        // Créer l'utilisateur
        try {
            User newUser = new User(nom, email, password, role, EtatCompte.ACTIF);
            userService.ajouter(newUser);

            // ✅ ENVOYER EMAIL DE BIENVENUE
            EmailService emailService = new EmailService();
            emailService.sendWelcomeEmail(email, nom);

            showSuccess(errorLabel, "✅ Compte créé ! Consultez votre email.");
            // Redirection après 1.5 secondes
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(() -> handleBackToLogin());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            ValidationUtils.showError(errorLabel, "Erreur lors de la création du compte");
            e.printStackTrace();
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