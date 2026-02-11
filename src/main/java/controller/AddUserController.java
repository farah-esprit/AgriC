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
    private ManageUsersController manageUsersController;

    // ================= INITIALISATION =================
    @FXML
    public void initialize() {
        userService = new UserService();

        // Remplir les ComboBox
        roleComboBox.getItems().addAll("AGRICULTEUR", "EXPERT", "FOURNISSEUR", "ADMIN");
        etatComboBox.getItems().addAll("ACTIF", "BLOQUE");
        etatComboBox.setValue("ACTIF");

        messageLabel.setText("");
    }

    // ================= DÉFINIR LES CONTRÔLEURS =================
    public void setAdminController(DashboardAdminController controller) {
        this.adminController = controller;
    }

    public void setManageUsersController(ManageUsersController controller) {
        this.manageUsersController = controller;
    }

    // ================= ENREGISTRER =================
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

        if (password.length() < 4) {
            showError("❌ Le mot de passe doit contenir au moins 4 caractères !");
            return;
        }

        try {
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

            // Informer le controller approprié
            if (manageUsersController != null) {
                manageUsersController.showSuccess("✅ Utilisateur ajouté avec succès !");
                manageUsersController.handleRefresh();
            } else if (adminController != null) {
                adminController.showSuccess("✅ Utilisateur ajouté avec succès !");
                adminController.handleRefresh();
            }

            handleCancel();

        } catch (Exception e) {
            showError("❌ Erreur lors de l'ajout : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= ANNULER =================
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }

    // ================= VALIDATION EMAIL =================
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // ================= MESSAGES =================
    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
    }
}