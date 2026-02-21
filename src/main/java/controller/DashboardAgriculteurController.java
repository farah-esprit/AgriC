package controller;

import entities.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DashboardAgriculteurController {

    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private Label themeModeIcon;
    @FXML private Label themeModeText;
    @FXML private AnchorPane contentPane;
    @FXML private AnchorPane sidebar;
    @FXML private ToggleButton themeToggle;

    @FXML private HBox menuHome;
    @FXML private HBox menuCulture;
    @FXML private HBox menuProblems;
    @FXML private HBox menuForum;
    @FXML private HBox menuReclamation;
    @FXML private HBox menuBoutique;

    private User currentUser;
    private boolean isDarkMode = false;

    // ================= INITIALISATION =================
    @FXML
    public void initialize() {
        System.out.println("✅ DashboardAgriculteurController initialisé");

        if (dateLabel != null) {
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
            dateLabel.setText(today.format(formatter));
        }

        if (menuHome != null) {
            setActiveMenu(menuHome);
        }

        // ✅ LIER LES DIMENSIONS À LA SCENE (COMME DANS LOGIN)
        javafx.application.Platform.runLater(() -> {
            try {
                // Récupérer l'AnchorPane racine
                AnchorPane root = (AnchorPane) sidebar.getParent();

                if (root != null && root.getScene() != null) {
                    Stage stage = (Stage) root.getScene().getWindow();
                    Scene scene = root.getScene();

                    // ✅ MAXIMISER LA FENÊTRE
                    stage.setMaximized(true);

                    // ✅ LIER LES DIMENSIONS (COMME DANS LOGIN)
                    if (scene.getRoot() instanceof javafx.scene.layout.Region r) {
                        r.prefWidthProperty().bind(scene.widthProperty());
                        r.prefHeightProperty().bind(scene.heightProperty());
                    }

                    System.out.println("✅ Dashboard étendu à la fenêtre maximisée");
                }
            } catch (Exception e) {
                System.err.println("⚠️ Erreur : " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // ================= DÉFINIR L'UTILISATEUR =================
    public void setUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Bienvenue, " + user.getNom() + " !");
        }
        System.out.println("✅ Utilisateur : " + user.getNom());
    }

    // ================= DARK MODE =================
    @FXML
    private void handleToggleTheme() {
        isDarkMode = !isDarkMode;

        if (isDarkMode) {
            sidebar.setStyle("-fx-background-color: #1a1a1a;");
            contentPane.setStyle("-fx-background-color: #121212;");

            if (themeModeIcon != null) themeModeIcon.setText("🌙");
            if (themeModeText != null) themeModeText.setText("Sombre");
            if (themeToggle != null) {
                themeToggle.setText("☀️");
                themeToggle.setStyle("-fx-background-color: #424242; -fx-background-radius: 15; -fx-cursor: hand; -fx-font-size: 14px; -fx-text-fill: white;");
            }
        } else {
            sidebar.setStyle("-fx-background-color: #388e3c;");
            contentPane.setStyle("-fx-background-color: #f5f5f5;");

            if (themeModeIcon != null) themeModeIcon.setText("☀️");
            if (themeModeText != null) themeModeText.setText("Clair");
            if (themeToggle != null) {
                themeToggle.setText("🌙");
                themeToggle.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-cursor: hand; -fx-font-size: 14px; -fx-text-fill: #388e3c;");
            }
        }
    }

    // ================= MENU ACTIF =================
    private void setActiveMenu(HBox activeMenuItem) {
        if (menuHome != null) menuHome.setStyle("-fx-background-radius: 8; -fx-cursor: hand;");
        if (menuCulture != null) menuCulture.setStyle("-fx-background-radius: 8; -fx-cursor: hand;");
        if (menuProblems != null) menuProblems.setStyle("-fx-background-radius: 8; -fx-cursor: hand;");
        if (menuForum != null) menuForum.setStyle("-fx-background-radius: 8; -fx-cursor: hand;");
        if (menuReclamation != null) menuReclamation.setStyle("-fx-background-radius: 8; -fx-cursor: hand;");
        if (menuBoutique != null) menuBoutique.setStyle("-fx-background-radius: 8; -fx-cursor: hand;");

        if (activeMenuItem != null) {
            activeMenuItem.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 8; -fx-cursor: hand;");
        }
    }

    // ================= NAVIGATION MENU =================
    @FXML
    private void handleShowHome(MouseEvent event) {
        setActiveMenu(menuHome);
        System.out.println("📍 Accueil");
    }

    @FXML
    private void handleShowCulture(MouseEvent event) {
        setActiveMenu(menuCulture);
        System.out.println("📍 Ma culture");
    }

    @FXML
    private void handleShowProblems(MouseEvent event) {
        setActiveMenu(menuProblems);
        System.out.println("📍 Problèmes");
    }

    @FXML
    private void handleShowForum(MouseEvent event) {
        setActiveMenu(menuForum);
        System.out.println("📍 Forum");
    }

    @FXML
    private void handleShowReclamation(MouseEvent event) {
        setActiveMenu(menuReclamation);
        System.out.println("📍 Réclamation");
    }

    @FXML
    private void handleShowBoutique(MouseEvent event) {
        setActiveMenu(menuBoutique);
        System.out.println("📍 Boutique");
    }

    // ================= PROFIL =================
    @FXML
    private void handleGoToProfil(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil.fxml"));
            Parent profilContent = loader.load();

            ProfilController controller = loader.getController();
            controller.setUser(currentUser);
            controller.setDashboardController(this);

            // ✅ CHARGER DANS LE CONTENTPANE (pas changer de Stage)
            contentPane.getChildren().clear();
            contentPane.getChildren().add(profilContent);

            AnchorPane.setTopAnchor(profilContent, 0.0);
            AnchorPane.setBottomAnchor(profilContent, 0.0);
            AnchorPane.setLeftAnchor(profilContent, 0.0);
            AnchorPane.setRightAnchor(profilContent, 0.0);

            System.out.println("✅ Profil chargé dans contentPane");

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement profil");
            e.printStackTrace();
        }
    }

    // ================= DÉCONNEXION =================
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) contentPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Login");
            stage.setMaximized(false);

            System.out.println("✅ Déconnexion");

        } catch (Exception e) {
            System.err.println("❌ Erreur déconnexion");
            e.printStackTrace();
        }
    }

    // ================= RECHARGER L'ACCUEIL =================
    public void reloadDashboard() {
        // Vider le contentPane pour afficher le contenu par défaut du dashboard
        if (contentPane != null && contentPane.getChildren().size() > 5) {
            // Si on a chargé du contenu externe, on le vide
            while (contentPane.getChildren().size() > 5) {
                contentPane.getChildren().remove(contentPane.getChildren().size() - 1);
            }
        }
        setActiveMenu(menuHome);
        System.out.println("✅ Dashboard rechargé");
    }

    // ================= GETTERS =================
    public User getCurrentUser() {
        return currentUser;
    }
}