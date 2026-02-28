package org.example.controllers.User;


import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.example.entities.EtatCompte;
import org.example.entities.Role;
import org.example.entities.User;
import org.example.services.User.UserService;
import org.example.utils.MyDatabase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

public class ManageUsersController {

    // ================= TABLE ET COLONNES =================
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, Role> colRole;
    @FXML private TableColumn<User, EtatCompte> colEtat;
    @FXML private TableColumn<User, Void> colActions;

    // ================= FILTRES =================
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> etatFilter;

    // ================= STATISTIQUES =================
    @FXML private Label totalUsersLabel;
    @FXML private Label actifsLabel;
    @FXML private Label bloquesLabel;
    @FXML private Label nouveauxLabel;
    @FXML private Label messageLabel;

    private User currentUser;
    private UserService userService;
    private ObservableList<User> usersList;
    private ObservableList<User> filteredList;
    private DashboardAdminController dashboardController;
    private AnchorPane contentPane; // ✅ référence au contentPane du dashboard

    // ================= INITIALISATION =================
    @FXML
    public void initialize() {
        userService = new UserService();
        usersList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();

        setupFilters();
        setupTableColumns();
        loadUsers();
        updateStatistics();

        if (messageLabel != null) messageLabel.setText("");

        startAutoRefresh();
    }

    // ================= CONTENT PANE =================
    public void setContentPane(AnchorPane contentPane) {
        this.contentPane = contentPane;
    }

