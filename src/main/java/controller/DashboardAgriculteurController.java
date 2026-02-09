package controller;

import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class DashboardAgriculteurController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        welcomeLabel.setText("Bienvenue, " + user.getNom() + " !");
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Login");
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

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Mon Profil");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}