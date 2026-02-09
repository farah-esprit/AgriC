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
    private User currentUser;
    private String originalEmail; // Pour vérifier si l'email a changé

    // ================= INITIALISATION =================
    @FXML
    public void initialize() {
        userService = new UserService();

        // Remplir les ComboBox
        roleComboBox.getItems().addAll("AGRICULTEUR", "EXPERT", "FOURNISSEUR", "ADMIN");
        etatComboBox.getItems().addAll("ACTIF", "BLOQUE");

        messageLabel.setText("");
    }

    // ================= DÉFINIR L'UTILISATEUR À MODIFIER =================
    public void setUser(User user) {
        this.currentUser = user;
        this.originalEmail = user.getEmail();

        // Remplir les champs avec les données existantes
        idField.setText(String.valueOf(user.getId()));
        nomField.setText(user.getNom());
        emailField.setText(user.getEmail());
        passwordField.setText(user.getMotDePasse());
        roleComboBox.setValue(user.getRole().name());
        etatComboBox.setValue(user.getEtatCompte().name());
    }

    // ================= DÉFINIR LE CONTRÔLEUR ADMIN =================
    public void setAdminController(DashboardAdminController controller) {
        this.adminController = controller;
    }

    // ================= ENREGISTRER LES MODIFICATIONS =================
    @FXML
    private void handleSave() {
        // Récupérer les valeurs des champs
        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();
        String etat = etatComboBox.getValue();

        // ===== VALIDATION =====
        if (nom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("❌ Tous les champs sont obligatoires !");
            return;
        }

        if (role == null || etat == null) {
            showError("❌ Veuillez sélectionner un rôle et un état !");
            return;
        }

        if (!isValidEmail(email)) {
            showError("❌ Format d'email invalide !");
            return;
        }

        // Vérifier si l'email a changé et s'il existe déjà
        if (!email.equals(originalEmail) && userService.emailExiste(email)) {
            showError("❌ Cet email est déjà utilisé par un autre utilisateur !");
            return;
        }

        if (password.length() < 4) {
            showError("❌ Le mot de passe doit contenir au moins 4 caractères !");
            return;
        }

        // ===== MISE À JOUR DE L'UTILISATEUR =====
        try {
            currentUser.setNom(nom);
            currentUser.setEmail(email);
            currentUser.setMotDePasse(password);
            currentUser.setRole(Role.valueOf(role));
            currentUser.setEtatCompte(EtatCompte.valueOf(etat));

            // Enregistrer dans la base de données
            userService.modifier(currentUser);

            // Informer le dashboard admin
            if (adminController != null) {
                adminController.showSuccess("✅ Utilisateur modifié avec succès !");
                adminController.handleRefresh();
            }

            // Fermer la fenêtre
            handleCancel();

        } catch (Exception e) {
            showError("❌ Erreur lors de la modification : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= ANNULER ET FERMER =================
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }

    // ================= VALIDATION EMAIL =================
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // ================= AFFICHER MESSAGE D'ERREUR =================
    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
    }

    // ================= AFFICHER MESSAGE DE SUCCÈS =================
    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
    }
}