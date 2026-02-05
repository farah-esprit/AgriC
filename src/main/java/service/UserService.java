package service;

import entities.User;
import utils.DataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User> {

    Connection connection;

    public UserService() {
        try {
            connection = DataBase.getConnection();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Ajouter
    @Override
    public void ajouter(User user) {

        String sql = "INSERT INTO user(nom,email,motDePasse,role,etatCompte) VALUES (?,?,?,?,?)";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);

            ps.setString(1, user.getNom());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getMotDePasse());
            ps.setString(4, user.getRole().name());
            ps.setString(5, user.getEtatCompte().name());

            ps.executeUpdate();
            System.out.println("User ajouté");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Modifier
    @Override
    public void modifier(User user) {

        String sql = "UPDATE user SET nom=?, email=?, motDePasse=?, role=?, etatCompte=? WHERE id=?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);

            ps.setString(1, user.getNom());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getMotDePasse());
            ps.setString(4, user.getRole().name());
            ps.setString(5, user.getEtatCompte().name());
            ps.setInt(6, user.getId());

            ps.executeUpdate();
            System.out.println("User modifié");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Supprimer
    @Override
    public void supprimer(int id) {

        String sql = "DELETE FROM user WHERE id=?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);

            ps.executeUpdate();
            System.out.println("User supprimé");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Afficher
    @Override
    public void afficher() {

        String sql = "SELECT * FROM user";

        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                System.out.println(
                        rs.getInt("id") + " | " +
                                rs.getString("nom") + " | " +
                                rs.getString("email") + " | " +
                                rs.getString("role") + " | " +
                                rs.getString("etatCompte")
                );
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
