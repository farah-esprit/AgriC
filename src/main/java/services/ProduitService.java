package services;

import entities.Produit;
import org.example.utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitService {

    public void ajouter(Produit p) {
        String sql = "INSERT INTO produit (nom, description, prix, categorie, actif) VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, p.getNom());
            ps.setString(2, p.getDescription());
            ps.setDouble(3, p.getPrix());
            ps.setString(4, p.getCategorie());
            ps.setBoolean(5, p.isActif());

            ps.executeUpdate();
            System.out.println("✅ Produit ajouté : " + p.getNom());
        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout produit : " + e.getMessage());
        }
    }

    public List<Produit> getAllProduits() {
        List<Produit> list = new ArrayList<>();
        String sql = "SELECT * FROM produit";

        try (Connection cn = MyDatabase.getInstance().getConnection();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Produit p = new Produit();
                p.setIdProduit(rs.getLong("id_produit"));
                p.setNom(rs.getString("nom"));
                p.setDescription(rs.getString("description"));
                p.setPrix(rs.getDouble("prix"));
                p.setCategorie(rs.getString("categorie"));
                p.setActif(rs.getBoolean("actif"));
                list.add(p);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lecture produits : " + e.getMessage());
        }
        return list;
    }

    public void modifier(Produit p) {
        String sql = "UPDATE produit SET nom=?, description=?, prix=?, categorie=?, actif=? WHERE id_produit=?";
        try (Connection cn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, p.getNom());
            ps.setString(2, p.getDescription());
            ps.setDouble(3, p.getPrix());
            ps.setString(4, p.getCategorie());
            ps.setBoolean(5, p.isActif());
            ps.setLong(6, p.getIdProduit());

            ps.executeUpdate();
            System.out.println("✅ Produit modifié : " + p.getNom());
        } catch (SQLException e) {
            System.err.println("❌ Erreur modification produit : " + e.getMessage());
        }
    }

    public void supprimer(Long id) {
        String sql = "DELETE FROM produit WHERE id_produit=?";
        try (Connection cn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();
            System.out.println("✅ Produit supprimé ID=" + id);
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression produit : " + e.getMessage());
        }
    }

    public Produit getById(long id) {
        String sql = "SELECT * FROM produit WHERE id_produit = ?";
        try (Connection cn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Produit p = new Produit();
                    p.setIdProduit(rs.getLong("id_produit"));
                    p.setNom(rs.getString("nom"));
                    p.setDescription(rs.getString("description"));
                    p.setPrix(rs.getDouble("prix"));
                    p.setCategorie(rs.getString("categorie"));
                    p.setActif(rs.getBoolean("actif"));
                    return p;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur getById(" + id + ") : " + e.getMessage());
        }
        return null;
    }
}