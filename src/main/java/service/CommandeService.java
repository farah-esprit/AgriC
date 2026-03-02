package service;

import entities.Commande;
import entities.User;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandeService {

    public void ajouter(Commande c) {
        String sql = "INSERT INTO commande (date_commande, statut, quantite_commandee, id_produit, user_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection cn = MyDataBase.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setTimestamp(1, Timestamp.valueOf(c.getDateCommande()));
            ps.setString(2, c.getStatut());
            ps.setInt(3, c.getQuantiteCommandee());

            if (c.getIdProduit() != null) {
                ps.setLong(4, c.getIdProduit());
            } else {
                ps.setNull(4, Types.BIGINT);
            }

            if (c.getUserId() != null) {
                ps.setInt(5, c.getUserId().getId()); // ✅ on extrait l'ID de l'objet User
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        c.setIdCommande(rs.getInt(1));
                        System.out.println("✅ Commande #" + c.getIdCommande() + " AJOUTÉE");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Ajout commande échoué : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Commande> getAllCommandes() {
        List<Commande> commandes = new ArrayList<>();
        String sql = "SELECT * FROM commande";

        try (Connection cn = MyDataBase.getConnection();
             Statement stmt = cn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Commande c = new Commande();
                c.setIdCommande(rs.getInt("id_commande"));
                c.setDateCommande(rs.getTimestamp("date_commande").toLocalDateTime());
                c.setStatut(rs.getString("statut"));
                c.setQuantiteCommandee(rs.getInt("quantite_commandee"));

                if (rs.getObject("id_produit") != null) {
                    c.setIdProduit(rs.getLong("id_produit"));
                }

                if (rs.getObject("user_id") != null) {
                    User user = new User();
                    user.setId(rs.getInt("user_id")); // ✅ on reconstruit un User minimal avec juste l'ID
                    c.setUserId(user);
                }

                commandes.add(c);
            }
            System.out.println("📋 " + commandes.size() + " commandes trouvées");
        } catch (SQLException e) {
            System.err.println("❌ Lecture commandes échouée : " + e.getMessage());
            e.printStackTrace();
        }

        return commandes;
    }

    public void modifierStatut(int id, String nouveauStatut) {
        String sql = "UPDATE commande SET statut = ? WHERE id_commande = ?";

        try (Connection cn = MyDataBase.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, nouveauStatut);
            ps.setInt(2, id);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Statut commande #" + id + " → " + nouveauStatut);
            } else {
                System.out.println("⚠️ Commande #" + id + " non trouvée");
            }
        } catch (SQLException e) {
            System.err.println("❌ Update statut échoué : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void supprimer(int id) {
        String sql = "DELETE FROM commande WHERE id_commande = ?";

        try (Connection cn = MyDataBase.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("🗑️ Commande #" + id + " supprimée");
            } else {
                System.out.println("⚠️ Commande #" + id + " non trouvée");
            }
        } catch (SQLException e) {
            System.err.println("❌ Suppression échouée : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Commande> getDernieresCommandes(int limit) {
        List<Commande> result = new ArrayList<>();
        String sql = "SELECT * FROM commande ORDER BY date_commande DESC LIMIT ?";

        try (Connection cn = MyDataBase.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Commande c = new Commande();
                    c.setIdCommande(rs.getInt("id_commande"));
                    c.setDateCommande(rs.getTimestamp("date_commande").toLocalDateTime());
                    c.setStatut(rs.getString("statut"));
                    c.setQuantiteCommandee(rs.getInt("quantite_commandee"));
                    if (rs.getObject("id_produit") != null) c.setIdProduit(rs.getLong("id_produit"));
                    if (rs.getObject("user_id") != null) {
                        User user = new User();
                        user.setId(rs.getInt("user_id")); // ✅
                        c.setUserId(user);
                    }
                    result.add(c);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur getDernieresCommandes : " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
}