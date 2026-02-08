package service;

import entities.User;
import entities.EtatCompte;
import entities.Role;
import utils.DataBase;

import java.sql.*;

public class UserService implements IService<User> {

    private Connection connection;

    public UserService() {
        try {
            connection = DataBase.getConnection();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ================= AJOUT =================
    @Override
    public void ajouter(User user) {

        String sql = "INSERT INTO user(nom, email, motDePasse, role, etatCompte) VALUES (?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = connection.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS
            );

            ps.setString(1, user.getNom());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getMotDePasse());
            ps.setString(4, user.getRole().name());
            ps.setString(5, user.getEtatCompte().name());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getInt(1));
            }

            System.out.println("User ajouté avec ID = " + user.getId());

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ================= EMAIL EXISTE =================
    public boolean emailExiste(String email) {

        String sql = "SELECT * FROM user WHERE email = ?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return false;
    }

    // ================= AUTHENTIFICATION =================
    public User authenticate(String email, String motDePasse) {

        String sql = "SELECT * FROM user WHERE email=? AND motDePasse=?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, motDePasse);

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
            System.out.println("Erreur authentification : " + e.getMessage());
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

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ================= SUPPRIMER =================
    @Override
    public void supprimer(int id) {

        String sql = "DELETE FROM user WHERE user_id=?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ================= AFFICHER =================
    @Override
    public void afficher() {

        String sql = "SELECT * FROM user";

        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                System.out.println(
                        rs.getInt("user_id") + " | " +
                                rs.getString("nom") + " | " +
                                rs.getString("email") + " | " +
                                rs.getString("role")
                );
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
