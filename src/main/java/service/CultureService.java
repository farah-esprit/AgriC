package service;

import entities.Culture;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CultureService {

    private Connection connection;

    public CultureService() {
        try {
            connection = MyDataBase.getConnection();
        } catch (SQLException e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
        }
    }

    public void ajouter(Culture c) throws SQLException {
        if (!c.estValide()) throw new IllegalArgumentException(c.obtenirErreursValidation());

        String req = "INSERT INTO culture (nom, type, superficie, localisation, image) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNom());
            ps.setString(2, c.getType());
            ps.setDouble(3, c.getSuperficie());
            ps.setString(4, c.getLocalisation());
            ps.setString(5, c.getImage());
            ps.executeUpdate();

            // Récupérer l'ID généré automatiquement
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    c.setIdCulture(rs.getInt(1)); // Mettre à jour l'objet avec l'ID réel
                }
            }
        }
    }


    public void modifier(Culture c) throws SQLException {
        if (!c.estValide()) throw new IllegalArgumentException(c.obtenirErreursValidation());

        String req = "UPDATE culture SET nom=?, type=?, superficie=?, localisation=?, image=? WHERE idCulture=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, c.getNom());
            ps.setString(2, c.getType());
            ps.setDouble(3, c.getSuperficie());
            ps.setString(4, c.getLocalisation());
            ps.setString(5, c.getImage());
            ps.setInt(6, c.getIdCulture());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM culture WHERE idCulture=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Culture> getAll() {
        List<Culture> cultures = new ArrayList<>();
        String sql = "SELECT * FROM culture";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                cultures.add(new Culture(
                        rs.getInt("idCulture"),
                        rs.getString("nom"),
                        rs.getString("type"),
                        rs.getDouble("superficie"),
                        rs.getString("localisation"),
                        rs.getString("image")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cultures;
    }
}
