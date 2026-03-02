package dao;

import entities.Evenement;
import entities.User;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EvenementDAO {
    
    public boolean create(Evenement evenement) {
        String sql = "INSERT INTO evenement (titre, description, date_debut, date_fin, lieu, capacite_max, organisateur_id, statut, image_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, evenement.getTitre());
            stmt.setString(2, evenement.getDescription());
            stmt.setDate(3, Date.valueOf(evenement.getDateDebut()));
            stmt.setDate(4, Date.valueOf(evenement.getDateFin()));
            stmt.setString(5, evenement.getLieu());
            stmt.setInt(6, evenement.getCapaciteMax());
            stmt.setInt(7, evenement.getOrganisateurId());
            stmt.setString(8, evenement.getStatut() != null ? evenement.getStatut() : "EN_ATTENTE");
            stmt.setString(9, evenement.getImageUrl());
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    evenement.setIdEvenement(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean update(Evenement evenement) {
        String sql = "UPDATE evenement SET titre = ?, description = ?, date_debut = ?, date_fin = ?, lieu = ?, capacite_max = ?, statut = ?, raison_rejet = ?, image_url = ? WHERE id_evenement = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, evenement.getTitre());
            stmt.setString(2, evenement.getDescription());
            stmt.setDate(3, Date.valueOf(evenement.getDateDebut()));
            stmt.setDate(4, Date.valueOf(evenement.getDateFin()));
            stmt.setString(5, evenement.getLieu());
            stmt.setInt(6, evenement.getCapaciteMax());
            stmt.setString(7, evenement.getStatut());
            stmt.setString(8, evenement.getRaisonRejet());
            stmt.setString(9, evenement.getImageUrl());
            stmt.setInt(10, evenement.getIdEvenement());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean delete(int idEvenement) {
        String sql = "DELETE FROM evenement WHERE id_evenement = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEvenement);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean approveEvent(int idEvenement) {
        String sql = "UPDATE evenement SET statut = 'APPROUVE' WHERE id_evenement = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEvenement);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean rejectEvent(int idEvenement, String raison) {
        String sql = "UPDATE evenement SET statut = 'REJETE', raison_rejet = ? WHERE id_evenement = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, raison);
            stmt.setInt(2, idEvenement);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public Evenement getById(int idEvenement) {
        String sql = "SELECT * FROM evenement WHERE id_evenement = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEvenement);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return extractEvenement(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public List<Evenement> getAll() {
        List<Evenement> evenements = new ArrayList<>();
        String sql = "SELECT * FROM evenement ORDER BY date_debut DESC";
        try (Connection conn = MyDataBase.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                evenements.add(extractEvenement(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return evenements;
    }

    public List<Evenement> getByOrganisateur(int organisateurId) {
        List<Evenement> evenements = new ArrayList<>();
        String sql = "SELECT * FROM evenement WHERE organisateur_id = ? ORDER BY date_debut DESC";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, organisateurId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                evenements.add(extractEvenement(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return evenements;
    }

    public List<Evenement> getByStatut(String statut) {
        List<Evenement> evenements = new ArrayList<>();
        String sql = "SELECT * FROM evenement WHERE statut = ? ORDER BY date_debut DESC";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, statut);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                evenements.add(extractEvenement(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return evenements;
    }

    public List<Evenement> getAvecPagination(int pageNum, int pageSize) {
        List<Evenement> evenements = new ArrayList<>();
        int offset = (pageNum - 1) * pageSize;
        String sql = "SELECT * FROM evenement ORDER BY date_debut DESC LIMIT ? OFFSET ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pageSize);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                evenements.add(extractEvenement(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return evenements;
    }

    public long getTotalCount() {
        String sql = "SELECT COUNT(*) as count FROM evenement";
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
        String sql = "SELECT COUNT(*) as count FROM evenement WHERE statut = ?";
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

    public List<Evenement> searchAndFilter(String titre, LocalDate dateFrom, LocalDate dateTo, 
                                          String lieu, String statut, int pageNum, int pageSize) {
        List<Evenement> evenements = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM evenement WHERE 1=1");
        
        if (titre != null && !titre.isEmpty()) {
            sql.append(" AND titre LIKE ?");
        }
        if (dateFrom != null) {
            sql.append(" AND date_debut >= ?");
        }
        if (dateTo != null) {
            sql.append(" AND date_fin <= ?");
        }
        if (lieu != null && !lieu.isEmpty()) {
            sql.append(" AND lieu LIKE ?");
        }
        if (statut != null && !statut.isEmpty()) {
            sql.append(" AND statut = ?");
        }
        
        sql.append(" ORDER BY date_debut DESC LIMIT ? OFFSET ?");
        
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (titre != null && !titre.isEmpty()) {
                stmt.setString(paramIndex++, "%" + titre + "%");
            }
            if (dateFrom != null) {
                stmt.setDate(paramIndex++, Date.valueOf(dateFrom));
            }
            if (dateTo != null) {
                stmt.setDate(paramIndex++, Date.valueOf(dateTo));
            }
            if (lieu != null && !lieu.isEmpty()) {
                stmt.setString(paramIndex++, "%" + lieu + "%");
            }
            if (statut != null && !statut.isEmpty()) {
                stmt.setString(paramIndex++, statut);
            }
            
            stmt.setInt(paramIndex++, pageSize);
            stmt.setInt(paramIndex, (pageNum - 1) * pageSize);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                evenements.add(extractEvenement(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return evenements;
    }
    
    public List<Evenement> search(String keyword, int organisateurId) {
        List<Evenement> evenements = new ArrayList<>();
        String sql = "SELECT * FROM evenement WHERE (titre LIKE ? OR lieu LIKE ?) AND organisateur_id = ? ORDER BY date_debut DESC";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setInt(3, organisateurId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                evenements.add(extractEvenement(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return evenements;
    }
    
    public List<Evenement> searchAll(String keyword) {
        List<Evenement> evenements = new ArrayList<>();
        String sql = "SELECT * FROM evenement WHERE titre LIKE ? OR lieu LIKE ? ORDER BY date_debut DESC";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                evenements.add(extractEvenement(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return evenements;
    }

    public int getTotalCountInt() {
        String sql = "SELECT COUNT(*) FROM evenement";
        try (Connection conn = MyDataBase.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    private Evenement extractEvenement(ResultSet rs) throws SQLException {
        Evenement e = new Evenement();
        e.setIdEvenement(rs.getInt("id_evenement"));
        e.setTitre(rs.getString("titre"));
        e.setDescription(rs.getString("description"));
        e.setDateDebut(rs.getDate("date_debut").toLocalDate());
        e.setDateFin(rs.getDate("date_fin").toLocalDate());
        e.setLieu(rs.getString("lieu"));
        e.setCapaciteMax(rs.getInt("capacite_max"));
        int orgId = rs.getInt("organisateur_id");
        e.setOrganisateurId(orgId);
        // Pour compatibilité "objet": on attache un User minimal (id seulement)
        if (orgId > 0) {
            User u = new User();
            u.setId(orgId);
            e.setOrganisateur(u);
        }
        e.setStatut(rs.getString("statut"));
        e.setRaisonRejet(rs.getString("raison_rejet"));
        e.setImageUrl(rs.getString("image_url"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            e.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            e.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return e;
    }
}
