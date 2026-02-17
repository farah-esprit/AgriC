package services;

import entities.Produit;
import org.example.utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitService {

    // ✅ CORRECTION : Ajout du champ imagePath
    public void ajouter(Produit p) {
        String sql = "INSERT INTO produit (nom, description, prix, categorie, actif, imagePath) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection cn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, p.getNom());
            ps.setString(2, p.getDescription());
            ps.setDouble(3, p.getPrix());
            ps.setString(4, p.getCategorie());
            ps.setBoolean(5, p.isActif());
            ps.setString(6, p.getImagePath()); // ✅ AJOUT

            ps.executeUpdate();
            System.out.println("✅ Produit ajouté : " + p.getNom() + " | Image : " + p.getImagePath());
        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout produit : " + e.getMessage());
            e.printStackTrace();
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
                p.setImagePath(rs.getString("imagePath"));
                list.add(p);


                System.out.println("📦 Produit chargé : " + p.getNom() + " | Image : " + p.getImagePath());
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lecture produits : " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }


    public void modifier(Produit p) {
        String sql = "UPDATE produit SET nom=?, description=?, prix=?, categorie=?, actif=?, imagePath=? WHERE id_produit=?";
        try (Connection cn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, p.getNom());
            ps.setString(2, p.getDescription());
            ps.setDouble(3, p.getPrix());
            ps.setString(4, p.getCategorie());
            ps.setBoolean(5, p.isActif());
            ps.setString(6, p.getImagePath()); // ✅ AJOUT
            ps.setLong(7, p.getIdProduit());

            ps.executeUpdate();
            System.out.println("✅ Produit modifié : " + p.getNom() + " | Image : " + p.getImagePath());
        } catch (SQLException e) {
            System.err.println("❌ Erreur modification produit : " + e.getMessage());
            e.printStackTrace();
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
                    p.setImagePath(rs.getString("imagePath")); // ✅ AJOUT
                    return p;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur getById(" + id + ") : " + e.getMessage());
        }
        return null;
    }
}