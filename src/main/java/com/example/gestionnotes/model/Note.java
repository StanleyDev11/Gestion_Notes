package com.example.gestionnotes.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Représente une note, incluant la note de devoir, la note d'examen, la moyenne calculée et le statut de validation.
 * Utilise les JavaFX Properties pour la mise à jour automatique de l'interface.
 */
public class Note {
    private final SimpleIntegerProperty id;
    private final SimpleIntegerProperty etudiantId;
    private final SimpleStringProperty nomEtudiant;
    private final SimpleStringProperty prenomEtudiant;
    private final SimpleStringProperty matiere;
    private final SimpleDoubleProperty noteDevoir;
    private final SimpleDoubleProperty noteExamen;
    private final SimpleDoubleProperty moyenne; // Propriété calculée
    private final SimpleStringProperty statutValidation; // Propriété calculée

    public Note(int id, int etudiantId, String nomEtudiant, String prenomEtudiant, String matiere, double noteDevoir, double noteExamen) {
        this.id = new SimpleIntegerProperty(id);
        this.etudiantId = new SimpleIntegerProperty(etudiantId);
        this.nomEtudiant = new SimpleStringProperty(nomEtudiant);
        this.prenomEtudiant = new SimpleStringProperty(prenomEtudiant);
        this.matiere = new SimpleStringProperty(matiere);
        this.noteDevoir = new SimpleDoubleProperty(noteDevoir);
        this.noteExamen = new SimpleDoubleProperty(noteExamen);

        // Calcul de la moyenne et du statut de validation
        double calculatedMoyenne = (noteDevoir * 0.40) + (noteExamen * 0.60);
        this.moyenne = new SimpleDoubleProperty(calculatedMoyenne);
        this.statutValidation = new SimpleStringProperty(calculatedMoyenne >= 10 ? "Validé" : "Non validé");
    }

    // --- Getters pour les propriétés JavaFX ---
    public SimpleIntegerProperty idProperty() {
        return id;
    }

    public SimpleStringProperty nomEtudiantProperty() {
        return nomEtudiant;
    }

    public SimpleStringProperty prenomEtudiantProperty() {
        return prenomEtudiant;
    }

    public SimpleStringProperty matiereProperty() {
        return matiere;
    }

    public SimpleDoubleProperty noteDevoirProperty() {
        return noteDevoir;
    }

    public SimpleDoubleProperty noteExamenProperty() {
        return noteExamen;
    }

    public SimpleDoubleProperty moyenneProperty() {
        return moyenne;
    }

    public SimpleStringProperty statutValidationProperty() {
        return statutValidation;
    }

    // --- Getters et Setters pour les valeurs brutes ---
    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public int getEtudiantId() {
        return etudiantId.get();
    }

    public void setEtudiantId(int etudiantId) {
        this.etudiantId.set(etudiantId);
    }

    public String getMatiere() {
        return matiere.get();
    }

    public void setMatiere(String matiere) {
        this.matiere.set(matiere);
    }

    public double getNoteDevoir() {
        return noteDevoir.get();
    }

    public void setNoteDevoir(double noteDevoir) {
        this.noteDevoir.set(noteDevoir);
        updateCalculatedProperties();
    }

    public double getNoteExamen() {
        return noteExamen.get();
    }

    public void setNoteExamen(double noteExamen) {
        this.noteExamen.set(noteExamen);
        updateCalculatedProperties();
    }

    public String getNomEtudiant() {
        return nomEtudiant.get();
    }

    public String getPrenomEtudiant() {
        return prenomEtudiant.get();
    }

    public double getMoyenne() {
        return moyenne.get();
    }

    public String getStatutValidation() {
        return statutValidation.get();
    }

    /**
     * Met à jour la moyenne et le statut de validation après modification des notes.
     */
    private void updateCalculatedProperties() {
        double calculatedMoyenne = (noteDevoir.get() * 0.40) + (noteExamen.get() * 0.60);
        moyenne.set(calculatedMoyenne);
        statutValidation.set(calculatedMoyenne >= 10 ? "Validé" : "Non validé");
    }
}