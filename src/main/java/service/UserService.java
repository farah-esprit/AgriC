package service;

import entities.User;
import entities.EtatCompte;
import entities.Role;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;

public class UserService implements IService<User> {

    private Connection connection;

    // ================= CONSTRUCTEUR =================
    public UserService() {
        try {
            connection = MyDataBase.getConnection();
        } catch (SQLException e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
        }
    }

    // ================= AJOUT =================
    @Override
    public void ajouter(User user) {
        String sql = "INSERT INTO user(nom, email, motDePasse, role, etatCompte, date_creation) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, user.getNom());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getMotDePasse());
            ps.setString(4, user.getRole().name());
            ps.setString(5, user.getEtatCompte().name());

            // Gérer la date de création
            if (user.getDateCreation() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(user.getDateCreation()));
            } else {
                ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            }

            ps.executeUpdate();

            // Récupérer l'ID généré
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getInt(1));
            }

            System.out.println("✅ User ajouté avec ID = " + user.getId());

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de l'ajout : " + e.getMessage());
            e.printStackTrace();
        }
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

    // ================= AUTHENTIFICATION =================
    public User authenticate(String email, String motDePasse) {
        String sql = "SELECT * FROM user WHERE email = ? AND motDePasse = ?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, motDePasse);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Récupérer la date de création
                Timestamp timestamp = rs.getTimestamp("date_creation");
                LocalDateTime dateCreation = timestamp != null ? timestamp.toLocalDateTime() : null;

                return new User(
                        rs.getInt("user_id"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("motDePasse"),
                        Role.valueOf(rs.getString("role")),
                        EtatCompte.valueOf(rs.getString("etatCompte")),
                        dateCreation
                );
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur authentification : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // ================= RÉCUPÉRER UN USER PAR ID =================
    public User getUserById(int id) {
        String sql = "SELECT * FROM user WHERE user_id = ?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("date_creation");
                LocalDateTime dateCreation = timestamp != null ? timestamp.toLocalDateTime() : null;

                return new User(
                        rs.getInt("user_id"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("motDePasse"),
                        Role.valueOf(rs.getString("role")),
                        EtatCompte.valueOf(rs.getString("etatCompte")),
                        dateCreation
                );
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur récupération user : " + e.getMessage());
        }

        return null;
    }

    // ================= MODIFIER =================
    @Override
    public void modifier(User user) {
        String sql = "UPDATE user SET nom=?, email=?, motDePasse=?, role=?, etatCompte=? WHERE user_id=?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);

            ps.setString(1, user.getNom());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getMotDePasse());
            ps.setString(4, user.getRole().name());
            ps.setString(5, user.getEtatCompte().name());
            ps.setInt(6, user.getId());

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ User modifié avec succès (ID: " + user.getId() + ")");
            } else {
                System.out.println("⚠️ Aucun user trouvé avec l'ID: " + user.getId());
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la modification : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= SUPPRIMER =================
    @Override
    public void supprimer(int id) {
        String sql = "DELETE FROM user WHERE user_id=?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ User supprimé avec succès (ID: " + id + ")");
            } else {
                System.out.println("⚠️ Aucun user trouvé avec l'ID: " + id);
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= AFFICHER TOUS LES USERS =================
    @Override
    public void afficher() {
        String sql = "SELECT * FROM user ORDER BY date_creation DESC";

        try {
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

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de l'affichage : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= COMPTER LES UTILISATEURS =================
    public int compterUsers() {
        String sql = "SELECT COUNT(*) as total FROM user";

        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur comptage users : " + e.getMessage());
        }

        return 0;
    }

    // ================= COMPTER PAR RÔLE =================
    public int compterParRole(Role role) {
        String sql = "SELECT COUNT(*) as total FROM user WHERE role = ?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, role.name());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur comptage par rôle : " + e.getMessage());
        }

        return 0;
    }

    // ================= COMPTER PAR ÉTAT =================
    public int compterParEtat(EtatCompte etat) {
        String sql = "SELECT COUNT(*) as total FROM user WHERE etatCompte = ?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, etat.name());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur comptage par état : " + e.getMessage());
        }

        return 0;
    }

    // ================= NOUVEAUX USERS (X DERNIERS JOURS) =================
    public int compterNouveaux(int jours) {
        String sql = "SELECT COUNT(*) as total FROM user WHERE date_creation >= DATE_SUB(NOW(), INTERVAL ? DAY)";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, jours);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur comptage nouveaux users : " + e.getMessage());
        }

        return 0;
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

    // ================= FERMER LA CONNEXION =================
    public void fermerConnexion() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Connexion fermée");
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur fermeture connexion : " + e.getMessage());
        }
    }
    public User findByEmail(String email) {
        try {
            Connection conn = MyDataBase.getConnection();
            String sql = "SELECT * FROM user WHERE email = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("motDePasse"),
                        Role.valueOf(rs.getString("role")),
                        EtatCompte.valueOf(rs.getString("etatCompte"))
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Met à jour le mot de passe d'un utilisateur
     */
    public boolean updatePassword(int userId, String newPassword) {
        try {
            Connection conn = MyDataBase.getConnection();
            String sql = "UPDATE user SET motDePasse = ? WHERE user_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, newPassword);
            ps.setInt(2, userId);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}