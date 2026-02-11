package controller;

import entities.EtatCompte;
import entities.Role;
import entities.User;
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
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import service.UserService;
import utils.DataBase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

public class ManageUsersController {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colEtat;
    @FXML private TableColumn<User, Void> colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private Label messageLabel;

    private User currentUser;
    private UserService userService;
    private ObservableList<User> usersList;
    private ObservableList<User> filteredList;

    // ================= INITIALISATION =================
    @FXML
    public void initialize() {
        userService = new UserService();
        usersList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();

        setupRoleFilter();
        setupTableColumns();
        loadUsers();

        if (messageLabel != null) {
            messageLabel.setText("");
        }
    }

    // ================= DÉFINIR L'UTILISATEUR =================
    public void setUser(User user) {
        this.currentUser = user;
    }

    // ================= CONFIGURATION FILTRES =================
    private void setupRoleFilter() {
        roleFilter.getItems().addAll("Tous", "AGRICULTEUR", "EXPERT", "FOURNISSEUR", "ADMIN");
        roleFilter.setValue("Tous");
    }

    // ================= CONFIGURATION COLONNES TABLEAU =================
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colEtat.setCellValueFactory(new PropertyValueFactory<>("etatCompte"));

        // Style pour l'état
        colEtat.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String etat, boolean empty) {
                super.updateItem(etat, empty);
                if (empty || etat == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(etat);
                    if (etat.equals("ACTIF")) {
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

    // ================= CHARGER UTILISATEURS =================
    private void loadUsers() {
        usersList.clear();
        try {
            Connection conn = DataBase.getConnection();
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
            showError("Erreur chargement utilisateurs : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= RECHERCHE =================
    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase();
        filteredList.clear();

        for (User user : usersList) {
            if (user.getNom().toLowerCase().contains(searchText) ||
                    user.getEmail().toLowerCase().contains(searchText)) {
                filteredList.add(user);
            }
        }
        usersTable.setItems(filteredList);
    }

    // ================= FILTRE PAR RÔLE =================
    @FXML
    private void handleFilterByRole() {
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
        usersTable.setItems(filteredList);
    }

    // ================= AJOUTER UTILISATEUR =================
    @FXML
    private void handleAddUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/addUser.fxml"));
            Parent root = loader.load();

            AddUserController controller = loader.getController();
            controller.setManageUsersController(this);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Ajouter un utilisateur");
            stage.show();

        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= MODIFIER UTILISATEUR =================
    private void handleEditUser(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/editUser.fxml"));
            Parent root = loader.load();

            EditUserController controller = loader.getController();
            controller.setUser(user);
            controller.setManageUsersController(this);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Modifier l'utilisateur");
            stage.show();

        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= SUPPRIMER UTILISATEUR =================
    private void handleDeleteUser(User user) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer l'utilisateur ?");
        confirmation.setContentText("Voulez-vous vraiment supprimer " + user.getNom() + " ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            userService.supprimer(user.getId());
            showSuccess("✅ Utilisateur supprimé !");
            handleRefresh();
        }
    }

    // ================= BLOQUER/DÉBLOQUER =================
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

    // ================= REFRESH =================
    @FXML
    public void handleRefresh() {
        loadUsers();
        searchField.clear();
        roleFilter.setValue("Tous");
        showSuccess("🔄 Données actualisées !");
    }

    // ================= NAVIGATION =================
    @FXML
    private void handleBackToDashboard(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard-admin.fxml"));
            Parent root = loader.load();

            DashboardAdminController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Dashboard Admin");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToProfil(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil.fxml"));
            Parent root = loader.load();

            ProfilController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Mon Profil");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= MESSAGES =================
    private void showError(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        }
    }

    public void showSuccess(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
        }
    }
}