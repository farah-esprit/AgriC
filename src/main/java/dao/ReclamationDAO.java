package dao;

import entities.Reclamation;
import entities.User;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReclamationDAO {
    
    public boolean create(Reclamation reclamation) {
        String sql = "INSERT INTO reclamation (objet, description, date_creation, statut, priorite, type, id_utilisateur) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, reclamation.getObjet());
            stmt.setString(2, reclamation.getDescription());
            stmt.setDate(3, Date.valueOf(reclamation.getDateCreation()));
            stmt.setString(4, reclamation.getStatut() != null ? reclamation.getStatut() : "EN_ATTENTE");
            stmt.setString(5, reclamation.getPriorite());
            stmt.setString(6, reclamation.getType());
            stmt.setInt(7, reclamation.getIdUtilisateur());
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    reclamation.setIdReclamation(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean update(Reclamation reclamation) {
        String sql = "UPDATE reclamation SET objet = ?, description = ?, date_creation = ?, statut = ?, priorite = ?, type = ?, reponse_admin = ?, date_reponse = ? WHERE id_reclamation = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reclamation.getObjet());
            stmt.setString(2, reclamation.getDescription());
            stmt.setDate(3, Date.valueOf(reclamation.getDateCreation()));
            stmt.setString(4, reclamation.getStatut());
            stmt.setString(5, reclamation.getPriorite());
            stmt.setString(6, reclamation.getType());
            stmt.setString(7, reclamation.getReponseAdmin());
            if (reclamation.getDateReponse() != null) {
                stmt.setDate(8, Date.valueOf(reclamation.getDateReponse()));
            } else {
                stmt.setNull(8, Types.DATE);
            }
            stmt.setInt(9, reclamation.getIdReclamation());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateStatut(int idReclamation, String statut) {
        String sql = "UPDATE reclamation SET statut = ? WHERE id_reclamation = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, statut);
            stmt.setInt(2, idReclamation);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean repondre(int idReclamation, String reponse) {
        String sql = "UPDATE reclamation SET reponse_admin = ?, date_reponse = ? WHERE id_reclamation = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reponse);
            stmt.setDate(2, Date.valueOf(LocalDate.now()));
            stmt.setInt(3, idReclamation);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean markAsProcessed(int idReclamation) {
        String sql = "UPDATE reclamation SET statut = 'TRAITEE', date_reponse = ? WHERE id_reclamation = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(LocalDate.now()));
            stmt.setInt(2, idReclamation);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean delete(int id) {
        String sql = "DELETE FROM reclamation WHERE id_reclamation = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Reclamation getById(int idReclamation) {
        String sql = "SELECT * FROM reclamation WHERE id_reclamation = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idReclamation);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return extractReclamation(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public List<Reclamation> getAll() {
        List<Reclamation> reclamations = new ArrayList<>();
        String sql = "SELECT * FROM reclamation ORDER BY date_creation DESC";
        try (Connection conn = MyDataBase.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reclamations.add(extractReclamation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reclamations;
    }

    public List<Reclamation> getAvecPagination(int pageNum, int pageSize) {
        List<Reclamation> reclamations = new ArrayList<>();
        int offset = (pageNum - 1) * pageSize;
        String sql = "SELECT * FROM reclamation ORDER BY date_creation DESC LIMIT ? OFFSET ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pageSize);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reclamations.add(extractReclamation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reclamations;
    }

    public long getTotalCount() {
        String sql = "SELECT COUNT(*) as count FROM reclamation";
        try (Connection conn = MyDataBase.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong("count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public long getCountByStatut(String statut) {
        String sql = "SELECT COUNT(*) as count FROM reclamation WHERE statut = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, statut);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public List<Reclamation> getByUtilisateur(int utilisateurId) {
        List<Reclamation> reclamations = new ArrayList<>();
        String sql = "SELECT * FROM reclamation WHERE id_utilisateur = ? ORDER BY date_creation DESC";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilisateurId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reclamations.add(extractReclamation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reclamations;
    }

    public List<Reclamation> getByStatut(String statut) {
        List<Reclamation> reclamations = new ArrayList<>();
        String sql = "SELECT * FROM reclamation WHERE statut = ? ORDER BY date_creation DESC";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, statut);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reclamations.add(extractReclamation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reclamations;
    }
    
    public List<Reclamation> search(String keyword, int utilisateurId) {
        List<Reclamation> reclamations = new ArrayList<>();
        String sql = "SELECT * FROM reclamation WHERE (objet LIKE ? OR type LIKE ?) AND id_utilisateur = ? ORDER BY date_creation DESC";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setInt(3, utilisateurId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reclamations.add(extractReclamation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reclamations;
    }
    
    public List<Reclamation> searchAll(String keyword) {
        List<Reclamation> reclamations = new ArrayList<>();
        String sql = "SELECT * FROM reclamation WHERE objet LIKE ? OR type LIKE ? ORDER BY date_creation DESC";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reclamations.add(extractReclamation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reclamations;
    }
    
    public Map<String, Integer> getStatsByStatut() {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT statut, COUNT(*) as count FROM reclamation GROUP BY statut";
        try (Connection conn = MyDataBase.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                stats.put(rs.getString("statut"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
    
    public Map<String, Integer> getStatsByType() {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT type, COUNT(*) as count FROM reclamation GROUP BY type";
        try (Connection conn = MyDataBase.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                stats.put(rs.getString("type"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
    
    private Reclamation extractReclamation(ResultSet rs) throws SQLException {
        Reclamation r = new Reclamation();
        r.setIdReclamation(rs.getInt("id_reclamation"));
        r.setObjet(rs.getString("objet"));
        r.setDescription(rs.getString("description"));
        r.setDateCreation(rs.getDate("date_creation").toLocalDate());
        r.setStatut(rs.getString("statut"));
        r.setPriorite(rs.getString("priorite"));
        r.setType(rs.getString("type"));
        int userId = rs.getInt("id_utilisateur");
        r.setIdUtilisateur(userId);
        // Pour compatibilité "objet": on attache un User minimal (id seulement)
        if (userId > 0) {
            User u = new User();
            u.setId(userId);
            r.setUser(u);
        }
        r.setReponseAdmin(rs.getString("reponse_admin"));
        
        java.sql.Date dateReponse = rs.getDate("date_reponse");
        if (dateReponse != null) {
            r.setDateReponse(dateReponse.toLocalDate());
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            r.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            r.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return r;
    }
}
