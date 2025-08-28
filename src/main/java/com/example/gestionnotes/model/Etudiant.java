package com.example.gestionnotes.model;

/**
 * Représente un étudiant avec ses informations de base.
 * C'est un simple POJO (Plain Old Java Object).
 */
public class Etudiant {
    private int id;
    private String nom;
    private String prenom;
    private String filiere; // Nouveau champ

    public Etudiant(int id, String nom, String prenom, String filiere) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.filiere = filiere;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getFiliere() { // Getter pour filiere
        return filiere;
    }

    public void setFiliere(String filiere) { // Setter pour filiere
        this.filiere = filiere;
    }

    /**
     * Représentation textuelle de l'étudiant, utile pour les ComboBox.
     * @return Le nom complet de l'étudiant.
     */
    @Override
    public String toString() {
        return prenom + " " + nom + " (" + filiere + ")";
    }
}