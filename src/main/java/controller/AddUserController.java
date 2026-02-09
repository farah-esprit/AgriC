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

public class AddUserController {

    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private ComboBox<String> etatComboBox;
    @FXML private Label messageLabel;

    private UserService userService;
    private DashboardAdminController adminController;

    @FXML
    public void initialize() {
        userService = new UserService();

        // Remplir les ComboBox
        roleComboBox.getItems().addAll("AGRICULTEUR", "EXPERT", "FOURNISSEUR", "ADMIN");
        etatComboBox.getItems().addAll("ACTIF", "BLOQUE");
        etatComboBox.setValue("ACTIF");

        messageLabel.setText("");
    }

    public void setAdminController(DashboardAdminController controller) {
        this.adminController = controller;
    }

    @FXML
    private void handleSave() {
        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();
        String etat = etatComboBox.getValue();

        // Validation
        if (nom.isEmpty() || email.isEmpty() || password.isEmpty() || role == null || etat == null) {
            showError("❌ Tous les champs sont obligatoires !");
            return;
        }

        if (!isValidEmail(email)) {
            showError("❌ Format d'email invalide !");
            return;
        }

        if (userService.emailExiste(email)) {
            showError("❌ Cet email existe déjà !");
            return;
        }

        // Créer l'utilisateur
        User user = new User(
                0,
                nom,
                email,
                password,
                Role.valueOf(role),
                EtatCompte.valueOf(etat)
        );

        userService.ajouter(user);
        adminController.showSuccess("✅ Utilisateur ajouté avec succès !");
        adminController.handleRefresh();
        handleCancel();
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
    }
}