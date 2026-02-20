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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import service.UserService;
import utils.MyDataBase;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

public class DashboardAdminController {

    @FXML private Label welcomeLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label agriculteursLabel;
    @FXML private Label expertsLabel;
    @FXML private Label fournisseursLabel;
    @FXML private Label comptesBloquesLabel;
    @FXML private Label comptesActifsLabel;
    @FXML private Label nouveauxLabel;
    @FXML private Label messageLabel;

    @FXML private Label themeModeIcon;
    @FXML private Label themeModeText;
    @FXML private AnchorPane sidebar;
    @FXML private ToggleButton themeToggle;

    @FXML private HBox menuDashboard;
    @FXML private HBox menuUsers;

    @FXML private AnchorPane dashboardPane;
    @FXML private AnchorPane usersPane;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, Role> colRole;
    @FXML private TableColumn<User, EtatCompte> colEtat;
    @FXML private TableColumn<User, Void> colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;

    private User currentUser;
    private UserService userService;
    private ObservableList<User> usersList;
    private ObservableList<User> filteredList;
    private boolean isDarkMode = false;

    @FXML
    public void initialize() {
        userService = new UserService();
        usersList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();

        setupRoleFilter();
        setupTableColumns();
        loadStatistics();
        loadUsers();

        if (messageLabel != null) {
            messageLabel.setText("");
        }

        System.out.println("✅ DashboardAdminController initialisé");

        // Full screen
        Platform.runLater(() -> {
            Stage stage = (Stage) totalUsersLabel.getScene().getWindow();
            stage.setMaximized(true);
            Scene scene = stage.getScene();
            Parent root = scene.getRoot();
            if (root instanceof Region) {
                ((Region) root).prefWidthProperty().bind(scene.widthProperty());
                ((Region) root).prefHeightProperty().bind(scene.heightProperty());
            }
        });
    }

