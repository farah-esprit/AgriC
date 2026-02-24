package controller;

import entities.User;
import javafx.application.Platform;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardAgriculteurController {

    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private Label userNameLabel;
    @FXML private Label themeModeText;
    @FXML private Label messageLabel;
    @FXML private Label culturesCountLabel;
    @FXML private Label problemsCountLabel;
    @FXML private Label ordersCountLabel;
    @FXML private AnchorPane contentPane;
    @FXML private ToggleButton themeToggle;
    @FXML private HBox menuHome;
    @FXML private HBox menuCulture;
    @FXML private HBox menuProblemes;
    @FXML private HBox menuForum;
    @FXML private HBox menuReclamation;
    @FXML private HBox menuBoutique;
    @FXML private AnchorPane sidebarExpanded;
    @FXML private AnchorPane sidebarCollapsed;
    private boolean sidebarOpen = false;
    private User currentUser;
    private boolean isDarkMode = false;

    // ✅ Sauvegarder le contenu original
    private List<Node> originalDashboardContent;

    @FXML
    public void initialize() {
        System.out.println("✅ DashboardAgriculteurController initialisé");

        if (dateLabel != null) {
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
            dateLabel.setText(today.format(formatter));
        }

        if (menuHome != null) setActiveMenu(menuHome);

        if (culturesCountLabel != null) culturesCountLabel.setText("0");
        if (problemsCountLabel != null) problemsCountLabel.setText("0");
        if (ordersCountLabel != null) ordersCountLabel.setText("0");

        Platform.runLater(() -> {
            try {
                // ✅ sidebarCollapsed à la place de sidebar
                AnchorPane root = (AnchorPane) sidebarCollapsed.getParent();
                if (root != null && root.getScene() != null) {
                    Stage stage = (Stage) root.getScene().getWindow();
                    Scene scene = root.getScene();
                    stage.setMaximized(true);
                    if (scene.getRoot() instanceof Region r) {
                        r.prefWidthProperty().bind(scene.widthProperty());
                        r.prefHeightProperty().bind(scene.heightProperty());
                    }
                }

                if (contentPane != null) {
                    originalDashboardContent = new ArrayList<>(contentPane.getChildren());
                }

                setupSidebarHover();

            } catch (Exception e) {
                System.err.println("⚠️ Erreur initialisation : " + e.getMessage());
            }
        });
    }

    private void setupSidebarHover() {
        // Quand souris entre dans sidebar collapsed
        sidebarCollapsed.setOnMouseEntered(e -> expandSidebar());

        // Quand souris entre dans sidebar expanded
        sidebarExpanded.setOnMouseEntered(e -> expandSidebar());

        // Quand souris quitte sidebar collapsed
        sidebarCollapsed.setOnMouseExited(e -> {
            // Vérifier si la souris est dans expanded
            if (!sidebarExpanded.isHover()) {
                collapseSidebar();
            }
        });

        // Quand souris quitte sidebar expanded
        sidebarExpanded.setOnMouseExited(e -> {
            if (!sidebarCollapsed.isHover()) {
                collapseSidebar();
            }
        });
    }

    private void expandSidebar() {
        sidebarExpanded.setVisible(true);
        AnchorPane.setLeftAnchor(contentPane, 310.0);
    }

    private void collapseSidebar() {
        sidebarExpanded.setVisible(false);
        AnchorPane.setLeftAnchor(contentPane, 70.0);
    }
    public void setUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) welcomeLabel.setText("Bienvenue, " + user.getNom() + " !");
        if (userNameLabel != null) userNameLabel.setText(user.getNom());
        System.out.println("✅ Utilisateur : " + user.getNom());
    }

    @FXML
    private void handleToggleTheme() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            sidebarCollapsed.setStyle("-fx-background-color: #1a1a1a;");
            sidebarExpanded.setStyle("-fx-background-color: #1a1a1a; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 18, 0, 6, 0);");
            contentPane.setStyle("-fx-background-color: #1e1e1e;");
            if (themeModeText != null) themeModeText.setText("Sombre");
            if (themeToggle != null) {
                themeToggle.setText("☀");
                themeToggle.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 10; -fx-cursor: hand;");
            }
        } else {
            sidebarCollapsed.setStyle("-fx-background-color: #2e4d2e;");
            sidebarExpanded.setStyle("-fx-background-color: #2e4d2e; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 18, 0, 6, 0);");
            contentPane.setStyle("-fx-background-color: #f0f4f0;");
            if (themeModeText != null) themeModeText.setText("Clair");
            if (themeToggle != null) {
                themeToggle.setText("🌙");
                themeToggle.setStyle("-fx-background-color: #f5f8f5; -fx-background-radius: 10; -fx-cursor: hand;");
            }
        }
    }
    private void setActiveMenu(HBox activeMenuItem) {
        String defaultStyle = "-fx-cursor: hand; -fx-background-radius: 12; -fx-padding: 0 20;";
        String activeStyle = "-fx-cursor: hand; -fx-background-color: rgba(255,255,255,0.25); -fx-background-radius: 12; -fx-padding: 0 20;";
        if (menuHome != null) menuHome.setStyle(defaultStyle);
        if (menuCulture != null) menuCulture.setStyle(defaultStyle);
        if (menuProblemes != null) menuProblemes.setStyle(defaultStyle);
        if (menuForum != null) menuForum.setStyle(defaultStyle);
        if (menuReclamation != null) menuReclamation.setStyle(defaultStyle);
        if (menuBoutique != null) menuBoutique.setStyle(defaultStyle);
        if (activeMenuItem != null) activeMenuItem.setStyle(activeStyle);
    }

    @FXML
    private void handleShowHome(MouseEvent event) {
        setActiveMenu(menuHome);
        reloadDashboardContent();
        System.out.println("📍 Accueil");
    }

    @FXML
    private void handleShowCulture(MouseEvent event) {
        setActiveMenu(menuCulture);
        showMessage("🌾 Fonctionnalité Ma culture en développement");
        System.out.println("📍 Ma culture");
    }

    @FXML
    private void handleShowProblems(MouseEvent event) {
        setActiveMenu(menuProblemes);
        showMessage("⚠️ Fonctionnalité Problèmes en développement");
        System.out.println("📍 Problèmes");
    }

    @FXML
    private void handleShowForum(MouseEvent event) {
        setActiveMenu(menuForum);
        showMessage("💬 Fonctionnalité Forum en développement");
        System.out.println("📍 Forum");
    }

    @FXML
    private void handleShowReclamation(MouseEvent event) {
        setActiveMenu(menuReclamation);
        showMessage("🐾 Fonctionnalité Réclamation en développement");
        System.out.println("📍 Réclamation");
    }

    @FXML
    private void handleShowBoutique(MouseEvent event) {
        setActiveMenu(menuBoutique);
        showMessage("🛒 Fonctionnalité Boutique en développement");
        System.out.println("📍 Boutique");
    }

    @FXML
    private void handleGoToProfil(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil.fxml"));
            Parent profilContent = loader.load();
            ProfilController controller = loader.getController();
            controller.setUser(currentUser);
            controller.setDashboardController(this); // ✅ passe le bon controller
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


    // ✅ Restaurer le contenu original du dashboard
    public void reloadDashboardContent() {
        if (contentPane != null && originalDashboardContent != null) {
            contentPane.getChildren().clear();
            contentPane.getChildren().addAll(originalDashboardContent);
            System.out.println("✅ Dashboard agriculteur restauré");
        }
        setActiveMenu(menuHome);
        if (messageLabel != null) messageLabel.setText("");
    }

    // Garde l'ancienne méthode pour compatibilité
    public void reloadDashboard() {
        reloadDashboardContent();
    }

    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }
}