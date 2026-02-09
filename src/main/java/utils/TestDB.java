package utils;

import java.sql.DriverManager;

public class TestDB {
    public static void main(String[] args) {
        System.out.println("Test de connexion...");

        try {
            // Charge explicitement le driver
            System.out.println("1. Chargement du driver MySQL...");
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✓ Driver chargé !");

            // Test de connexion
            System.out.println("2. Connexion à MySQL...");
            String url = "jdbc:mysql://127.0.0.1:3306/mysql?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            var conn = DriverManager.getConnection(url, "root", "");
            System.out.println("✓ Connexion réussie !");

            // Test simple
            var stmt = conn.createStatement();
            var rs = stmt.executeQuery("SELECT VERSION()");
            if (rs.next()) {
                System.out.println("Version MySQL : " + rs.getString(1));
            }

            conn.close();
            System.out.println("✓ Test terminé avec succès !");

        } catch (ClassNotFoundException e) {
            System.out.println("✗ Driver MySQL introuvable !");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("✗ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}