    //DÉFINIR L'UTILISATEUR =================
    public void setUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Bienvenue, " + user.getNom() + " !");
        }
        System.out.println("✅ Utilisateur : " + user.getNom() + " (Admin)");
    }

    @FXML
    private void handleToggleTheme() {
        isDarkMode = !isDarkMode;

        if (isDarkMode) {
            // DARK MODE
            sidebar.setStyle("-fx-background-color: #1a1a1a;");
            dashboardPane.setStyle("-fx-background-color: #121212;");

            if (themeModeIcon != null) themeModeIcon.setText("🌙");
            if (themeModeText != null) themeModeText.setText("Sombre");
            if (themeToggle != null) {
                themeToggle.setText("☀️");
                themeToggle.setStyle("-fx-background-color: #424242; -fx-background-radius: 15; -fx-cursor: hand; -fx-font-size: 14px; -fx-text-fill: white;");
            }

            System.out.println("🌙 Mode sombre activé");
        } else {
            // LIGHT MODE
            sidebar.setStyle("-fx-background-color: #388e3c;");
            dashboardPane.setStyle("");

            if (themeModeIcon != null) themeModeIcon.setText("☀️");
            if (themeModeText != null) themeModeText.setText("Clair");
            if (themeToggle != null) {
                themeToggle.setText("🌙");
                themeToggle.setStyle("-fx-background-color: #81c784; -fx-background-radius: 15; -fx-cursor: hand; -fx-font-size: 14px; -fx-text-fill: #388e3c;");
            }
            System.out.println("☀️ Mode clair activé");
        }
    }

    private void setupRoleFilter() {
        if (roleFilter != null) {
            roleFilter.getItems().addAll("Tous", "AGRICULTEUR", "EXPERT", "FOURNISSEUR", "ADMIN");
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

        // Style pour l'état
        colEtat.setCellFactory(column -> new TableCell<User, EtatCompte>() {
            @Override
            protected void updateItem(EtatCompte etat, boolean empty) {
                super.updateItem(etat, empty);
                if (empty || etat == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(etat.toString());
                    if (etat == EtatCompte.ACTIF) {
                        setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Colonne Actions avec boutons
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final Button blockBtn = new Button("🔒");

            {
                editBtn.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3; -fx-padding: 5 10;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3; -fx-padding: 5 10;");
                blockBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3; -fx-padding: 5 10;");

                editBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleEditUser(user);
                });

                deleteBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDeleteUser(user);
                });

                blockBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleToggleBlockUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    if (user.getEtatCompte() == EtatCompte.ACTIF) {
                        blockBtn.setText("🔒");
                        blockBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");
                    } else {
                        blockBtn.setText("🔓");
                        blockBtn.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");
                    }

                    HBox hbox = new HBox(5, editBtn, deleteBtn, blockBtn);
                    hbox.setAlignment(Pos.CENTER);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void loadStatistics() {
        try {
            Connection conn = MyDataBase.getConnection();
            Statement st = conn.createStatement();

            // Total utilisateurs
            ResultSet rs = st.executeQuery("SELECT COUNT(*) as total FROM user");
            if (rs.next() && totalUsersLabel != null) {
                totalUsersLabel.setText(String.valueOf(rs.getInt("total")));
            }

            // Agriculteurs
            rs = st.executeQuery("SELECT COUNT(*) as total FROM user WHERE role='AGRICULTEUR'");
            if (rs.next() && agriculteursLabel != null) {
                agriculteursLabel.setText(String.valueOf(rs.getInt("total")));
            }

            // Experts
            rs = st.executeQuery("SELECT COUNT(*) as total FROM user WHERE role='EXPERT'");
            if (rs.next() && expertsLabel != null) {
                expertsLabel.setText(String.valueOf(rs.getInt("total")));
            }

            // Fournisseurs
            rs = st.executeQuery("SELECT COUNT(*) as total FROM user WHERE role='FOURNISSEUR'");
            if (rs.next() && fournisseursLabel != null) {
                fournisseursLabel.setText(String.valueOf(rs.getInt("total")));
            }

            // Comptes actifs
            rs = st.executeQuery("SELECT COUNT(*) as total FROM user WHERE etatCompte='ACTIF'");
            if (rs.next() && comptesActifsLabel != null) {
                comptesActifsLabel.setText(String.valueOf(rs.getInt("total")));
            }

            // Comptes bloqués
            rs = st.executeQuery("SELECT COUNT(*) as total FROM user WHERE etatCompte='BLOQUE'");
            if (rs.next() && comptesBloquesLabel != null) {
                comptesBloquesLabel.setText(String.valueOf(rs.getInt("total")));
            }

            // Nouveaux utilisateurs (7 derniers jours)
            rs = st.executeQuery("SELECT COUNT(*) as total FROM user WHERE date_creation >= DATE_SUB(NOW(), INTERVAL 7 DAY)");
            if (rs.next() && nouveauxLabel != null) {
                nouveauxLabel.setText(String.valueOf(rs.getInt("total")));
            }

            System.out.println("✅ Statistiques chargées");

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement stats : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadUsers() {
        usersList.clear();
        try {
            Connection conn = MyDataBase.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM user ORDER BY user_id DESC");

            while (rs.next()) {
                User user = new User(
                        rs.getInt("user_id"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("motDePasse"),
                        Role.valueOf(rs.getString("role")),
                        EtatCompte.valueOf(rs.getString("etatCompte"))
                );
                usersList.add(user);
            }

            filteredList.setAll(usersList);
            if (usersTable != null) {
                usersTable.setItems(filteredList);
            }

            System.out.println("✅ Utilisateurs chargés : " + usersList.size());

        } catch (Exception e) {
            showError("Erreur chargement utilisateurs : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        if (searchField == null) return;

        String searchText = searchField.getText().toLowerCase();
        filteredList.clear();

        for (User user : usersList) {
            if (user.getNom().toLowerCase().contains(searchText) ||
                    user.getEmail().toLowerCase().contains(searchText)) {
                filteredList.add(user);
            }
        }

        if (usersTable != null) {
            usersTable.setItems(filteredList);
        }
    }

    @FXML
    private void handleFilterByRole() {
        if (roleFilter == null) return;

        String selectedRole = roleFilter.getValue();
        filteredList.clear();

        if (selectedRole.equals("Tous")) {
            filteredList.setAll(usersList);
        } else {
            for (User user : usersList) {
                if (user.getRole().name().equals(selectedRole)) {
                    filteredList.add(user);
                }
            }
        }

        if (usersTable != null) {
            usersTable.setItems(filteredList);
        }
    }

    @FXML
    private void handleAddUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/addUser.fxml"));
            Parent root = loader.load();

            AddUserController controller = loader.getController();
            controller.setAdminController(this);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Ajouter un utilisateur");
            stage.show();

        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleEditUser(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/editUser.fxml"));
            Parent root = loader.load();

            EditUserController controller = loader.getController();
            controller.setUser(user);
            controller.setAdminController(this);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Modifier l'utilisateur");
            stage.show();

        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDeleteUser(User user) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer l'utilisateur ?");
        confirmation.setContentText("Voulez-vous vraiment supprimer " + user.getNom() + " ?\nCette action est irréversible.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            userService.supprimer(user.getId());
            showSuccess("✅ Utilisateur supprimé !");
            handleRefresh();
        }
    }

    private void handleToggleBlockUser(User user) {
        if (user.getEtatCompte() == EtatCompte.ACTIF) {
            user.setEtatCompte(EtatCompte.BLOQUE);
            showSuccess("🔒 Compte bloqué !");
        } else {
            user.setEtatCompte(EtatCompte.ACTIF);
            showSuccess("🔓 Compte débloqué !");
        }
        userService.modifier(user);
        handleRefresh();
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        handleRefresh();
    }

    public void handleRefresh() {
        loadUsers();
        loadStatistics();
        if (searchField != null) {
            searchField.clear();
        }
        if (roleFilter != null) {
            roleFilter.setValue("Tous");
        }
        showSuccess("Données actualisées !");
    }

    @FXML
    private void openManageUsers(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/managerUsers.fxml"));
            Parent root = loader.load();
            ManageUsersController controller = loader.getController();
            controller.setUser(currentUser);
            Stage stage = (Stage) dashboardPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Gestion des utilisateurs");
            stage.setMaximized(true); // ✅
        } catch (Exception e) {
            showError("Impossible de charger la gestion des utilisateurs");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToProfil(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profilAdmin.fxml"));
            Parent root = loader.load();
            ProfilAdminController controller = loader.getController();
            controller.setUser(currentUser);
            Stage stage = (Stage) dashboardPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Mon Profil");
            stage.setMaximized(true); // ✅
        } catch (Exception e) {
            showError("Impossible de charger le profil");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) dashboardPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Login");
            stage.setMaximized(true); // ✅
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors de la déconnexion");
        }
    }

    @FXML
    private void handleShowDashboard(MouseEvent event) {
        showPane(dashboardPane);
        setActiveMenu(menuDashboard);
    }

    @FXML
    private void handleShowUsers(MouseEvent event) {
        showPane(usersPane);
        setActiveMenu(menuUsers);
    }

    private void showPane(AnchorPane pane) {
        if (dashboardPane != null) dashboardPane.setVisible(false);
        if (usersPane != null) usersPane.setVisible(false);
        if (pane != null) pane.setVisible(true);
    }

    private void setActiveMenu(HBox menu) {
        if (menuDashboard != null) {
            menuDashboard.setStyle("-fx-cursor: hand; -fx-background-radius: 8;");
        }
        if (menuUsers != null) {
            menuUsers.setStyle("-fx-cursor: hand; -fx-background-radius: 8;");
        }
        if (menu != null) {
            menu.setStyle("-fx-cursor: hand; -fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 8;");
        }
    }

    private void showError(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        }
        System.err.println("ERROR: " + message);
    }

    public void showSuccess(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
        }
        System.out.println("SUCCESS: " + message);
    }

    public User getCurrentUser() {
        return currentUser;
    }
}