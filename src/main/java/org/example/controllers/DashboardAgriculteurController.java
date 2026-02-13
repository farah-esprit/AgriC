package org.example.controllers;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;

public class DashboardAgriculteurController {

    @FXML
    private AnchorPane contentPane;

    @FXML
    private void handleGoToCulture() {
        try {
            AnchorPane view = FXMLLoader.load(
                    getClass().getResource("/CultureList.fxml")
            );
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

