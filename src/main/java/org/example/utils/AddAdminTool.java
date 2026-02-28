package org.example.utils;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;

public class AddAdminTool {

    public static void main(String[] args) {

        // ✅ INFORMATIONS DE L'ADMIN À CRÉER
        String nom = "Admin Principal";
        String email = "admin@gmail.com";
        String motDePasseClair = "Admin@2026"; // ⚠️ À CHANGER !

        // ✅ HACHER LE MOT DE PASSE AVEC BCRYPT
        String motDePasseHache = BCrypt.hashpw(motDePasseClair, BCrypt.gensalt(12));

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO user (nom, email, motDePasse, role, etatCompte, date_creation) VALUES (?, ?, ?, ?, ?, ?)")) {

            ps.setString(1, nom);
            ps.setString(2, email);
            ps.setString(3, motDePasseHache);
            ps.setString(4, "ADMIN");
            ps.setString(5, "ACTIF");
            ps.setTimestamp(6, java.sql.Timestamp.valueOf(LocalDateTime.now()));

            int result = ps.executeUpdate();

            if (result > 0) {
                System.out.println("✅ Admin créé !");
                System.out.println("📧 Email : " + email);
                System.out.println("🔑 Mot de passe : " + motDePasseClair);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}