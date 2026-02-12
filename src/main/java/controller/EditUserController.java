package controller;
import entities.EtatCompte;
import entities.Role;
import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import service.UserService;
import utils.ValidationUtils;
public class EditUserController {
    @FXML private TextField idField;
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private ComboBox<String> etatComboBox;
    @FXML private Label messageLabel;

    private UserService userService;
    private DashboardAdminController adminController;
    private ManageUsersController manageUsersController;
    private User currentUser;
    private String originalEmail;

    @FXML
    public void initialize() {
        userService = new UserService();

        roleComboBox.getItems().addAll("AGRICULTEUR", "EXPERT", "FOURNISSEUR", "ADMIN");
        etatComboBox.getItems().addAll("ACTIF", "BLOQUE");

        ValidationUtils.clearMessage(messageLabel);

        // Validation en temps réel
        setupRealTimeValidation();
    }

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

        passwordField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty()) {
                if (ValidationUtils.hasMinLength(newVal, 4)) {
                    ValidationUtils.setFieldSuccess(passwordField);
                } else {
                    ValidationUtils.setFieldError(passwordField);
                }
            } else {
                ValidationUtils.resetFieldStyle(passwordField);
            }
        });
    }

    public void setUser(User user) {
        this.currentUser = user;
        this.originalEmail = user.getEmail();

        idField.setText(String.valueOf(user.getId()));
        nomField.setText(user.getNom());
        emailField.setText(user.getEmail());
        passwordField.setText(user.getMotDePasse());
        roleComboBox.setValue(user.getRole().name());
        etatComboBox.setValue(user.getEtatCompte().name());
    }

    public void setAdminController(DashboardAdminController controller) {
        this.adminController = controller;
    }

    public void setManageUsersController(ManageUsersController controller) {
        this.manageUsersController = controller;
    }

    @FXML
    private void handleSave() {
        String nom = ValidationUtils.sanitize(nomField.getText());
        String email = ValidationUtils.sanitize(emailField.getText());
        String password = passwordField.getText();
        String role = roleComboBox.getValue();
        String etat = etatComboBox.getValue();

        // Validation
        if (!ValidationUtils.isNotEmpty(nom) || !ValidationUtils.isNotEmpty(email) || !ValidationUtils.isNotEmpty(password)) {
            ValidationUtils.showError(messageLabel, "Tous les champs sont obligatoires");
            return;
        }

        if (role == null || etat == null) {
            ValidationUtils.showError(messageLabel, "Veuillez sélectionner un rôle et un état");
            return;
        }

        if (!ValidationUtils.isValidName(nom)) {
            ValidationUtils.showError(messageLabel, "Format de nom invalide");
            ValidationUtils.setFieldError(nomField);
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            ValidationUtils.showError(messageLabel, "Format d'email invalide");
            ValidationUtils.setFieldError(emailField);
            return;
        }

        if (!email.equals(originalEmail) && userService.emailExiste(email)) {
            ValidationUtils.showError(messageLabel, "Cet email est déjà utilisé");
            ValidationUtils.setFieldError(emailField);
            return;
        }

        if (!ValidationUtils.hasMinLength(password, 4)) {
            ValidationUtils.showError(messageLabel, "Le mot de passe doit contenir au moins 4 caractères");
            ValidationUtils.setFieldError(passwordField);
            return;
        }

        try {
            currentUser.setNom(nom);
            currentUser.setEmail(email);
            currentUser.setMotDePasse(password);
            currentUser.setRole(Role.valueOf(role));
            currentUser.setEtatCompte(EtatCompte.valueOf(etat));

            userService.modifier(currentUser);

            if (manageUsersController != null) {
                manageUsersController.showSuccess("✅ Utilisateur modifié avec succès");
                manageUsersController.handleRefresh();
            } else if (adminController != null) {
                adminController.showSuccess("✅ Utilisateur modifié avec succès");
                adminController.handleRefresh();
            }

            handleCancel();

        } catch (Exception e) {
            ValidationUtils.showError(messageLabel, "Erreur lors de la modification");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }
}