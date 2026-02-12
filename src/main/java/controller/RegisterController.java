package controller;
import entities.EtatCompte;
import entities.Role;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import service.UserService;
import utils.ValidationUtils;
public class RegisterController {
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ChoiceBox<Role> roleChoice;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;

    private UserService userService;

    @FXML
    public void initialize() {
        userService = new UserService();

        // Remplir le choix de rôle
        roleChoice.getItems().addAll(Role.AGRICULTEUR, Role.EXPERT, Role.FOURNISSEUR);
        roleChoice.setValue(Role.AGRICULTEUR);

        // Validation en temps réel
        setupRealTimeValidation();
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

        // Validation du mot de passe
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                if (ValidationUtils.isValidPassword(newVal)) {
                    ValidationUtils.setFieldSuccess(passwordField);
                } else {
                    ValidationUtils.setFieldError(passwordField);
                }
            } else {
                ValidationUtils.resetFieldStyle(passwordField);
            }
        });

        // Validation de la confirmation
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                if (newVal.equals(passwordField.getText())) {
                    ValidationUtils.setFieldSuccess(confirmPasswordField);
                } else {
                    ValidationUtils.setFieldError(confirmPasswordField);
                }
            } else {
                ValidationUtils.resetFieldStyle(confirmPasswordField);
            }
        });
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

        // Validation du mot de passe
        if (!ValidationUtils.hasMinLength(password, 4)) {
            ValidationUtils.showError(errorLabel, "Le mot de passe doit contenir au moins 4 caractères");
            ValidationUtils.setFieldError(passwordField);
            return;
        }

        // Vérification de la confirmation
        if (!password.equals(confirmPassword)) {
            ValidationUtils.showError(errorLabel, "Les mots de passe ne correspondent pas");
            ValidationUtils.setFieldError(confirmPasswordField);
            return;
        }

        // Créer l'utilisateur
        try {
            User newUser = new User(nom, email, password, role, EtatCompte.ACTIF);
            userService.ajouter(newUser);

            ValidationUtils.showSuccess(errorLabel, "Compte créé avec succès !");

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

    // ================= RETOUR LOGIN =================
    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Connexion");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}