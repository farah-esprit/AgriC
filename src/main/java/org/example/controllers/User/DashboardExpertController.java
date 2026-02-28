package org.example.controllers.User;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.example.entities.User;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardExpertController {

    // ================= LABELS =================
    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private Label userNameLabel;
    @FXML private Label themeModeText;
    @FXML private Label messageLabel;

    // ================= COMPTEURS =================
    @FXML private Label consultationsCountLabel;
    @FXML private Label questionsCountLabel;
    @FXML private Label articlesCountLabel;

    // ================= LAYOUT =================
    @FXML private AnchorPane contentPane;
    @FXML private AnchorPane sidebar;
    @FXML private ToggleButton themeToggle;

    // ================= MENUS =================
    @FXML private HBox menuHome;
    @FXML private HBox menuConsultations;
    @FXML private HBox menuForum;
    @FXML private HBox menuArticles;
    @FXML private HBox menuRapports;
    @FXML private HBox menuAgenda;

    private User currentUser;
    private boolean isDarkMode = false;

    // ✅ Sauvegarder le contenu original
    private List<Node> originalDashboardContent;

    // ================= INITIALISATION =================
    @FXML
    public void initialize() {
        System.out.println("✅ DashboardExpertController initialisé");

        // Date actuelle
        if (dateLabel != null) {
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
            dateLabel.setText(today.format(formatter));
        }

        // Menu actif par défaut
        if (menuHome != null) {
            setActiveMenu(menuHome);
        }

        // Initialiser les compteurs
        if (consultationsCountLabel != null) consultationsCountLabel.setText("0 en attente");
        if (questionsCountLabel != null) questionsCountLabel.setText("0 répondues");
        if (articlesCountLabel != null) articlesCountLabel.setText("0 publiés");

        // ✅ FULLSCREEN ET BINDING
        Platform.runLater(() -> {
            try {
                AnchorPane root = (AnchorPane) sidebar.getParent();

                if (root != null && root.getScene() != null) {
                    Stage stage = (Stage) root.getScene().getWindow();
                    Scene scene = root.getScene();

                    stage.setMaximized(true);

                    if (scene.getRoot() instanceof Region r) {
                        r.prefWidthProperty().bind(scene.widthProperty());
                        r.prefHeightProperty().bind(scene.heightProperty());
                    }

                    System.out.println("✅ Dashboard Expert étendu à la fenêtre maximisée");
                }

                // ✅ Sauvegarder le contenu original du contentPane
                if (contentPane != null) {
                    originalDashboardContent = new ArrayList<>(contentPane.getChildren());
                    System.out.println("✅ Contenu original sauvegardé : " + originalDashboardContent.size() + " éléments");
                }

            } catch (Exception e) {
                System.err.println("⚠️ Erreur initialisation : " + e.getMessage());
            }
        });
    }

    // ================= DÉFINIR L'UTILISATEUR =================
    public void setUser(User user) {
        this.currentUser = user;

        if (welcomeLabel != null) {
            welcomeLabel.setText("Bienvenue, " + user.getNom() + " !");
        }

        if (userNameLabel != null) {
            userNameLabel.setText(user.getNom());
        }

        System.out.println("✅ Utilisateur Expert : " + user.getNom());
    }

    // ================= DARK MODE =================
    @FXML
    private void handleToggleTheme() {
        isDarkMode = !isDarkMode;

        if (isDarkMode) {
            // Mode sombre
            sidebar.setStyle("-fx-background-color: linear-gradient(180deg, #1a1a1a 0%, #0d0d0d 100%);");
            contentPane.setStyle("-fx-background-color: #1e1e1e;");

            if (themeModeText != null) themeModeText.setText("Sombre");
            if (themeToggle != null) {
                themeToggle.setText("☀️");
                themeToggle.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 15; -fx-cursor: hand;");
            }
        } else {
            // Mode clair
            sidebar.setStyle("-fx-background-color: linear-gradient(180deg, #2e7d32 0%, #1b5e20 100%);");
            contentPane.setStyle("-fx-background-color: #f7f8fc;");

            if (themeModeText != null) themeModeText.setText("Clair");
            if (themeToggle != null) {
                themeToggle.setText("🌙");
                themeToggle.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-background-radius: 15; -fx-cursor: hand;");
            }
        }
    }

    // ================= MENU ACTIF =================
    private void setActiveMenu(HBox activeMenuItem) {
        // Réinitialiser tous les menus
        String defaultStyle = "-fx-cursor: hand; -fx-background-radius: 12; -fx-padding: 0 20;";
        String activeStyle = "-fx-cursor: hand; -fx-background-color: rgba(255,255,255,0.25); -fx-background-radius: 12; -fx-padding: 0 20;";

        if (menuHome != null) menuHome.setStyle(defaultStyle);
        if (menuConsultations != null) menuConsultations.setStyle(defaultStyle);
        if (menuForum != null) menuForum.setStyle(defaultStyle);
        if (menuArticles != null) menuArticles.setStyle(defaultStyle);
        if (menuRapports != null) menuRapports.setStyle(defaultStyle);
        if (menuAgenda != null) menuAgenda.setStyle(defaultStyle);

        // Activer le menu sélectionné
        if (activeMenuItem != null) {
            activeMenuItem.setStyle(activeStyle);
        }
    }

    // ================= NAVIGATION MENU =================
    @FXML
    private void handleShowHome(MouseEvent event) {
        setActiveMenu(menuHome);
        reloadDashboardContent();
        System.out.println("📍 Accueil Expert");
    }

    @FXML
    private void handleShowConsultations(MouseEvent event) {
        setActiveMenu(menuConsultations);
        showMessage("📋 Fonctionnalité Consultations en développement");
        System.out.println("📍 Consultations");
    }

    @FXML
    private void handleShowForum(MouseEvent event) {
        setActiveMenu(menuForum);
        showMessage("💬 Fonctionnalité Forum en développement");
        System.out.println("📍 Forum");
    }

    @FXML
    private void handleShowArticles(MouseEvent event) {
        setActiveMenu(menuArticles);
        showMessage("📝 Fonctionnalité Mes Articles en développement");
        System.out.println("📍 Mes Articles");
    }

    @FXML
    private void handleShowRapports(MouseEvent event) {
        setActiveMenu(menuRapports);
        showMessage("📊 Fonctionnalité Rapports en développement");
        System.out.println("📍 Rapports");
    }

    @FXML
    private void handleShowAgenda(MouseEvent event) {
        setActiveMenu(menuAgenda);
        showMessage("📅 Fonctionnalité Agenda en développement");
        System.out.println("📍 Agenda");
    }

    // ================= PROFIL =================
    @FXML
    private void handleGoToProfil(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil.fxml"));
            Parent profilContent = loader.load();

            ProfilController controller = loader.getController();
            controller.setUser(currentUser);
            controller.setDashboardController(this);

            contentPane.getChildren().clear();
            contentPane.getChildren().add(profilContent);

            AnchorPane.setTopAnchor(profilContent, 0.0);
            AnchorPane.setBottomAnchor(profilContent, 0.0);
            AnchorPane.setLeftAnchor(profilContent, 0.0);
            AnchorPane.setRightAnchor(profilContent, 0.0);

            System.out.println("✅ Profil Expert chargé dans contentPane");

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement profil");
            e.printStackTrace();
        }
    }

    // ================= DÉCONNEXION =================

    @FXML
    private void handleLogout(MouseEvent event) {
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
    public void reloadDashboardContent() {
        if (contentPane != null && originalDashboardContent != null) {
            contentPane.getChildren().clear();
            contentPane.getChildren().addAll(originalDashboardContent);
            System.out.println("✅ Dashboard Expert restauré");
        }
        setActiveMenu(menuHome);
        if (messageLabel != null) messageLabel.setText("");
    }

    // Garde l'ancienne méthode pour compatibilité
    public void reloadDashboard() {
        reloadDashboardContent();
    }

    // ================= MESSAGES =================
    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        }
    }

    // ================= GETTERS =================
    public User getCurrentUser() {
        return currentUser;
    }
}
