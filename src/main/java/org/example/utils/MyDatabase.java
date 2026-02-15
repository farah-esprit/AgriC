package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static final String USER = "root";
    private static final String PASSWORD = "";  // mets ton mot de passe ici si besoin
    private static final String URL = "jdbc:mysql://localhost:3306/agriconnect_db" +
            "?useSSL=false" +
            "&serverTimezone=UTC" +
            "&allowPublicKeyRetrieval=true" +
            "&connectTimeout=10000" +      // 10 secondes max pour se connecter
            "&socketTimeout=30000";        // 30 secondes d'inactivité max

    private static MyDatabase instance;

    private MyDatabase() {
        // Constructeur privé → singleton
    }

    public static synchronized MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    /**
     * Retourne TOUJOURS une nouvelle connexion valide
     * → évite les erreurs "connection closed"
     */
    public Connection getConnection() throws SQLException {
        try {
            // Charge le driver (plus nécessaire avec JDBC 4+, mais sécurise)
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Nouvelle connexion créée ✅");
            return conn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL non trouvé", e);
        }
    }
}