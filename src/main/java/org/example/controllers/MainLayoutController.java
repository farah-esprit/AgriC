package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class MainLayoutController {

    @FXML
    private StackPane contentArea;   // ⚠️ PAS static

    @FXML
    public void initialize() {
        loadPage("/CreerCultureList.fxml");
    }

    public void goDashboard() {
        loadPage("/Dashboard.fxml");
    }

    public void goCultures() {
        loadPage("/CultureList.fxml");
    }

    private void loadPage(String path) {
        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(path)
            );

            Parent page = loader.load();

            contentArea.getChildren().setAll(page);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