    private void loadInContentPane(Parent root) {
        if (contentPane != null) {
            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);
            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);
        }
    }

    // ================= AUTO-REFRESH =================
    private void startAutoRefresh() {
        Thread refreshThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000);
                    Platform.runLater(() -> {
                        loadUsers();
                        updateStatistics();
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    // ================= CONFIGURATION FILTRES =================
    private void setupFilters() {
        if (roleFilter != null) {
            roleFilter.getItems().addAll("Tous", "AGRICULTEUR", "EXPERT", "FOURNISSEUR", "ADMIN");
            roleFilter.setValue("Tous");
        }
        if (etatFilter != null) {
            etatFilter.getItems().addAll("Tous", "ACTIF", "BLOQUE");
            etatFilter.setValue("Tous");
        }
    }

    // ================= CONFIGURATION COLONNES =================
    private void setupTableColumns() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colEtat.setCellValueFactory(new PropertyValueFactory<>("etatCompte"));

        colRole.setCellFactory(column -> new TableCell<User, Role>() {
            @Override
            protected void updateItem(Role role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(role.toString());
                    switch (role) {
                        case ADMIN -> setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-font-weight: bold; -fx-padding: 5; -fx-background-radius: 5;");
                        case AGRICULTEUR -> setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-weight: bold; -fx-padding: 5; -fx-background-radius: 5;");
                        case EXPERT -> setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-font-weight: bold; -fx-padding: 5; -fx-background-radius: 5;");
                        case FOURNISSEUR -> setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #3730a3; -fx-font-weight: bold; -fx-padding: 5; -fx-background-radius: 5;");
                    }
                }
            }
        });

        colEtat.setCellFactory(column -> new TableCell<User, EtatCompte>() {
            @Override
            protected void updateItem(EtatCompte etat, boolean empty) {
                super.updateItem(etat, empty);
                if (empty || etat == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label();
                    if (etat == EtatCompte.ACTIF) {
                        badge.setText("✓ ACTIF");
                        badge.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-weight: bold; -fx-padding: 5 15; -fx-background-radius: 15;");
                    } else {
                        badge.setText("✕ BLOQUÉ");
                        badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-font-weight: bold; -fx-padding: 5 15; -fx-background-radius: 15;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final MenuButton menuBtn = new MenuButton("⚙");

            {
                menuBtn.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-border-color: #e2e8f0;" +
                                "-fx-border-width: 1.5;" +
                                "-fx-border-radius: 8;" +
                                "-fx-background-radius: 8;" +
                                "-fx-cursor: hand;" +
                                "-fx-font-size: 14px;"
                );

                MenuItem editItem = new MenuItem("Modifier");
                MenuItem blockItem = new MenuItem("Bloquer");
                MenuItem deleteItem = new MenuItem("Supprimer");
                deleteItem.setStyle("-fx-text-fill: #dc2626;");

                editItem.setOnAction(e -> handleEditUser(getTableView().getItems().get(getIndex())));
                blockItem.setOnAction(e -> handleToggleBlockUser(getTableView().getItems().get(getIndex())));
                deleteItem.setOnAction(e -> handleDeleteUser(getTableView().getItems().get(getIndex())));

                menuBtn.getItems().addAll(editItem, new SeparatorMenuItem(), blockItem, new SeparatorMenuItem(), deleteItem);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    // Mettre à jour bloquer/débloquer
                    menuBtn.getItems().stream()
                            .filter(i -> i instanceof MenuItem && ((MenuItem) i).getText() != null
                                    && (((MenuItem) i).getText().contains("Bloquer") || ((MenuItem) i).getText().contains("Débloquer")))
                            .forEach(i -> {
                                if (user.getEtatCompte() == EtatCompte.ACTIF) {
                                    ((MenuItem) i).setText("Bloquer");
                                } else {
                                    ((MenuItem) i).setText("Débloquer");
                                }
                            });
                    HBox hbox = new HBox(menuBtn);
                    hbox.setAlignment(Pos.CENTER);
                    setGraphic(hbox);
                }
            }
        });
    }

    // ================= CHARGER UTILISATEURS =================
    private void loadUsers() {
        usersList.clear();
        try {
            MyDatabase db = new MyDatabase();
            Connection conn = db.getConnection(); // ✅ Correct
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
            usersTable.setItems(filteredList);
        } catch (Exception e) {
            showError("Erreur chargement : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= STATISTIQUES =================
    private void updateStatistics() {
        try {
            Connection conn = MyDataBase.getConnection();
            Statement st = conn.createStatement();

            ResultSet rs = st.executeQuery("SELECT COUNT(*) as total FROM user");
            if (rs.next() && totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(rs.getInt("total")));

            rs = st.executeQuery("SELECT COUNT(*) as total FROM user WHERE etatCompte='ACTIF'");
            if (rs.next() && actifsLabel != null) actifsLabel.setText(String.valueOf(rs.getInt("total")));

            rs = st.executeQuery("SELECT COUNT(*) as total FROM user WHERE etatCompte='BLOQUE'");
            if (rs.next() && bloquesLabel != null) bloquesLabel.setText(String.valueOf(rs.getInt("total")));

            rs = st.executeQuery("SELECT COUNT(*) as total FROM user WHERE date_creation >= DATE_SUB(NOW(), INTERVAL 7 DAY)");
            if (rs.next() && nouveauxLabel != null) nouveauxLabel.setText(String.valueOf(rs.getInt("total")));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= FILTRES =================
    @FXML private void handleSearch() { applyFilters(); }
    @FXML private void handleFilterByRole() { applyFilters(); }
    @FXML private void handleFilterByEtat() { applyFilters(); }

    private void applyFilters() {
        String searchText = (searchField != null) ? searchField.getText().toLowerCase() : "";
        String selectedRole = (roleFilter != null) ? roleFilter.getValue() : "Tous";
        String selectedEtat = (etatFilter != null) ? etatFilter.getValue() : "Tous";

        filteredList.clear();
        for (User user : usersList) {
            boolean matchSearch = searchText.isEmpty() ||
                    user.getNom().toLowerCase().contains(searchText) ||
                    user.getEmail().toLowerCase().contains(searchText);
            boolean matchRole = selectedRole.equals("Tous") || user.getRole().name().equals(selectedRole);
            boolean matchEtat = selectedEtat.equals("Tous") || user.getEtatCompte().name().equals(selectedEtat);

            if (matchSearch && matchRole && matchEtat) filteredList.add(user);
        }
        usersTable.setItems(filteredList);
    }

    @FXML
    private void handleResetFilters() {
        if (searchField != null) searchField.clear();
        if (roleFilter != null) roleFilter.setValue("Tous");
        if (etatFilter != null) etatFilter.setValue("Tous");
        filteredList.setAll(usersList);
        usersTable.setItems(filteredList);
        showSuccess("🔄 Filtres réinitialisés");
    }

    // ================= ACTIONS =================
    @FXML
    private void handleAddUser() {
        System.out.println("🔍 contentPane = " + contentPane); // ← vérifier si null
        if (contentPane == null) {
            showError("❌ contentPane est null !");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/addUser.fxml"));
            Parent root = loader.load();
            AddUserController controller = loader.getController();
            controller.setManageUsersController(this);
            controller.setContentPane(contentPane);
            loadInContentPane(root);
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
            controller.setManageUsersController(this);
            controller.setContentPane(contentPane); // ✅

            loadInContentPane(root); // ✅ dans contentPane

        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDeleteUser(User user) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer l'utilisateur ?");
        confirmation.setContentText("Voulez-vous vraiment supprimer " + user.getNom() + " ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            userService.supprimer(user.getId());
            loadUsers();
            updateStatistics();
            showSuccess("✅ Utilisateur supprimé !");
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
        loadUsers();
        updateStatistics();
    }

    // ================= NAVIGATION =================
    @FXML
    private void handleBackToDashboard(MouseEvent event) {
        if (dashboardController != null) {
            dashboardController.reloadDashboardContent();
        }
    }

    // ================= REFRESH =================
    public void handleRefresh() {
        loadUsers();
        updateStatistics();
    }

    // ================= GETTERS / SETTERS =================
    public void setUser(User user) { this.currentUser = user; }
    public User getCurrentUser() { return currentUser; }
    public void setDashboardController(DashboardAdminController controller) { this.dashboardController = controller; }
    public DashboardAdminController getDashboardController() { return dashboardController; }

    // ================= MESSAGES =================
    private void showError(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
        }
    }

    public void showSuccess(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        }
    }
}
