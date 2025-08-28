package com.example.gestionnotes;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Classe principale de l'application de gestion des notes.
 * Elle étend `Application` de JavaFX et est le point d'entrée de l'interface graphique.
 */
public class Main extends Application {

    /**
     * Méthode `start` appelée au lancement de l'application JavaFX.
     * Charge le fichier FXML de l'interface utilisateur et affiche la fenêtre.
     * @param stage Le stage (fenêtre principale) de l'application.
     * @throws IOException Si le fichier FXML ne peut pas être chargé.
     */
    @Override
    public void start(Stage stage) throws IOException {
        // Charge le fichier FXML de la vue principale
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("MainView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600); // Définit la taille initiale de la fenêtre
        stage.setTitle("Gestion des Notes"); // Titre de la fenêtre
        stage.setScene(scene); // Définit la scène pour le stage
        stage.show(); // Affiche la fenêtre
    }

    /**
     * Méthode `main` : point d'entrée standard pour une application Java.
     * Appelle `launch()` qui initialise JavaFX et appelle la méthode `start`.
     * @param args Arguments de la ligne de commande.
     */
    public static void main(String[] args) {
        launch();
    }
}
