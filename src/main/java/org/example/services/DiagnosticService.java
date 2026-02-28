package org.example.services;

import org.example.entities.Diagnostic;
import org.example.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DiagnosticService implements IService<Diagnostic> {

    private final Connection connection;

    public DiagnosticService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Diagnostic diag) throws SQLException {

        String sql = """
                INSERT INTO diagnostic
                (dateDiagnostic, etat, symptomes, informationsComplementaires,
                 recommandations, idCulture, idUser,
                 disease_detected, confidence, rapport_ia)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setDate(1, Date.valueOf(diag.getDateDiagnostic()));
            ps.setString(2, diag.getEtat());
            ps.setString(3, diag.getSymptomes());
            ps.setString(4, diag.getInformationsComplementaires());
            ps.setString(5, diag.getRecommandations());
            ps.setInt(6, diag.getIdCulture());
            ps.setInt(7, diag.getIdUser());
            ps.setString(8, diag.getDiseaseDetected());
            ps.setDouble(9, diag.getConfidence());
            ps.setString(10, diag.getRapportIa());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                diag.setIdDiagnostic(rs.getInt(1));
            }
        }
    }

    @Override
    public void supprimer(Diagnostic diag) throws SQLException {

        String sql = "DELETE FROM diagnostic WHERE idDiagnostic=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, diag.getIdDiagnostic());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {

    }

    @Override
    public void modifier(Diagnostic diag) throws SQLException {

        String sql = """
                UPDATE diagnostic SET
                dateDiagnostic=?,
                etat=?,
                symptomes=?,
                informationsComplementaires=?,
                recommandations=?,
                idCulture=?,
                idUser=?,
                disease_detected=?,
                confidence=?,
                rapport_ia=?
                WHERE idDiagnostic=?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(diag.getDateDiagnostic()));
            ps.setString(2, diag.getEtat());
            ps.setString(3, diag.getSymptomes());
            ps.setString(4, diag.getInformationsComplementaires());
            ps.setString(5, diag.getRecommandations());
            ps.setInt(6, diag.getIdCulture());
            ps.setInt(7, diag.getIdUser());
            ps.setString(8, diag.getDiseaseDetected());
            ps.setDouble(9, diag.getConfidence());
            ps.setString(10, diag.getRapportIa());
            ps.setInt(11, diag.getIdDiagnostic());

            ps.executeUpdate();
        }
    }

    @Override
    public void lire(Diagnostic diag) throws SQLException {

        String sql = "SELECT * FROM diagnostic WHERE idDiagnostic=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, diag.getIdDiagnostic());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                diag.setIdCulture(rs.getInt("idCulture"));
                diag.setDateDiagnostic(rs.getDate("dateDiagnostic").toLocalDate());
                diag.setEtat(rs.getString("etat"));
                diag.setSymptomes(rs.getString("symptomes"));
                diag.setInformationsComplementaires(rs.getString("informationsComplementaires"));
                diag.setRecommandations(rs.getString("recommandations"));
                diag.setIdUser(rs.getInt("idUser"));
                diag.setDiseaseDetected(rs.getString("disease_detected"));
                diag.setConfidence(rs.getDouble("confidence"));
                diag.setRapportIa(rs.getString("rapport_ia"));
            }
        }
    }

    public List<Diagnostic> getByCultureId(int cultureId) throws SQLException {

        List<Diagnostic> list = new ArrayList<>();
        String sql = "SELECT * FROM diagnostic WHERE idCulture=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, cultureId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Diagnostic d = new Diagnostic();

                d.setIdDiagnostic(rs.getInt("idDiagnostic"));
                d.setIdCulture(rs.getInt("idCulture"));
                d.setDateDiagnostic(rs.getDate("dateDiagnostic").toLocalDate());
                d.setEtat(rs.getString("etat"));
                d.setSymptomes(rs.getString("symptomes"));
                d.setInformationsComplementaires(rs.getString("informationsComplementaires"));
                d.setRecommandations(rs.getString("recommandations"));
                d.setIdUser(rs.getInt("idUser"));
                d.setDiseaseDetected(rs.getString("disease_detected"));
                d.setConfidence(rs.getDouble("confidence"));
                d.setRapportIa(rs.getString("rapport_ia"));

                list.add(d);
            }
        }

        return list;
    }
}
