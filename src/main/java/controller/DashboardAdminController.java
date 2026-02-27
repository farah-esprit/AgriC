package controller;

import entities.EtatCompte;
import entities.Role;
import entities.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import service.UserService;
import utils.MyDataBase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DashboardAdminController {

    @FXML private Label         welcomeLabel;
    @FXML private Label         dateLabel;
    @FXML private Label         adminNameLabel;
    @FXML private Label         messageLabel;
    @FXML private ToggleButton  themeToggle;

    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label blockedUsersLabel;
    @FXML private Label inactiveUsersLabel;
    @FXML private Label newUsersWeekLabel;
    @FXML private Label newUsersMonthLabel;
    @FXML private Label agriculteursLabel;
    @FXML private Label expertsLabel;
    @FXML private Label fournisseursLabel;
    @FXML private Label adminsLabel;

    @FXML private PieChart                     rolesPieChart;
    @FXML private PieChart                     statusPieChart;
    @FXML private BarChart<String, Number>     monthlySignupsChart;
    @FXML private LineChart<String, Number>    dailyActivityChart;
    @FXML private CategoryAxis                 monthAxis;
    @FXML private NumberAxis                   signupsAxis;
    @FXML private CategoryAxis                 dayAxis;
    @FXML private NumberAxis                   activityAxis;

    @FXML private AnchorPane  sidebarCollapsed;
    @FXML private AnchorPane  sidebarExpanded;
    @FXML private AnchorPane  contentPane;
    @FXML private AnchorPane  dashboardPane;
    @FXML private AnchorPane  usersPane;

    @FXML private HBox menuDashboard;
    @FXML private HBox menuUsers;
    @FXML private HBox menuProfil;

    @FXML private TableView<User>               usersTable;
    @FXML private TableColumn<User, Integer>    colId;
    @FXML private TableColumn<User, String>     colNom;
    @FXML private TableColumn<User, String>     colEmail;
    @FXML private TableColumn<User, Role>       colRole;
    @FXML private TableColumn<User, EtatCompte> colEtat;
    @FXML private TableColumn<User, Void>       colActions;
    @FXML private TextField                     searchField;
    @FXML private ComboBox<String>              roleFilter;

    private User currentUser;
    private UserService userService;
    private ObservableList<User> usersList;
    private ObservableList<User> filteredList;
    private boolean isDarkMode = false;
    private List<Node> originalDashboardContent;

    // =========================================================================
    // INITIALISATION
    // =========================================================================
    @FXML
    public void initialize() {
        userService  = new UserService();
        usersList    = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();

        if (dateLabel != null) {
            dateLabel.setText(LocalDate.now().format(
                    DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)));
        }

        setupRoleFilter();
        setupTableColumns();
        loadUsers();

        if (messageLabel != null) messageLabel.setText("");

        // TOUT le chargement visuel dans Platform.runLater
        // pour garantir que le FXML est complètement rendu
        Platform.runLater(() -> {
            try {
                AnchorPane root = (AnchorPane) sidebarCollapsed.getParent();
                if (root != null && root.getScene() != null) {
                    Stage stage = (Stage) root.getScene().getWindow();
                    stage.setMaximized(true);
                    Scene scene = root.getScene();
                    if (scene.getRoot() instanceof Region r) {
                        r.prefWidthProperty().bind(scene.widthProperty());
                        r.prefHeightProperty().bind(scene.heightProperty());
                    }
                }
                if (contentPane != null)
                    originalDashboardContent = new ArrayList<>(contentPane.getChildren());
                setupSidebarHover();
            } catch (Exception e) {
                System.err.println("⚠️ Init layout : " + e.getMessage());
            }

            // Charger stats et graphiques APRÈS le rendu
            loadStatistics();
            loadCharts();
        });

        System.out.println("✅ DashboardAdminController initialisé");
    }

    // =========================================================================
    // SIDEBAR HOVER
    // =========================================================================
    private void setupSidebarHover() {
        sidebarCollapsed.setOnMouseEntered(e -> expandSidebar());
        sidebarExpanded.setOnMouseEntered(e -> expandSidebar());
        sidebarCollapsed.setOnMouseExited(e -> { if (!sidebarExpanded.isHover()) collapseSidebar(); });
        sidebarExpanded.setOnMouseExited(e ->  { if (!sidebarCollapsed.isHover()) collapseSidebar(); });
    }

    private void expandSidebar()  { sidebarExpanded.setVisible(true);  AnchorPane.setLeftAnchor(contentPane, 310.0); }
    private void collapseSidebar(){ sidebarExpanded.setVisible(false); AnchorPane.setLeftAnchor(contentPane, 70.0);  }

    // =========================================================================
    // SETUSER
    // =========================================================================
    public void setUser(User user) {
        this.currentUser = user;
        if (welcomeLabel   != null) welcomeLabel.setText("Bienvenue, " + user.getNom() + " !");
        if (adminNameLabel != null) adminNameLabel.setText(user.getNom());
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================
    @FXML
    private void handleShowDashboard(MouseEvent event) { reloadDashboardContent(); }

    public void reloadDashboardContent() {
        if (contentPane != null && originalDashboardContent != null) {
            contentPane.getChildren().clear();
            contentPane.getChildren().addAll(originalDashboardContent);
        }
        setActiveMenu(menuDashboard);
        if (messageLabel != null) messageLabel.setText("");
        Platform.runLater(() -> {
            loadStatistics();
            loadCharts();
        });
    }

    public void reloadDashboard() { reloadDashboardContent(); }

    @FXML private void openManageUsers(MouseEvent event) {
        setActiveMenu(menuUsers);
        loadContentInPane("/managerUsers.fxml");
    }

    @FXML private void handleGoToProfil(MouseEvent event) {
        setActiveMenu(menuProfil);
        loadContentInPane("/profilAdmin.fxml");
    }

    @FXML private void handleLogout(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) contentPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Login");
            stage.setMaximized(false);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadContentInPane(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newContent = loader.load();
            Object ctrl = loader.getController();
            if (ctrl instanceof ProfilAdminController c) {
                c.setUser(currentUser); c.setDashboardController(this);
            } else if (ctrl instanceof ManageUsersController c) {
                c.setUser(currentUser); c.setDashboardController(this); c.setContentPane(contentPane);
            }
            AnchorPane target = contentPane != null ? contentPane : dashboardPane;
            target.getChildren().clear();
            target.getChildren().add(newContent);
            AnchorPane.setTopAnchor(newContent,    0.0);
            AnchorPane.setBottomAnchor(newContent, 0.0);
            AnchorPane.setLeftAnchor(newContent,   0.0);
            AnchorPane.setRightAnchor(newContent,  0.0);
        } catch (Exception e) { System.err.println("❌ " + fxmlPath); e.printStackTrace(); }
    }

    // =========================================================================
    // THEME
    // =========================================================================
    @FXML private void handleToggleTheme() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            sidebarCollapsed.setStyle("-fx-background-color: #1a1a1a;");
            sidebarExpanded.setStyle("-fx-background-color: #1a1a1a; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.25),18,0,6,0);");
            contentPane.setStyle("-fx-background-color: #1e1e1e;");
            if (themeToggle != null) themeToggle.setText("☀");
        } else {
            sidebarCollapsed.setStyle("-fx-background-color: #388e3c;");
            sidebarExpanded.setStyle("-fx-background-color: #388e3c; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.22),18,0,6,0);");
            contentPane.setStyle("-fx-background-color: #f0f4f0;");
            if (themeToggle != null) themeToggle.setText("🌙");
        }
    }

    private void setActiveMenu(HBox active) {
        String def = "-fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 0 16;";
        String act = "-fx-cursor: hand; -fx-background-color: rgba(255,255,255,0.25); -fx-background-radius: 10; -fx-padding: 0 16;";
        if (menuDashboard != null) menuDashboard.setStyle(def);
        if (menuUsers     != null) menuUsers.setStyle(def);
        if (menuProfil    != null) menuProfil.setStyle(def);
        if (active        != null) active.setStyle(act);
    }

    // =========================================================================
    // STATISTIQUES
    // =========================================================================
    private void loadStatistics() {
        // Exécuter en background thread pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                Connection conn = MyDataBase.getConnection();
                Statement st = conn.createStatement();

                int total = 0, actifs = 0, bloques = 0, inactifs = 0;
                int semaine = 0, mois = 0, agri = 0, exp = 0, fourn = 0, adm = 0;

                ResultSet rs;
                rs = st.executeQuery("SELECT COUNT(*) as t FROM user");
                if (rs.next()) total = rs.getInt("t");

                rs = st.executeQuery("SELECT COUNT(*) as t FROM user WHERE etatCompte='ACTIF'");
                if (rs.next()) actifs = rs.getInt("t");

                rs = st.executeQuery("SELECT COUNT(*) as t FROM user WHERE etatCompte='BLOQUE'");
                if (rs.next()) bloques = rs.getInt("t");

                rs = st.executeQuery("SELECT COUNT(*) as t FROM user WHERE etatCompte='INACTIF'");
                if (rs.next()) inactifs = rs.getInt("t");

                rs = st.executeQuery("SELECT COUNT(*) as t FROM user WHERE date_creation >= DATE_SUB(NOW(), INTERVAL 7 DAY)");
                if (rs.next()) semaine = rs.getInt("t");

                rs = st.executeQuery("SELECT COUNT(*) as t FROM user WHERE date_creation >= DATE_SUB(NOW(), INTERVAL 30 DAY)");
                if (rs.next()) mois = rs.getInt("t");

                rs = st.executeQuery("SELECT COUNT(*) as t FROM user WHERE role='AGRICULTEUR'");
                if (rs.next()) agri = rs.getInt("t");

                rs = st.executeQuery("SELECT COUNT(*) as t FROM user WHERE role='EXPERT'");
                if (rs.next()) exp = rs.getInt("t");

                rs = st.executeQuery("SELECT COUNT(*) as t FROM user WHERE role='FOURNISSEUR'");
                if (rs.next()) fourn = rs.getInt("t");

                rs = st.executeQuery("SELECT COUNT(*) as t FROM user WHERE role='ADMIN'");
                if (rs.next()) adm = rs.getInt("t");

                final int fTotal=total, fActifs=actifs, fBloques=bloques, fInactifs=inactifs;
                final int fSemaine=semaine, fMois=mois, fAgri=agri, fExp=exp, fFourn=fourn, fAdm=adm;

                Platform.runLater(() -> {
                    if (totalUsersLabel    != null) totalUsersLabel.setText(String.valueOf(fTotal));
                    if (activeUsersLabel   != null) activeUsersLabel.setText(String.valueOf(fActifs));
                    if (blockedUsersLabel  != null) blockedUsersLabel.setText(String.valueOf(fBloques));
                    if (inactiveUsersLabel != null) inactiveUsersLabel.setText(String.valueOf(fInactifs));
                    if (newUsersWeekLabel  != null) newUsersWeekLabel.setText(String.valueOf(fSemaine));
                    if (newUsersMonthLabel != null) newUsersMonthLabel.setText(String.valueOf(fMois));
                    if (agriculteursLabel  != null) agriculteursLabel.setText(String.valueOf(fAgri));
                    if (expertsLabel       != null) expertsLabel.setText(String.valueOf(fExp));
                    if (fournisseursLabel  != null) fournisseursLabel.setText(String.valueOf(fFourn));
                    if (adminsLabel        != null) adminsLabel.setText(String.valueOf(fAdm));
                    System.out.println("✅ Stats affichées : total=" + fTotal);
                });

            } catch (Exception e) {
                System.err.println("❌ Erreur loadStatistics : " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // =========================================================================
    // GRAPHIQUES
    // =========================================================================
    private void loadCharts() {
        loadRolesChart();
        loadStatusChart();
        loadMonthlySignups();
        loadDailyActivity();
    }

    private void loadRolesChart() {
        if (rolesPieChart == null) return;
        new Thread(() -> {
            try {
                Connection conn = MyDataBase.getConnection();
                ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT role, COUNT(*) as c FROM user GROUP BY role");
                Map<String, Integer> map = new LinkedHashMap<>();
                map.put("Agriculteurs",0); map.put("Experts",0);
                map.put("Fournisseurs",0); map.put("Admins",0);
                while (rs.next()) {
                    switch (rs.getString("role")) {
                        case "AGRICULTEUR" -> map.put("Agriculteurs", rs.getInt("c"));
                        case "EXPERT"      -> map.put("Experts",      rs.getInt("c"));
                        case "FOURNISSEUR" -> map.put("Fournisseurs", rs.getInt("c"));
                        case "ADMIN"       -> map.put("Admins",       rs.getInt("c"));
                    }
                }
                ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
                map.forEach((k,v) -> { if (v > 0) data.add(new PieChart.Data(k + " (" + v + ")", v)); });
                Platform.runLater(() -> {
                    rolesPieChart.setData(data);
                    rolesPieChart.setTitle("Répartition par Rôle");
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void loadStatusChart() {
        if (statusPieChart == null) return;
        new Thread(() -> {
            try {
                Connection conn = MyDataBase.getConnection();
                ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT etatCompte, COUNT(*) as c FROM user GROUP BY etatCompte");
                Map<String, Integer> map = new LinkedHashMap<>();
                map.put("Actifs",0); map.put("Bloqués",0); map.put("Inactifs",0);
                while (rs.next()) {
                    switch (rs.getString("etatCompte")) {
                        case "ACTIF"   -> map.put("Actifs",   rs.getInt("c"));
                        case "BLOQUE"  -> map.put("Bloqués",  rs.getInt("c"));
                        case "INACTIF" -> map.put("Inactifs", rs.getInt("c"));
                    }
                }
                ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
                map.forEach((k,v) -> { if (v > 0) data.add(new PieChart.Data(k + " (" + v + ")", v)); });
                Platform.runLater(() -> {
                    statusPieChart.setData(data);
                    statusPieChart.setTitle("Répartition par Statut");
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void loadMonthlySignups() {
        if (monthlySignupsChart == null) return;
        new Thread(() -> {
            try {
                Connection conn = MyDataBase.getConnection();
                ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT DATE_FORMAT(date_creation,'%Y-%m') as m, COUNT(*) as c " +
                                "FROM user WHERE date_creation >= DATE_SUB(NOW(), INTERVAL 6 MONTH) " +
                                "GROUP BY m ORDER BY m");
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Inscriptions");
                String[] mois = {"","Jan","Fév","Mar","Avr","Mai","Juin","Juil","Aoû","Sep","Oct","Nov","Déc"};
                while (rs.next()) {
                    String[] p = rs.getString("m").split("-");
                    String label = mois[Integer.parseInt(p[1])] + " " + p[0];
                    int count = rs.getInt("c");
                    series.getData().add(new XYChart.Data<>(label, count));
                }
                Platform.runLater(() -> {
                    monthlySignupsChart.getData().clear();
                    monthlySignupsChart.getData().add(series);
                    monthlySignupsChart.setTitle("Inscriptions (6 derniers mois)");
                    monthlySignupsChart.setCategoryGap(40);
                    monthlySignupsChart.setBarGap(4);
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void loadDailyActivity() {
        if (dailyActivityChart == null) return;
        new Thread(() -> {
            try {
                Connection conn = MyDataBase.getConnection();
                ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT DATE_FORMAT(date_creation,'%Y-%m-%d') as d, COUNT(*) as c " +
                                "FROM user WHERE date_creation >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                                "GROUP BY d ORDER BY d");
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Inscriptions quotidiennes");
                String[] mois = {"","Jan","Fév","Mar","Avr","Mai","Juin","Juil","Aoû","Sep","Oct","Nov","Déc"};
                while (rs.next()) {
                    String[] p = rs.getString("d").split("-");
                    series.getData().add(new XYChart.Data<>(p[2] + " " + mois[Integer.parseInt(p[1])], rs.getInt("c")));
                }
                Platform.runLater(() -> {
                    dailyActivityChart.getData().clear();
                    dailyActivityChart.getData().add(series);
                    dailyActivityChart.setTitle("Activité (30 derniers jours)");
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // =========================================================================
    // TABLE UTILISATEURS
    // =========================================================================
    private void setupRoleFilter() {
        if (roleFilter != null) {
            roleFilter.getItems().addAll("Tous","AGRICULTEUR","EXPERT","FOURNISSEUR","ADMIN");
            roleFilter.setValue("Tous");
        }
    }

    private void setupTableColumns() {
        if (usersTable == null) return;
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colEtat.setCellValueFactory(new PropertyValueFactory<>("etatCompte"));
        colEtat.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(EtatCompte e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) { setText(null); setStyle(""); }
                else { setText(e.toString()); setStyle(e == EtatCompte.ACTIF
                        ? "-fx-text-fill: #4caf50; -fx-font-weight: bold;"
                        : "-fx-text-fill: #d32f2f; -fx-font-weight: bold;"); }
            }
        });
        colActions.setCellFactory(p -> new TableCell<>() {
            final Button edit = new Button("✏️"), del = new Button("🗑️"), blk = new Button("🔒");
            {
                edit.setStyle("-fx-background-color:#2196f3;-fx-text-fill:white;-fx-cursor:hand;-fx-background-radius:3;-fx-padding:5 10;");
                del.setStyle("-fx-background-color:#f44336;-fx-text-fill:white;-fx-cursor:hand;-fx-background-radius:3;-fx-padding:5 10;");
                blk.setStyle("-fx-background-color:#ff9800;-fx-text-fill:white;-fx-cursor:hand;-fx-background-radius:3;-fx-padding:5 10;");
                edit.setOnAction(e -> handleEditUser(getTableView().getItems().get(getIndex())));
                del.setOnAction(e  -> handleDeleteUser(getTableView().getItems().get(getIndex())));
                blk.setOnAction(e  -> handleToggleBlockUser(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                User u = getTableView().getItems().get(getIndex());
                blk.setText(u.getEtatCompte() == EtatCompte.ACTIF ? "🔒" : "🔓");
                HBox h = new HBox(5, edit, del, blk); h.setAlignment(Pos.CENTER); setGraphic(h);
            }
        });
    }

    private void loadUsers() {
        usersList.clear();
        try {
            Connection conn = MyDataBase.getConnection();
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM user ORDER BY user_id DESC");
            while (rs.next())
                usersList.add(new User(
                        rs.getInt("user_id"), rs.getString("nom"), rs.getString("email"),
                        rs.getString("motDePasse"), Role.valueOf(rs.getString("role")),
                        EtatCompte.valueOf(rs.getString("etatCompte"))));
            filteredList.setAll(usersList);
            if (usersTable != null) usersTable.setItems(filteredList);
        } catch (Exception e) { showError("Erreur chargement : " + e.getMessage()); e.printStackTrace(); }
    }

    @FXML private void handleSearch() {
        if (searchField == null) return;
        String s = searchField.getText().toLowerCase();
        filteredList.clear();
        for (User u : usersList)
            if (u.getNom().toLowerCase().contains(s) || u.getEmail().toLowerCase().contains(s))
                filteredList.add(u);
        if (usersTable != null) usersTable.setItems(filteredList);
    }

    @FXML private void handleFilterByRole() {
        if (roleFilter == null) return;
        String role = roleFilter.getValue(); filteredList.clear();
        if (role.equals("Tous")) filteredList.setAll(usersList);
        else for (User u : usersList) if (u.getRole().name().equals(role)) filteredList.add(u);
        if (usersTable != null) usersTable.setItems(filteredList);
    }

    @FXML private void handleAddUser() {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/addUser.fxml"));
            Parent r = l.load(); ((AddUserController)l.getController()).setAdminController(this);
            Stage s = new Stage(); s.setScene(new Scene(r)); s.setTitle("Ajouter"); s.show();
        } catch (Exception e) { showError(e.getMessage()); }
    }

    private void handleEditUser(User user) {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/editUser.fxml"));
            Parent r = l.load(); EditUserController c = l.getController();
            c.setUser(user); c.setAdminController(this);
            Stage s = new Stage(); s.setScene(new Scene(r)); s.setTitle("Modifier"); s.show();
        } catch (Exception e) { showError(e.getMessage()); }
    }

    private void handleDeleteUser(User user) {
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer " + user.getNom() + " ?", ButtonType.OK, ButtonType.CANCEL);
        conf.showAndWait().filter(b -> b == ButtonType.OK)
                .ifPresent(b -> { userService.supprimer(user.getId()); showSuccess("✅ Supprimé !"); handleRefresh(); });
    }

    private void handleToggleBlockUser(User user) {
        user.setEtatCompte(user.getEtatCompte() == EtatCompte.ACTIF ? EtatCompte.BLOQUE : EtatCompte.ACTIF);
        userService.modifier(user);
        showSuccess(user.getEtatCompte() == EtatCompte.BLOQUE ? "🔒 Bloqué !" : "🔓 Débloqué !");
        handleRefresh();
    }

    @FXML public void handleRefresh(ActionEvent e) { handleRefresh(); }
    public void handleRefresh() {
        loadUsers();
        if (searchField != null) searchField.clear();
        if (roleFilter  != null) roleFilter.setValue("Tous");
        Platform.runLater(() -> { loadStatistics(); loadCharts(); });
        showSuccess("Actualisé !");
    }

    @FXML private void handleShowUsers(MouseEvent e) { setActiveMenu(menuUsers); loadUsers(); }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================
    public AnchorPane getContentPane()  { return contentPane; }
    public User        getCurrentUser() { return currentUser; }

    public void showSuccess(String msg) {
        if (messageLabel != null) { messageLabel.setText(msg); messageLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;"); }
    }
    private void showError(String msg) {
        if (messageLabel != null) { messageLabel.setText(msg); messageLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;"); }
    }
}