package com.example.gestionnotes.dao;

import com.example.gestionnotes.model.Note;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour les opérations CRUD (Create, Read, Update, Delete) sur l'entité Note.
 */
public class NoteDAO {

    /**
     * Récupère toutes les notes de la base de données avec les informations de l'étudiant.
     * Utilise une jointure SQL pour combiner les tables 'note' et 'etudiant'.
     * @return Une liste d'objets Note.
     */
    public List<Note> getAllNotes() {
        List<Note> notes = new ArrayList<>();
        // La requête SQL joint les tables note et etudiant pour récupérer le nom et prénom
        String sql = "SELECT n.id, n.etudiant_id, e.nom, e.prenom, n.matiere, n.note_devoir, n.note_examen " +
                     "FROM note n JOIN etudiant e ON n.etudiant_id = e.id ORDER BY e.nom, n.matiere";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                int etudiantId = rs.getInt("etudiant_id");
                String nomEtudiant = rs.getString("nom");
                String prenomEtudiant = rs.getString("prenom");
                String matiere = rs.getString("matiere");
                double noteDevoir = rs.getDouble("note_devoir"); // Nouveau champ
                double noteExamen = rs.getDouble("note_examen"); // Nouveau champ
                notes.add(new Note(id, etudiantId, nomEtudiant, prenomEtudiant, matiere, noteDevoir, noteExamen));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notes;
    }

    /**
     * Ajoute une nouvelle note dans la base de données.
     * @param note L'objet Note à ajouter (seuls etudiant_id, matiere, noteDevoir et noteExamen sont utilisés).
     * @return true si l'ajout a réussi, false sinon.
     */
    public boolean addNote(Note note) {
        String sql = "INSERT INTO note (etudiant_id, matiere, note_devoir, note_examen) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, note.getEtudiantId());
            pstmt.setString(2, note.getMatiere());
            pstmt.setDouble(3, note.getNoteDevoir()); // Nouveau champ
            pstmt.setDouble(4, note.getNoteExamen()); // Nouveau champ

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Met à jour une note existante.
     * @param note L'objet Note contenant les nouvelles informations.
     * @return true si la mise à jour a réussi, false sinon.
     */
    public boolean updateNote(Note note) {
        String sql = "UPDATE note SET matiere = ?, note_devoir = ?, note_examen = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, note.getMatiere());
            pstmt.setDouble(2, note.getNoteDevoir()); // Nouveau champ
            pstmt.setDouble(3, note.getNoteExamen()); // Nouveau champ
            pstmt.setInt(4, note.getId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Supprime une note de la base de données.
     * @param noteId L'ID de la note à supprimer.
     * @return true si la suppression a réussi, false sinon.
     */
    public boolean deleteNote(int noteId) {
        String sql = "DELETE FROM note WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, noteId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}