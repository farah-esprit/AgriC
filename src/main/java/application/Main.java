package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));

            Scene scene = new Scene(root);
            primaryStage.setTitle("AgriConnect - Login");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();

        } catch (Exception e) {
            System.out.println("Erreur lors du chargement de l'interface :");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}