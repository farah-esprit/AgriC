package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataBase {

    private static Connection connection;

    public static Connection getConnection() throws SQLException {

        if (connection == null || connection.isClosed()) {
            try {
                connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/agriconnect_db",
                        "root",
                        ""
                );
                System.out.println("Connexion MySQL réussie");
            } catch (SQLException e) {
                System.err.println("MySQL indisponible !");
                throw e;
            }
        }
        return connection;
    }
}

