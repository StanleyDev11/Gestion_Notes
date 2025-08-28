package com.example.gestionnotes.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gère la connexion à la base de données MySQL.
 * Utilise le pattern Singleton pour garantir une seule instance de connexion.
 */
public class DBConnection {

    // --- ATTENTION : Configurez ces informations pour votre base de données --- //
    private static final String JDBC_URL = "jdbc:mysql://192.168.1.94:3306/Db_Tennis?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "ace3i"; // Votre nom d'utilisateur
    private static final String DB_PASSWORD = "africa@2025"; // Votre mot de passe
    // ------------------------------------------------------------------------- //

    private static Connection connection = null;

    /**
     * Fournit un point d'accès global à l'instance de connexion.
     * Si la connexion n'existe pas, elle est créée.
     *
     * @return L'instance de la connexion à la base de données.
     * @throws SQLException si la connexion échoue.
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Chargement du driver JDBC pour MySQL
                Class.forName("com.mysql.cj.jdbc.Driver");
                // Établissement de la connexion
                connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
            } catch (ClassNotFoundException e) {
                System.err.println("Erreur: Driver MySQL non trouvé.");
                e.printStackTrace();
                throw new SQLException("Driver non trouvé", e);
            }
        }
        return connection;
    }

    /**
     * Méthode utilitaire pour fermer la connexion.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                System.err.println("Erreur lors de la fermeture de la connexion.");
                e.printStackTrace();
            }
        }
    }
}
