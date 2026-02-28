package org.example.services.User;


import org.example.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Random;

public class PasswordResetService {

    /**
     * Génère un code de réinitialisation à 6 chiffres
     */
    private String generateResetCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6 chiffres
        return String.valueOf(code);
    }

    /**
     * Crée une demande de réinitialisation
     */
    public String createResetRequest(int userId) {
        try {
            MyDatabase db = new MyDatabase();        // ✅ créer une instance
            Connection conn = db.getConnection();    // ✅ appeler la méthode non statique

            // Générer le code
            String token = generateResetCode();

            // Date d'expiration (30 minutes)
            LocalDateTime expiration = LocalDateTime.now().plusMinutes(30);

            // Insérer dans la BD
            String sql = "INSERT INTO password_reset (user_id, token, expiration) VALUES (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setString(2, token);
            ps.setTimestamp(3, Timestamp.valueOf(expiration));
            ps.executeUpdate();

            System.out.println("✅ Code de réinitialisation créé : " + token);
            return token;

        } catch (SQLException e) {
            System.err.println("❌ Erreur création code : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    public Integer verifyResetCode(String token) {
        try {
            MyDatabase db = new MyDatabase();        // ✅ créer une instance
            Connection conn = db.getConnection();    // ✅ appeler la méthode non statique

            String sql = "SELECT user_id, expiration, used FROM password_reset " +
                    "WHERE token = ? ORDER BY created_at DESC LIMIT 1";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                boolean used = rs.getBoolean("used");
                Timestamp expiration = rs.getTimestamp("expiration");
                int userId = rs.getInt("user_id");

                // Vérifications
                if (used) {
                    System.out.println("❌ Code déjà utilisé");
                    return null;
                }

                if (expiration.before(new Timestamp(System.currentTimeMillis()))) {
                    System.out.println("❌ Code expiré");
                    return null;
                }

                System.out.println("✅ Code valide pour user_id : " + userId);
                return userId;

            } else {
                System.out.println("❌ Code introuvable");
                return null;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur vérification code : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     *Marque le code comme utilisé
     */
    public void markTokenAsUsed(String token) {
        try {
            Connection conn = MyDataBase.getConnection();
            String sql = "UPDATE password_reset SET used = TRUE WHERE token = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, token);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
