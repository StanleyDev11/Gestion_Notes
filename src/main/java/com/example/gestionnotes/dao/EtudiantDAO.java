package com.example.gestionnotes.dao;

import com.example.gestionnotes.model.Etudiant;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour les opérations CRUD sur l'entité Etudiant.
 */
public class EtudiantDAO {

    /**
     * Récupère tous les étudiants de la base de données.
     * @return Une liste d'objets Etudiant.
     */
    public List<Etudiant> getAllEtudiants() {
        List<Etudiant> etudiants = new ArrayList<>();
        String sql = "SELECT * FROM etudiant ORDER BY nom, prenom";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                String filiere = rs.getString("filiere");
                etudiants.add(new Etudiant(id, nom, prenom, filiere));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return etudiants;
    }

    /**
     * Récupère les étudiants d'une filière spécifique.
     * @param filiere La filière à filtrer.
     * @return Une liste d'objets Etudiant appartenant à la filière spécifiée.
     */
    public List<Etudiant> getEtudiantsByFiliere(String filiere) {
        List<Etudiant> etudiants = new ArrayList<>();
        String sql = "SELECT * FROM etudiant WHERE filiere = ? ORDER BY nom, prenom";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, filiere);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String nom = rs.getString("nom");
                    String prenom = rs.getString("prenom");
                    String etudiantFiliere = rs.getString("filiere");
                    etudiants.add(new Etudiant(id, nom, prenom, etudiantFiliere));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return etudiants;
    }

    /**
     * Récupère toutes les filières distinctes de la base de données.
     * @return Une liste de chaînes de caractères représentant les filières.
     */
    public List<String> getAllFilieres() {
        List<String> filieres = new ArrayList<>();
        String sql = "SELECT DISTINCT filiere FROM etudiant ORDER BY filiere";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                filieres.add(rs.getString("filiere"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return filieres;
    }

    /**
     * Ajoute un nouvel étudiant dans la base de données.
     * @param etudiant L'objet Etudiant à ajouter.
     * @return true si l'ajout a réussi, false sinon.
     */
    public boolean addEtudiant(Etudiant etudiant) {
        String sql = "INSERT INTO etudiant (nom, prenom, filiere) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, etudiant.getNom());
            pstmt.setString(2, etudiant.getPrenom());
            pstmt.setString(3, etudiant.getFiliere());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        etudiant.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Met à jour un étudiant existant dans la base de données.
     * @param etudiant L'objet Etudiant contenant les nouvelles informations.
     * @return true si la mise à jour a réussi, false sinon.
     */
    public boolean updateEtudiant(Etudiant etudiant) {
        String sql = "UPDATE etudiant SET nom = ?, prenom = ?, filiere = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, etudiant.getNom());
            pstmt.setString(2, etudiant.getPrenom());
            pstmt.setString(3, etudiant.getFiliere());
            pstmt.setInt(4, etudiant.getId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Supprime un étudiant de la base de données.
     * @param etudiantId L'ID de l'étudiant à supprimer.
     * @return true si la suppression a réussi, false sinon.
     */
    public boolean deleteEtudiant(int etudiantId) {
        String sql = "DELETE FROM etudiant WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, etudiantId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addEtudiants(List<Etudiant> etudiants) {
        String sql = "INSERT INTO etudiant (nom, prenom, filiere) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false); // Start transaction

            for (Etudiant etudiant : etudiants) {
                pstmt.setString(1, etudiant.getNom());
                pstmt.setString(2, etudiant.getPrenom());
                pstmt.setString(3, etudiant.getFiliere());
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            conn.commit(); // Commit transaction
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            // Consider rolling back in a real app
            return false;
        }
    }
}