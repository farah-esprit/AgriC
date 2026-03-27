package controller;

import entities.User;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardAgriculteurController {

    // ══════════════════════════════════════════════════════════════════════
    // LABELS DASHBOARD
    // ══════════════════════════════════════════════════════════════════════
    @FXML private Label        welcomeLabel;
    @FXML private Label        dateLabel;
    @FXML private Label        userNameLabel;
    @FXML private Label        themeModeText;
    @FXML private Label        messageLabel;
    @FXML private Label        culturesCountLabel;
    @FXML private Label        problemsCountLabel;
    @FXML private Label        ordersCountLabel;

    // ══════════════════════════════════════════════════════════════════════
    // LABELS MÉTÉO
    // ══════════════════════════════════════════════════════════════════════
    @FXML private Label        temperatureLabel;
    @FXML private Label        windspeedLabel;
    @FXML private Label        weatherCodeLabel;

    // ══════════════════════════════════════════════════════════════════════
    // LAYOUT
    // ══════════════════════════════════════════════════════════════════════
    @FXML private AnchorPane   contentPane;
    @FXML private AnchorPane   sidebarExpanded;
    @FXML private AnchorPane   sidebarCollapsed;
    @FXML private ToggleButton themeToggle;

    // ══════════════════════════════════════════════════════════════════════
    // MENUS SIDEBAR
    // ══════════════════════════════════════════════════════════════════════
    @FXML private HBox menuHome;
    @FXML private HBox menuCulture;
    @FXML private HBox menuProblemes;
    @FXML private HBox menuForum;
    @FXML private HBox menuReclamation;
    @FXML private HBox menuBoutique;
    @FXML private HBox menuAnalyse;

    // ══════════════════════════════════════════════════════════════════════
    // ÉTAT INTERNE
    // ══════════════════════════════════════════════════════════════════════
    private User       currentUser;
    private boolean    isDarkMode = false;
    private List<Node> originalDashboardContent;

    // ══════════════════════════════════════════════════════════════════════
    // API MÉTÉO
    // ══════════════════════════════════════════════════════════════════════
    private final String API_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=36.8065&longitude=10.1815&current_weather=true";

    // ══════════════════════════════════════════════════════════════════════
    // INITIALISATION
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        System.out.println("✅ DashboardAgriculteurController initialisé");

        if (dateLabel != null) {
            LocalDate today = LocalDate.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
            dateLabel.setText(today.format(fmt));
        }

        if (menuHome != null) setActiveMenu(menuHome);

        if (culturesCountLabel != null) culturesCountLabel.setText("0");
        if (problemsCountLabel != null) problemsCountLabel.setText("0");
        if (ordersCountLabel   != null) ordersCountLabel.setText("0");

        loadWeather();
        startAutoRefresh();

        Platform.runLater(() -> {
            try {
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
                    System.out.println("✅ Contenu original sauvegardé : "
                            + originalDashboardContent.size() + " éléments");
                }

                setupSidebarHover();

            } catch (Exception e) {
                System.err.println("⚠️ Erreur initialisation : " + e.getMessage());
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // MÉTÉO
    // ══════════════════════════════════════════════════════════════════════
    private void loadWeather() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    URL url = new URL(API_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestMethod("GET");

                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) response.append(line);
                        reader.close();
                        conn.disconnect();

                        JSONObject json    = new JSONObject(response.toString());
                        JSONObject current = json.getJSONObject("current_weather");

                        double temperature = current.getDouble("temperature");
                        double windspeed   = current.getDouble("windspeed");
                        int    weathercode = current.getInt("weathercode");
                        String description = getWeatherDescription(weathercode);

                        Platform.runLater(() -> {
                            if (temperatureLabel != null) temperatureLabel.setText(temperature + " °C");
                            if (windspeedLabel   != null) windspeedLabel.setText(windspeed + " km/h");
                            if (weatherCodeLabel != null) weatherCodeLabel.setText(description);
                        });
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        if (temperatureLabel != null) temperatureLabel.setText("--");
                        if (windspeedLabel   != null) windspeedLabel.setText("--");
                        if (weatherCodeLabel != null) weatherCodeLabel.setText("Erreur API ⚠");
                    });
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private String getWeatherDescription(int code) {
        return switch (code) {
            case 0        -> "Ensoleillé ☀";
            case 1, 2, 3  -> "Partiellement nuageux ⛅";
            case 45, 48   -> "Brouillard 🌫";
            case 51,53,55 -> "Pluie légère 🌦";
            case 61,63,65 -> "Pluie 🌧";
            case 71,73,75 -> "Neige ❄";
            case 95       -> "Orage ⛈";
            default       -> "Conditions inconnues";
        };
    }

    private void startAutoRefresh() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.minutes(30), e -> loadWeather())
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    // ══════════════════════════════════════════════════════════════════════
    // SIDEBAR HOVER
    // ══════════════════════════════════════════════════════════════════════
    private void setupSidebarHover() {
        sidebarCollapsed.setOnMouseEntered(e -> expandSidebar());
        sidebarExpanded.setOnMouseEntered(e  -> expandSidebar());
        sidebarCollapsed.setOnMouseExited(e  -> { if (!sidebarExpanded.isHover()) collapseSidebar(); });
        sidebarExpanded.setOnMouseExited(e   -> { if (!sidebarCollapsed.isHover()) collapseSidebar(); });
    }

    private void expandSidebar() {
        sidebarExpanded.setVisible(true);
        AnchorPane.setLeftAnchor(contentPane, 310.0);
    }

    private void collapseSidebar() {
        sidebarExpanded.setVisible(false);
        AnchorPane.setLeftAnchor(contentPane, 70.0);
    }

    // ══════════════════════════════════════════════════════════════════════
    // SET USER
    // ══════════════════════════════════════════════════════════════════════
    public void setUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) welcomeLabel.setText("Bienvenue, " + user.getNom() + " !");
        if (userNameLabel != null) userNameLabel.setText(user.getNom());
        System.out.println("✅ Utilisateur : " + user.getNom());
    }

    // ══════════════════════════════════════════════════════════════════════
    // THEME TOGGLE
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void handleToggleTheme() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            sidebarCollapsed.setStyle("-fx-background-color: #1a1a1a;");
            sidebarExpanded.setStyle("-fx-background-color: #1a1a1a; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 18, 0, 6, 0);");
            contentPane.setStyle("-fx-background-color: #1e1e1e;");
            if (themeModeText != null) themeModeText.setText("Sombre");
            if (themeToggle   != null) {
                themeToggle.setText("☀");
                themeToggle.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 10; -fx-cursor: hand;");
            }
        } else {
            sidebarCollapsed.setStyle("-fx-background-color: #2e4d2e;");
            sidebarExpanded.setStyle("-fx-background-color: #2e4d2e; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 18, 0, 6, 0);");
            contentPane.setStyle("-fx-background-color: #f0f4f0;");
            if (themeModeText != null) themeModeText.setText("Clair");
            if (themeToggle   != null) {
                themeToggle.setText("🌙");
                themeToggle.setStyle("-fx-background-color: #f5f8f5; -fx-background-radius: 10; -fx-cursor: hand;");
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // MENU ACTIF
    // ══════════════════════════════════════════════════════════════════════
    private void setActiveMenu(HBox activeMenuItem) {
        String def    = "-fx-cursor: hand; -fx-background-radius: 12; -fx-padding: 0 20;";
        String active = "-fx-cursor: hand; -fx-background-color: rgba(255,255,255,0.25); -fx-background-radius: 12; -fx-padding: 0 20;";

        if (menuHome        != null) menuHome.setStyle(def);
        if (menuCulture     != null) menuCulture.setStyle(def);
        if (menuProblemes   != null) menuProblemes.setStyle(def);
        if (menuForum       != null) menuForum.setStyle(def);
        if (menuReclamation != null) menuReclamation.setStyle(def);
        if (menuBoutique    != null) menuBoutique.setStyle(def);
        if (menuAnalyse     != null) menuAnalyse.setStyle(def);

        if (activeMenuItem  != null) activeMenuItem.setStyle(active);
    }

    // ══════════════════════════════════════════════════════════════════════
    // UTILITAIRE — charger un FXML dans le contentPane
    // ══════════════════════════════════════════════════════════════════════
    private FXMLLoader loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();

            contentPane.getChildren().clear();
            contentPane.getChildren().add(content);

            AnchorPane.setTopAnchor(content,    0.0);
            AnchorPane.setBottomAnchor(content, 0.0);
            AnchorPane.setLeftAnchor(content,   0.0);
            AnchorPane.setRightAnchor(content,  0.0);

            System.out.println("✅ Contenu chargé : " + fxmlPath);
            return loader;

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement : " + fxmlPath + " → " + e.getMessage());
            e.printStackTrace();
            showMessage("⚠️ Impossible de charger la page.");
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ACCESSEURS POUR LES CONTRÔLEURS ENFANTS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ✅ Expose le contentPane pour que CultureListController puisse
     * y charger CultureForm directement (sans nouveau Stage).
     */
    public AnchorPane getContentPane() {
        return contentPane;
    }

    /**
     * ✅ Recharge la liste des cultures dans le contentPane.
     * Appelé par CultureController après sauvegarde ou annulation.
     */
    public void handleShowCultureDirect() {
        setActiveMenu(menuCulture);
        FXMLLoader loader = loadContent("/CreerCultureList.fxml");
        if (loader != null) {
            try {
                CultureListController ctrl = loader.getController();
                if (ctrl != null) ctrl.setDashboardController(this);
            } catch (Exception e) {
                System.err.println("⚠️ Erreur rechargement liste cultures : " + e.getMessage());
            }
        }
        System.out.println("📍 Retour liste cultures");
    }

    // ══════════════════════════════════════════════════════════════════════
    // NAVIGATION MENU
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleShowHome(MouseEvent event) {
        setActiveMenu(menuHome);
        reloadDashboardContent();
        System.out.println("📍 Accueil");
    }

    @FXML
    private void handleShowCulture(MouseEvent event) {
        handleShowCultureDirect();
    }

    @FXML
    private void handleShowProblems(MouseEvent event) {
        setActiveMenu(menuProblemes);
        FXMLLoader loader = loadContent("/DiagnosticForm.fxml");
        if (loader != null) {
            try {
                DiagnosticController ctrl = loader.getController();
                if (ctrl != null) {
                    ctrl.setDashboardController(this);
                    if (currentUser != null) ctrl.setUserId(currentUser.getId());
                }
            } catch (Exception e) {
                System.err.println("⚠️ Erreur injection DiagnosticController : " + e.getMessage());
            }
        }
        System.out.println("📍 Problèmes / Diagnostic");
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
        showMessage("📋 Fonctionnalité Réclamation en développement");
        System.out.println("📍 Réclamation");
    }

    @FXML
    private void handleShowBoutique(MouseEvent event) {
        setActiveMenu(menuBoutique);
        showMessage("🛒 Fonctionnalité Boutique en développement");
        System.out.println("📍 Boutique");
    }

    // ══════════════════════════════════════════════════════════════════════
    // ANALYSE / PLANT DISEASE
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    public void goAnalyse(MouseEvent event) {
        setActiveMenu(menuAnalyse);
        System.out.println("🌿 Lancement de l'analyse maladie...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PlantDiseaseApp.fxml"));
            VBox plantDiseaseRoot = loader.load();

            contentPane.getChildren().clear();
            contentPane.getChildren().add(plantDiseaseRoot);

            AnchorPane.setTopAnchor(plantDiseaseRoot,    0.0);
            AnchorPane.setBottomAnchor(plantDiseaseRoot, 0.0);
            AnchorPane.setLeftAnchor(plantDiseaseRoot,   0.0);
            AnchorPane.setRightAnchor(plantDiseaseRoot,  0.0);

            System.out.println("✅ PlantDiseaseApp chargé");
        } catch (IOException e) {
            System.err.println("❌ Erreur chargement PlantDiseaseApp.fxml");
            e.printStackTrace();
            showMessage("❌ Erreur lors du chargement de l'analyse");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // SCANNER PLANTE (PlantNet)
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    public void handleScanPlant(ActionEvent event) {
        System.out.println("🌱 Lancement du scanner PlantNet...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PlantNetView.fxml"));
            VBox plantNetRoot = loader.load();

            PlantNetController plantNetController = loader.getController();
            if (plantNetController != null) {
                plantNetController.setDashboardController(this);
            }

            contentPane.getChildren().clear();
            contentPane.getChildren().add(plantNetRoot);

            AnchorPane.setTopAnchor(plantNetRoot,    0.0);
            AnchorPane.setBottomAnchor(plantNetRoot, 0.0);
            AnchorPane.setLeftAnchor(plantNetRoot,   0.0);
            AnchorPane.setRightAnchor(plantNetRoot,  0.0);

            System.out.println("✅ PlantNet chargé");
        } catch (IOException e) {
            System.err.println("❌ Erreur chargement PlantNetView.fxml");
            e.printStackTrace();
            showMessage("❌ Erreur lors du chargement du scanner");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // PROFIL
    // ══════════════════════════════════════════════════════════════════════
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

            AnchorPane.setTopAnchor(profilContent,    0.0);
            AnchorPane.setBottomAnchor(profilContent, 0.0);
            AnchorPane.setLeftAnchor(profilContent,   0.0);
            AnchorPane.setRightAnchor(profilContent,  0.0);

            System.out.println("✅ Profil chargé");
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement profil");
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // LOGOUT
    // ══════════════════════════════════════════════════════════════════════
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

    // ══════════════════════════════════════════════════════════════════════
    // RELOAD DASHBOARD
    // ══════════════════════════════════════════════════════════════════════
    public void reloadDashboardContent() {
        if (contentPane != null && originalDashboardContent != null) {
            contentPane.getChildren().clear();
            contentPane.getChildren().addAll(originalDashboardContent);
            System.out.println("✅ Dashboard restauré");
            loadWeather();
        }
        setActiveMenu(menuHome);
        if (messageLabel != null) messageLabel.setText("");
    }

    /** Alias pour compatibilité */
    public void reloadDashboard() {
        reloadDashboardContent();
    }

    // ══════════════════════════════════════════════════════════════════════
    // DIAGNOSTIC POUR UNE CULTURE SPÉCIFIQUE
    // ══════════════════════════════════════════════════════════════════════
    public void ouvrirDiagnosticPourCulture(entities.Culture culture) {
        setActiveMenu(menuProblemes);
        FXMLLoader loader = loadContent("/DiagnosticForm.fxml");
        if (loader != null) {
            try {
                DiagnosticController ctrl = loader.getController();
                if (ctrl != null) {
                    ctrl.setDashboardController(this);
                    ctrl.setCulture(culture);
                    if (currentUser != null) ctrl.setUserId(currentUser.getId());
                }
            } catch (Exception e) {
                System.err.println("⚠️ Erreur injection DiagnosticController : " + e.getMessage());
            }
        }
        System.out.println("📍 Diagnostic pour culture : " + culture.getNom());
    }

    // ══════════════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ══════════════════════════════════════════════════════════════════════
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