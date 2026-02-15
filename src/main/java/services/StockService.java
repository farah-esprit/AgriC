package services;

import entities.Stock;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockService {

    public void ajouter(Stock s) {
        String sql = "INSERT INTO stock (quantite, disponible, seuilAlert, idProduit) VALUES (?, ?, ?, ?)";

        try (Connection cn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, s.getQuantite());
            ps.setInt(2, s.getDisponible());
            ps.setInt(3, s.getSeuilAlert());
            ps.setInt(4, s.getIdProduit());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                s.setIdStock(rs.getInt(1));
                System.out.println("✅ Stock #" + s.getIdStock() + " AJOUTÉ !");
            }

        } catch (SQLException e) {
            System.err.println("❌ Ajout stock échoué : " + e.getMessage());
        }
    }

    public List<Stock> getAllStocks() {
        List<Stock> stocks = new ArrayList<>();
        String sql = "SELECT * FROM stock";

        try (Connection cn = MyDatabase.getInstance().getConnection();
             Statement stmt = cn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Stock s = new Stock();
                s.setIdStock(rs.getInt("idStock"));
                s.setQuantite(rs.getInt("quantite"));
                s.setDisponible(rs.getInt("disponible"));
                s.setSeuilAlert(rs.getInt("seuilAlert"));
                s.setIdProduit(rs.getInt("idProduit"));
                stocks.add(s);
            }

        } catch (SQLException e) {
            System.err.println("❌ Lecture stocks échouée : " + e.getMessage());
        }

        return stocks;
    }

    public void modifier(Stock s) {
        String sql = "UPDATE stock SET quantite=?, disponible=?, seuilAlert=?, idProduit=? WHERE idStock=?";

        try (Connection cn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, s.getQuantite());
            ps.setInt(2, s.getDisponible());
            ps.setInt(3, s.getSeuilAlert());
            ps.setInt(4, s.getIdProduit());
            ps.setInt(5, s.getIdStock());

            ps.executeUpdate();
            System.out.println("✅ Stock #" + s.getIdStock() + " MODIFIÉ !");

        } catch (SQLException e) {
            System.err.println("❌ Modification stock échouée : " + e.getMessage());
        }
    }

    public void supprimer(int id) {
        String sql = "DELETE FROM stock WHERE idStock=?";

        try (Connection cn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("🗑️ Stock #" + id + " SUPPRIMÉ !");

        } catch (SQLException e) {
            System.err.println("❌ Suppression stock échouée : " + e.getMessage());
        }
    }

    public int getQuantiteDisponible(int idProduit) {
        String sql = "SELECT disponible FROM stock WHERE idProduit=?";
        int quantite = 0;

        try (Connection cn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, idProduit);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                quantite = rs.getInt("disponible");
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur getQuantiteDisponible : " + e.getMessage());
        }

        return quantite;
    }
}
