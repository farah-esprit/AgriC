package org.example.utils;


import org.mindrot.jbcrypt.BCrypt;
import utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;

public class AddAdminTool {

    public static void main(String[] args) {
        try {
            // ✅ INFORMATIONS DE L'ADMIN À CRÉER
            String nom = "Admin Principal";
            String email = "admin@gmail.com";
            String motDePasseClair = "Admin@2026"; // ⚠️ À CHANGER !

            // ✅ HACHER LE MOT DE PASSE AVEC BCRYPT
            String motDePasseHache = BCrypt.hashpw(motDePasseClair, BCrypt.gensalt(12));

            // ✅ INSÉRER DANS LA BDD
            Connection conn = MyDataBase.getConnection();
            String sql = "INSERT INTO user (nom, email, motDePasse, role, etatCompte, date_creation) VALUES (?, ?, ?, ?, ?, ?)";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, nom);
            ps.setString(2, email);
            ps.setString(3, motDePasseHache);
            ps.setString(4, "ADMIN");
            ps.setString(5, "ACTIF");
            ps.setTimestamp(6, java.sql.Timestamp.valueOf(LocalDateTime.now()));

            int result = ps.executeUpdate();

            if (result > 0) {
                System.out.println("✅ Admin créé avec succès !");
                System.out.println("📧 Email : " + email);
                System.out.println("🔑 Mot de passe : " + motDePasseClair);
                System.out.println("🔒 Hash BCrypt : " + motDePasseHache);
            } else {
                System.out.println("❌ Erreur lors de la création de l'admin");
            }

            conn.close();

        } catch (Exception e) {
            System.err.println("❌ ERREUR : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
