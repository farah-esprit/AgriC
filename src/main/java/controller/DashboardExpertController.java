package controller;

import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardExpertController {
    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        welcomeLabel.setText("Bienvenue, " + user.getNom() + " !");
    }
}
