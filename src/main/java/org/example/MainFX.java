package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Charger le premier fichier FXML (par exemple "produit.fxml")
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/home.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        // Configurer la fenêtre principale
        primaryStage.setTitle("Gestion de Stock Agricole");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args); // Lance l'application JavaFX
    }
}
