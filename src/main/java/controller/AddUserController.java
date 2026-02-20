package controller;
import entities.EtatCompte;
import entities.Role;
import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import service.UserService;
import utils.ValidationUtils;
public class AddUserController {
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private ComboBox<String> etatComboBox;
    @FXML private Label messageLabel;

    private UserService userService;
    private DashboardAdminController adminController;
    private ManageUsersController manageUsersController;

    @FXML
    public void initialize() {
        // Ton code existant
        userService = new UserService();
        roleComboBox.getItems().addAll("AGRICULTEUR", "EXPERT", "FOURNISSEUR", "ADMIN");
        etatComboBox.getItems().addAll("ACTIF", "BLOQUE");
        etatComboBox.setValue("ACTIF");
        ValidationUtils.clearMessage(messageLabel);
        setupRealTimeValidation();

        // Full screen
        Platform.runLater(() -> {
            Stage stage = (Stage) roleComboBox.getScene().getWindow();
            stage.setMaximized(true);
            Scene scene = stage.getScene();
            Parent root = scene.getRoot();
            if (root instanceof Region) {
                ((Region) root).prefWidthProperty().bind(scene.widthProperty());
                ((Region) root).prefHeightProperty().bind(scene.heightProperty());
            }
        });
    }

    private void setupRealTimeValidation() {
        nomField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                ValidationUtils.setFieldSuccess(nomField);
            } else {
                ValidationUtils.resetFieldStyle(nomField);
            }
        });

        emailField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.trim().isEmpty() && ValidationUtils.isValidEmail(newVal)) {
                ValidationUtils.setFieldSuccess(emailField);
            } else if (!newVal.trim().isEmpty()) {
                ValidationUtils.setFieldError(emailField);
            } else {
                ValidationUtils.resetFieldStyle(emailField);
            }
        });

        passwordField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty() && ValidationUtils.hasMinLength(newVal, 4)) {
                ValidationUtils.setFieldSuccess(passwordField);
            } else if (!newVal.isEmpty()) {
                ValidationUtils.setFieldError(passwordField);
            } else {
                ValidationUtils.resetFieldStyle(passwordField);
            }
        });
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

        if (!ValidationUtils.isNotEmpty(nom) || !ValidationUtils.isNotEmpty(email) ||
                !ValidationUtils.isNotEmpty(password) || role == null || etat == null) {
            ValidationUtils.showError(messageLabel, "Tous les champs sont obligatoires");
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            ValidationUtils.showError(messageLabel, "Format d'email invalide");
            ValidationUtils.setFieldError(emailField);
            return;
        }

        if (userService.emailExiste(email)) {
            ValidationUtils.showError(messageLabel, "Cet email existe déjà");
            ValidationUtils.setFieldError(emailField);
            return;
        }

        if (!ValidationUtils.hasMinLength(password, 4)) {
            ValidationUtils.showError(messageLabel, "Le mot de passe doit contenir au moins 4 caractères");
            ValidationUtils.setFieldError(passwordField);
            return;
        }

        try {
            User user = new User(0, nom, email, password, Role.valueOf(role), EtatCompte.valueOf(etat));
            userService.ajouter(user);

            if (manageUsersController != null) {
                manageUsersController.showSuccess("✅ Utilisateur ajouté avec succès");
                manageUsersController.handleRefresh();
            } else if (adminController != null) {
                adminController.showSuccess("✅ Utilisateur ajouté avec succès");
                adminController.handleRefresh();
            }

            handleCancel();

        } catch (Exception e) {
            ValidationUtils.showError(messageLabel, "Erreur lors de l'ajout");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }
}