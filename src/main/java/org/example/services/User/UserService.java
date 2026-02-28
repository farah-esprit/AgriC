package org.example.services.User;

import org.example.entities.EtatCompte;
import org.example.entities.Role;
import org.example.entities.User;
import org.example.services.IService;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;

public class UserService implements IService<User> {

    private Connection connection;

    // ================= CONSTRUCTEUR =================
    public UserService() {

            MyDatabase db = new MyDatabase();
            connection = db.getConnection();
    }
    // Vérifie si l'utilisateur a activé la 2FA
    public boolean is2FAEnabled(int userId) {
        try {
            String sql = "SELECT two_factor_enabled FROM user WHERE user_id = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("two_factor_enabled"); // assuming your table has this column
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    // Trouver un utilisateur par son email
    public User findByEmail(String email) {
        try {
            String sql = "SELECT * FROM user WHERE email = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("motDePasse"),
                        Role.valueOf(rs.getString("role")),
                        EtatCompte.valueOf(rs.getString("etatCompte")),
                        rs.getTimestamp("date_creation") != null ? rs.getTimestamp("date_creation").toLocalDateTime() : null,
                        rs.getString("verification_code"),
                        rs.getTimestamp("code_expiration")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ================= AJOUT =================
    @Override
    public void ajouter(User user) throws SQLException {
        String sql = "INSERT INTO user(nom, email, motDePasse, role, etatCompte, date_creation, verification_code, code_expiration) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, user.getNom());
        ps.setString(2, user.getEmail());
        ps.setString(3, PasswordService.hashPassword(user.getMotDePasse()));
        ps.setString(4, user.getRole().name());
        ps.setString(5, user.getEtatCompte().name());
        ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
        ps.setString(7, user.getVerificationCode());
        ps.setTimestamp(8, user.getCodeExpiration());

        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            user.setId(rs.getInt(1));
        }

        System.out.println("✅ User ajouté avec mot de passe hashé et code de vérification");
    }

    // ================= SUPPRIMER =================
    @Override
    public void supprimer(User user) throws SQLException {
        String sql = "DELETE FROM user WHERE user_id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, user.getId());
        ps.executeUpdate();
        System.out.println("✅ User supprimé avec succès (ID: " + user.getId() + ")");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM user WHERE user_id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("✅ User supprimé avec succès (ID: " + id + ")");
    }
    // ================= VÉRIFIER SI EMAIL EXISTE =================
    public boolean emailExiste(String email) {
        String sql = "SELECT COUNT(*) as total FROM user WHERE email = ?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("total") > 0;
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur vérification email : " + e.getMessage());
        }

        return false;
    }
    // ================= CHANGER MOT DE PASSE =================
    public boolean changerMotDePasse(int userId, String ancienMdp, String nouveauMdp) {
        // Vérifier l'ancien mot de passe
        String sqlVerif = "SELECT * FROM user WHERE user_id = ? AND motDePasse = ?";

        try {
            PreparedStatement psVerif = connection.prepareStatement(sqlVerif);
            psVerif.setInt(1, userId);
            psVerif.setString(2, ancienMdp);

            ResultSet rs = psVerif.executeQuery();

            if (!rs.next()) {
                System.out.println("❌ Ancien mot de passe incorrect");
                return false;
            }

            // Mettre à jour le mot de passe
            String sqlUpdate = "UPDATE user SET motDePasse = ? WHERE user_id = ?";
            PreparedStatement psUpdate = connection.prepareStatement(sqlUpdate);
            psUpdate.setString(1, nouveauMdp);
            psUpdate.setInt(2, userId);

            int rowsAffected = psUpdate.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Mot de passe changé avec succès");
                return true;
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur changement mot de passe : " + e.getMessage());
        }

        return false;
    }
    // ================= MODIFIER =================
    @Override
    public void modifier(User user) throws SQLException {
        String sql = "UPDATE user SET nom=?, email=?, motDePasse=?, role=?, etatCompte=?, verification_code=?, code_expiration=? WHERE user_id=?";
        PreparedStatement ps = connection.prepareStatement(sql);

        ps.setString(1, user.getNom());
        ps.setString(2, user.getEmail());
        ps.setString(3, user.getMotDePasse());
        ps.setString(4, user.getRole().name());
        ps.setString(5, user.getEtatCompte().name());
        ps.setString(6, user.getVerificationCode());
        ps.setTimestamp(7, user.getCodeExpiration());
        ps.setInt(8, user.getId());

        int rowsAffected = ps.executeUpdate();
        if (rowsAffected > 0) {
            System.out.println("✅ User modifié avec succès (ID: " + user.getId() + ")");
        } else {
            System.out.println("⚠️ Aucun user trouvé avec l'ID: " + user.getId());
        }
    }
    public String get2FASecret(int userId) {
        String sql = "SELECT two_factor_secret FROM user WHERE user_id = ?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("two_factor_secret");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
    public void enable2FA(int userId, String secret) {
        String sql = "UPDATE user SET two_factor_secret = ?, two_factor_enabled = TRUE WHERE user_id = ?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, secret);
            ps.setInt(2, userId);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ 2FA activé pour userId: " + userId);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur enable2FA : " + e.getMessage());
            e.printStackTrace();
        }
    }
    // ================= LIRE =================
    @Override
    public void lire(User user) throws SQLException {
        String sql = "SELECT * FROM user ORDER BY date_creation DESC";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        System.out.println("\n========== LISTE DES UTILISATEURS ==========");
        System.out.printf("%-5s | %-20s | %-25s | %-12s | %-10s | %-20s%n",
                "ID", "NOM", "EMAIL", "RÔLE", "ÉTAT", "DATE CRÉATION");
        System.out.println("─".repeat(110));

        while (rs.next()) {
            System.out.printf("%-5d | %-20s | %-25s | %-12s | %-10s | %-20s%n",
                    rs.getInt("user_id"),
                    rs.getString("nom"),
                    rs.getString("email"),
                    rs.getString("role"),
                    rs.getString("etatCompte"),
                    rs.getTimestamp("date_creation")
            );
        }

        System.out.println("=".repeat(110) + "\n");
    }

    // ================= AUTRES MÉTHODES =================
    public User authenticate(String email, String plainPassword) {
        try {
            String sql = "SELECT * FROM user WHERE email = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next() && PasswordService.checkPassword(plainPassword, rs.getString("motDePasse"))) {
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("motDePasse"),
                        Role.valueOf(rs.getString("role")),
                        EtatCompte.valueOf(rs.getString("etatCompte")),
                        rs.getTimestamp("date_creation") != null ? rs.getTimestamp("date_creation").toLocalDateTime() : null,
                        rs.getString("verification_code"),
                        rs.getTimestamp("code_expiration")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // updatePassword corrigé pour MyDatabase non statique
    public boolean updatePassword(int userId, String newPassword) {
        try {
            MyDatabase db = new MyDatabase();
            Connection conn = db.getConnection();
            String sql = "UPDATE user SET motDePasse = ? WHERE user_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, newPassword);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}