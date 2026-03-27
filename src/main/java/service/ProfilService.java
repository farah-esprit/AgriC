package service;

import entities.Profil;
import utils.MyDataBase;

import java.sql.*;

public class ProfilService implements IService<Profil> {

    private Connection connection;

    public ProfilService() {
        try {
            connection = MyDataBase.getConnection();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    //AJOUT
    @Override
    public void ajouter(Profil profil) {

        if (profil.getUser() == null || profil.getUser().getId() <= 0) {
            System.out.println("Profil doit être lié à un User existant !");
            return;
        }

        String sql = "INSERT INTO profil(bio, telephone, nom, prenom, image, user_id) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);

            ps.setString(1, profil.getBio());
            ps.setString(2, profil.getTelephone());
            ps.setString(3, profil.getNom());
            ps.setString(4, profil.getPrenom());
            ps.setString(5, profil.getImage());
            ps.setInt(6, profil.getUser().getId());

            ps.executeUpdate();
            System.out.println("Profil ajouté");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    //MODIFIER
    @Override
    public void modifier(Profil profil) {

        if (profil.getId() <= 0) {
            System.out.println("ID Profil invalide !");
            return;
        }

        String sql = "UPDATE profil SET bio=?, telephone=?, nom=?, prenom=?, image=? WHERE id=?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);

            ps.setString(1, profil.getBio());
            ps.setString(2, profil.getTelephone());
            ps.setString(3, profil.getNom());
            ps.setString(4, profil.getPrenom());
            ps.setString(5, profil.getImage());
            ps.setInt(6, profil.getId());

            ps.executeUpdate();
            System.out.println("Profil modifié");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    //SUPPRIMER
    @Override
    public void supprimer(int id) {

        String sql = "DELETE FROM profil WHERE id=?";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();

            System.out.println("Profil supprimé");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    //AFFICHER
    @Override
    public void lire(Profil p) throws SQLException {

        String sql = """
                SELECT p.id, p.nom, p.prenom, p.telephone, u.email
                FROM profil p
                JOIN user u ON p.user_id = u.user_id
                """;

        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                System.out.println(
                        rs.getInt("id") + " | " +
                                rs.getString("nom") + " " +
                                rs.getString("prenom") + " | " +
                                rs.getString("telephone") + " | " +
                                rs.getString("email")
                );
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
