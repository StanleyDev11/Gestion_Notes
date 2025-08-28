package com.example.gestionnotes.controller;

import com.example.gestionnotes.dao.EtudiantDAO;
import com.example.gestionnotes.dao.NoteDAO;
import com.example.gestionnotes.model.Etudiant;
import com.example.gestionnotes.model.Note;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.text.DecimalFormat; // Pour formater la moyenne

/**
 * Contrôleur principal de l'application de gestion des notes.
 * Gère les interactions entre la vue (MainView.fxml) et les modèles/DAOs.
 */
public class MainController {

    // --- Composants FXML injectés ---
    @FXML
    private ComboBox<String> filiereFilterComboBox;
    
    @FXML
    private FlowPane studentCardsContainer; // Nouveau conteneur pour les cartes étudiants
    @FXML
    private Label studentCountLabel; // Nouveau label pour le nombre d'étudiants
    @FXML
    private TextField searchField; // Nouveau champ de recherche
    @FXML
    private Label selectedStudentLabel; // Label pour l'étudiant sélectionné
    @FXML
    private Label studentInfoLabel; // Label pour les infos de l'étudiant sélectionné
    @FXML
    private Label studentAverageLabel; // Label pour la moyenne générale de l'étudiant sélectionné
    @FXML
    private TextField matiereTextField;
    @FXML
    private TextField noteDevoirTextField;
    @FXML
    private TextField noteExamenTextField;
    @FXML
    private Label moyenneCalculatedLabel; // Label pour la moyenne calculée du formulaire
    @FXML
    private Label statutValidationLabel; // Label pour le statut de validation du formulaire
    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button clearButton;
    @FXML
    private Button calculateButton;
    @FXML
    private TableView<Note> tableViewNotes;
    @FXML
    private TableColumn<Note, String> matiereColumn;
    @FXML
    private TableColumn<Note, Double> noteDevoirColumn;
    @FXML
    private TableColumn<Note, Double> noteExamenColumn;
    @FXML
    private TableColumn<Note, Double> moyenneColumn;
    @FXML
    private TableColumn<Note, String> statutValidationColumn;
    @FXML
    private Label notesCountLabel; // Nouveau label pour le nombre de notes
    @FXML
    private Label totalStudentsLabel; // Nouveau label pour les stats du footer
    @FXML
    private Label globalAverageLabel; // Nouveau label pour les stats du footer
    @FXML
    private Label successRateLabel; // Nouveau label pour les stats du footer

    @FXML
    private Label statusMessageLabel;

    @FXML
    private Label clockLabel;
    

    @FXML
    private TabPane tabPane;

    @FXML
    private Tab studentManagementTab;

    @FXML
    private StudentController studentViewController;

    // --- Instances des DAOs ---
    private NoteDAO noteDAO;
    private EtudiantDAO etudiantDAO;

    // --- Liste observable pour le TableView des notes ---
    private ObservableList<Note> noteList;

    // --- Étudiant actuellement sélectionné ---
    private Etudiant selectedEtudiantForNotes;

    // Format pour la moyenne
    private static final DecimalFormat df = new DecimalFormat("#.##");

    /**
     * Méthode d'initialisation appelée automatiquement après le chargement du FXML.
     * Configure le TableView, charge les données initiales et met en place les écouteurs.
     */
    @FXML
    public void initialize() {
        noteDAO = new NoteDAO();
        etudiantDAO = new EtudiantDAO();

        // Initialisation des colonnes du TableView des notes
        matiereColumn.setCellValueFactory(new PropertyValueFactory<>("matiere"));
        noteDevoirColumn.setCellValueFactory(new PropertyValueFactory<>("noteDevoir"));
        noteExamenColumn.setCellValueFactory(new PropertyValueFactory<>("noteExamen"));
        moyenneColumn.setCellValueFactory(new PropertyValueFactory<>("moyenne"));
        statutValidationColumn.setCellValueFactory(new PropertyValueFactory<>("statutValidation"));

        // Charger les filières
        loadFilieres();

        // Configurer l'écouteur pour le ComboBox des filières
        filiereFilterComboBox.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> filterAndDisplayStudents());

        

        // Configurer l'écouteur pour le champ de recherche
        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> filterAndDisplayStudents());

        // Écouteur de sélection sur le TableView des notes pour la modification/suppression
        tableViewNotes.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showNoteDetails(newValue));

        // Écouteurs pour les champs de note pour le calcul en temps réel
        noteDevoirTextField.textProperty().addListener((obs, oldVal, newVal) -> handleCalculateButtonAction());
        noteExamenTextField.textProperty().addListener((obs, oldVal, newVal) -> handleCalculateButtonAction());

        // Afficher tous les étudiants au démarrage
        filterAndDisplayStudents();

        // Désactiver les boutons de gestion des notes au démarrage
        setNoteButtonsDisabled(true);

        // Calculer et afficher les statistiques du footer
        updateFooterStatistics();

        // Initialiser le contrôleur de la vue étudiant
        studentViewController.setMainController(this);

        // Initialiser l'horloge
        initClock();

        // Setup icons
        setupIcons();
    }

    /**
     * Charge les filières distinctes depuis la base de données et remplit le ComboBox.
     */
    private void loadFilieres() {
        List<String> filieres = etudiantDAO.getAllFilieres();
        ObservableList<String> observableFilieres = FXCollections.observableArrayList("Toutes les filières");
        observableFilieres.addAll(filieres);
        filiereFilterComboBox.setItems(observableFilieres);
        filiereFilterComboBox.getSelectionModel().selectFirst(); // Sélectionne "Toutes les filières" par défaut
    }

    

    /**
     * Filtre et affiche les étudiants en fonction de la filière et du texte de recherche.
     */
    private void filterAndDisplayStudents() {
        studentCardsContainer.getChildren().clear(); // Nettoie les cartes existantes
        String selectedFiliere = filiereFilterComboBox.getSelectionModel().getSelectedItem();
        String searchText = searchField.getText().toLowerCase();

        List<Etudiant> allStudents = etudiantDAO.getAllEtudiants();
        List<Etudiant> filteredStudents = new java.util.ArrayList<>();

        // If a specific filiere is selected, or if a search is being performed
        if ((selectedFiliere != null && !selectedFiliere.equals("Toutes les filières")) || !searchText.isEmpty()) {
            for (Etudiant etudiant : allStudents) {
                boolean matchesFiliere = (selectedFiliere == null || selectedFiliere.equals("Toutes les filières") || etudiant.getFiliere().equals(selectedFiliere));
                boolean matchesSearch = (searchText.isEmpty() ||
                                         etudiant.getNom().toLowerCase().contains(searchText) ||
                                         etudiant.getPrenom().toLowerCase().contains(searchText) ||
                                         etudiant.getFiliere().toLowerCase().contains(searchText));

                if (matchesFiliere && matchesSearch) {
                    filteredStudents.add(etudiant);
                }
            }
        }

        studentCountLabel.setText("(" + filteredStudents.size() + ")");

        if (filteredStudents.isEmpty()) {
            if (searchText.isEmpty() && (selectedFiliere == null || selectedFiliere.equals("Toutes les filières"))) {
                studentCardsContainer.getChildren().add(new Label("Veuillez sélectionner une filière ou lancer une recherche."));
            } else {
                studentCardsContainer.getChildren().add(new Label("Aucun étudiant trouvé avec ces critères."));
            }
        } else {
            for (Etudiant etudiant : filteredStudents) {
                VBox studentCard = createStudentCard(etudiant);
                studentCardsContainer.getChildren().add(studentCard);
            }
        }
    }

    /**
     * Crée une "carte" visuelle pour un étudiant.
     * @param etudiant L'étudiant pour lequel créer la carte.
     * @return Une VBox représentant la carte de l'étudiant.
     */
    private VBox createStudentCard(Etudiant etudiant) {
        System.out.println("Creating student card for: " + etudiant.getNom() + " " + etudiant.getPrenom());
        VBox card = new VBox(5);
        card.getStyleClass().add("student-card");
        card.setPrefSize(150, 80); // Taille fixe pour les cartes
        card.setAlignment(javafx.geometry.Pos.CENTER);

        Label nameLabel = new Label(etudiant.getPrenom() + " " + etudiant.getNom());
        nameLabel.getStyleClass().add("student-card-name");
        nameLabel.setWrapText(true);

        Label filiereLabel = new Label(etudiant.getFiliere());
        filiereLabel.getStyleClass().add("student-card-filiere");

        card.getChildren().addAll(nameLabel, filiereLabel);

        // Gérer la sélection de la carte
        card.setOnMouseClicked(event -> {
            System.out.println("Student card clicked: " + etudiant.getNom() + " " + etudiant.getPrenom());
            // Désélectionner toutes les autres cartes
            for (javafx.scene.Node node : studentCardsContainer.getChildren()) {
                node.getStyleClass().remove("student-card-selected");
            }
            // Sélectionner la carte actuelle
            card.getStyleClass().add("student-card-selected");
            selectStudentForNotes(etudiant);
        });

        return card;
    }

    /**
     * Sélectionne un étudiant pour la saisie des notes et met à jour l'interface.
     * @param etudiant L'étudiant sélectionné.
     */
    private void selectStudentForNotes(Etudiant etudiant) {
        System.out.println("Selecting student for notes: " + etudiant.getNom() + " " + etudiant.getPrenom());
        this.selectedEtudiantForNotes = etudiant;
        selectedStudentLabel.setText("Saisie des notes pour :");
        studentInfoLabel.setText(etudiant.getPrenom() + " " + etudiant.getNom() + " (" + etudiant.getFiliere() + ")");
        
        // Calculer et afficher la moyenne générale de l'étudiant
        double overallAverage = noteDAO.getAllNotes().stream()
                .filter(note -> note.getEtudiantId() == etudiant.getId())
                .mapToDouble(Note::getMoyenne)
                .average()
                .orElse(0.0);
        studentAverageLabel.setText("Moyenne générale: " + df.format(overallAverage));
        studentAverageLabel.getStyleClass().clear();
        studentAverageLabel.getStyleClass().add(overallAverage >= 10 ? "status-valide" : "status-non-valide");


        loadNotesForSelectedStudent();
        setNoteButtonsDisabled(false); // Activer les boutons de gestion des notes
        clearNoteFields();
    }

    /**
     * Charge les notes de l'étudiant actuellement sélectionné dans le TableView.
     */
    private void loadNotesForSelectedStudent() {
        if (selectedEtudiantForNotes != null) {
            noteList = FXCollections.observableArrayList(noteDAO.getAllNotes().stream()
                    .filter(note -> note.getEtudiantId() == selectedEtudiantForNotes.getId())
                    .collect(java.util.stream.Collectors.toList()));
            tableViewNotes.setItems(noteList);
            notesCountLabel.setText("(" + noteList.size() + " notes)");
        } else {
            tableViewNotes.setItems(FXCollections.emptyObservableList());
            notesCountLabel.setText("(0 notes)");
        }
    }

    /**
     * Affiche les détails de la note sélectionnée dans les champs de saisie.
     * @param note La note sélectionnée dans le TableView.
     */
    private void showNoteDetails(Note note) {
        if (note != null) {
            matiereTextField.setText(note.getMatiere());
            noteDevoirTextField.setText(String.valueOf(note.getNoteDevoir()));
            noteExamenTextField.setText(String.valueOf(note.getNoteExamen()));
            
            // Calculer et afficher la moyenne et le statut pour la note sélectionnée
            double currentMoyenne = (note.getNoteDevoir() * 0.40) + (note.getNoteExamen() * 0.60);
            moyenneCalculatedLabel.setText(df.format(currentMoyenne));
            statutValidationLabel.setText(currentMoyenne >= 10 ? "Validé" : "Non validé");
            statutValidationLabel.getStyleClass().clear();
            statutValidationLabel.getStyleClass().add(currentMoyenne >= 10 ? "status-valide" : "status-non-valide");


            addButton.setDisable(true); // Désactive le bouton Ajouter en mode modification/suppression
            updateButton.setDisable(false);
            deleteButton.setDisable(false);
        } else {
            clearNoteFields();
            addButton.setDisable(false); // Active le bouton Ajouter en mode ajout
            updateButton.setDisable(true);
            deleteButton.setDisable(true);
        }
    }

    /**
     * Active ou désactive les boutons d'ajout, modification et suppression de notes.
     * @param disabled true pour désactiver, false pour activer.
     */
    private void setNoteButtonsDisabled(boolean disabled) {
        addButton.setDisable(disabled);
        updateButton.setDisable(disabled);
        deleteButton.setDisable(disabled);
        matiereTextField.setDisable(disabled);
        noteDevoirTextField.setDisable(disabled);
        noteExamenTextField.setDisable(disabled);
    }

    /**
     * Gère l'action du bouton "Calculer".
     * Calcule et affiche la moyenne et le statut de validation dans le formulaire.
     */
    @FXML
    private void handleCalculateButtonAction() {
        try {
            double noteDevoir = Double.parseDouble(noteDevoirTextField.getText());
            double noteExamen = Double.parseDouble(noteExamenTextField.getText());

            if (noteDevoir < 0 || noteDevoir > 20 || noteExamen < 0 || noteExamen > 20) {
                moyenneCalculatedLabel.setText("N/A");
                statutValidationLabel.setText("Invalide");
                statutValidationLabel.getStyleClass().clear();
                statutValidationLabel.getStyleClass().add("status-non-valide");
                return;
            }

            double moyenne = (noteDevoir * 0.40) + (noteExamen * 0.60);
            moyenneCalculatedLabel.setText(df.format(moyenne));
            String statut = (moyenne >= 10) ? "Validé" : "Non validé";
            statutValidationLabel.setText(statut);
            statutValidationLabel.getStyleClass().clear();
            statutValidationLabel.getStyleClass().add(statut.equals("Validé") ? "status-valide" : "status-non-valide");

        } catch (NumberFormatException e) {
            moyenneCalculatedLabel.setText("N/A");
            statutValidationLabel.setText("Invalide");
            statutValidationLabel.getStyleClass().clear();
            statutValidationLabel.getStyleClass().add("status-non-valide");
        }
    }

    /**
     * Gère l'action du bouton "Ajouter Note".
     * Ajoute une nouvelle note à la base de données après validation.
     */
    @FXML
    private void handleAddButtonAction() {
        if (selectedEtudiantForNotes == null) {
            showStatusMessage("Veuillez sélectionner un étudiant avant d'ajouter une note.", "status-error", 3);
            return;
        }
        if (isNoteInputValid()) {
            String matiere = matiereTextField.getText();
            double noteDevoir = Double.parseDouble(noteDevoirTextField.getText());
            double noteExamen = Double.parseDouble(noteExamenTextField.getText());

            Note newNote = new Note(0, selectedEtudiantForNotes.getId(),
                    selectedEtudiantForNotes.getNom(), selectedEtudiantForNotes.getPrenom(),
                    matiere, noteDevoir, noteExamen);

            if (noteDAO.addNote(newNote)) {
                showStatusMessage("Note ajoutée avec succès !", "status-success", 3);
                loadNotesForSelectedStudent(); // Rafraîchir le TableView
                clearNoteFields();
                updateFooterStatistics(); // Mettre à jour les stats du footer
            } else {
                showStatusMessage("Échec de l'ajout de la note.", "status-error", 3);
            }
        }
    }

    /**
     * Gère l'action du bouton "Modifier Note".
     * Met à jour la note sélectionnée dans la base de données après validation.
     */
    @FXML
    private void handleUpdateButtonAction() {
        Note selectedNote = tableViewNotes.getSelectionModel().getSelectedItem();
        if (selectedNote != null) {
            if (isNoteInputValid()) {
                selectedNote.setMatiere(matiereTextField.getText());
                selectedNote.setNoteDevoir(Double.parseDouble(noteDevoirTextField.getText()));
                selectedNote.setNoteExamen(Double.parseDouble(noteExamenTextField.getText()));

                if (noteDAO.updateNote(selectedNote)) {
                    showStatusMessage("Note modifiée avec succès !", "status-success", 3);
                    loadNotesForSelectedStudent(); // Rafraîchir le TableView
                    clearNoteFields();
                    updateFooterStatistics(); // Mettre à jour les stats du footer
                } else {
                    showStatusMessage("Échec de la modification de la note.", "status-error", 3);
                }
            }
        } else {
            showStatusMessage("Veuillez sélectionner une note à modifier dans le tableau.", "status-warning", 3);
        }
    }

    /**
     * Gère l'action du bouton "Supprimer Note".
     * Supprime la note sélectionnée de la base de données.
     */
    @FXML
    private void handleDeleteButtonAction() {
        Note selectedNote = tableViewNotes.getSelectionModel().getSelectedItem();
        if (selectedNote != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation de suppression");
            alert.setHeaderText("Supprimer la note ?");
            alert.setContentText("Êtes-vous sûr de vouloir supprimer la note de " + selectedEtudiantForNotes.getPrenom() + " " + selectedEtudiantForNotes.getNom() + " en " + selectedNote.getMatiere() + " ?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (noteDAO.deleteNote(selectedNote.getId())) {
                    showStatusMessage("Note supprimée avec succès !", "status-success", 3);
                    loadNotesForSelectedStudent(); // Rafraîchir le TableView
                    clearNoteFields();
                    updateFooterStatistics(); // Mettre à jour les stats du footer
                } else {
                    showStatusMessage("Échec de la suppression de la note.", "status-error", 3);
                }
            }
        }
        else {
            showStatusMessage("Veuillez sélectionner une note à supprimer dans le tableau.", "status-warning", 3);
        }
    }

    /**
     * Gère l'action du bouton "Effacer".
     * Réinitialise les champs de saisie des notes et la sélection du tableau.
     */
    @FXML
    private void handleClearButtonAction() {
        clearNoteFields();
        tableViewNotes.getSelectionModel().clearSelection();
    }

    /**
     * Valide les entrées utilisateur dans les champs de texte de la note.
     * @return true si les entrées sont valides, false sinon.
     */
    private boolean isNoteInputValid() {
        String errorMessage = "";

        if (matiereTextField.getText() == null || matiereTextField.getText().isEmpty()) {
            errorMessage += "Veuillez entrer une matière !\n";
        }

        // Validation pour noteDevoir
        if (noteDevoirTextField.getText() == null || noteDevoirTextField.getText().isEmpty()) {
            errorMessage += "Veuillez entrer la note de devoir !\n";
        } else {
            try {
                double noteValue = Double.parseDouble(noteDevoirTextField.getText());
                if (noteValue < 0 || noteValue > 20) {
                    errorMessage += "La note de devoir doit être comprise entre 0 et 20 !\n";
                }
            } catch (NumberFormatException e) {
                errorMessage += "La note de devoir doit être un nombre valide !\n";
            }
        }

        // Validation pour noteExamen
        if (noteExamenTextField.getText() == null || noteExamenTextField.getText().isEmpty()) {
            errorMessage += "Veuillez entrer la note d'examen !\n";
        }
        else {
            try {
                double noteValue = Double.parseDouble(noteExamenTextField.getText());
                if (noteValue < 0 || noteValue > 20) {
                    errorMessage += "La note d'examen doit être comprise entre 0 et 20 !\n";
                }
            } catch (NumberFormatException e) {
                errorMessage += "La note d'examen doit être un nombre valide !\n";
            }
        }

        if (errorMessage.isEmpty()) {
            return true;
        }
        else {
            showStatusMessage("Veuillez corriger les erreurs suivantes : " + errorMessage, "status-error", 5);
            return false;
        }
    }

    /**
     * Réinitialise tous les champs de saisie des notes.
     */
    private void clearNoteFields() {
        matiereTextField.setText("");
        noteDevoirTextField.setText("");
        noteExamenTextField.setText("");
        moyenneCalculatedLabel.setText("-");
        statutValidationLabel.setText("-");
        statutValidationLabel.getStyleClass().clear(); // Nettoyer les styles de statut
        // Réactiver/désactiver les boutons selon la sélection
        if (selectedEtudiantForNotes != null) {
            addButton.setDisable(false);
            updateButton.setDisable(true);
            deleteButton.setDisable(true);
        }
        else {
            setNoteButtonsDisabled(true);
        }
    }

    

    public void refreshStudentView() {
        loadFilieres();
        filterAndDisplayStudents();
    }

    private void initClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            clockLabel.setText(LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void setupIcons() {
        addButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PLUS));
        updateButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PENCIL));
        deleteButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
        clearButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TIMES));
        calculateButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.CALCULATOR));
    }

    /**
     * Met à jour les statistiques affichées dans le pied de page.
     */
    private void updateFooterStatistics() {
        List<Etudiant> allEtudiants = etudiantDAO.getAllEtudiants();
        List<Note> allNotes = noteDAO.getAllNotes();

        totalStudentsLabel.setText(String.valueOf(allEtudiants.size()));

        double totalMoyenne = 0.0;
        int validatedCount = 0;
        if (!allNotes.isEmpty()) {
            for (Note note : allNotes) {
                totalMoyenne += note.getMoyenne();
                if (note.getMoyenne() >= 10) {
                    validatedCount++;
                }
            }
            globalAverageLabel.setText(df.format(totalMoyenne / allNotes.size()));
            successRateLabel.setText(df.format((double) validatedCount / allNotes.size() * 100) + "%");
        } else {
            globalAverageLabel.setText("0.00");
            successRateLabel.setText("0%");
        }
    }

    /**
     * Affiche une boîte de dialogue d'alerte.
     * @param alertType Le type d'alerte (ERROR, INFORMATION, WARNING, etc.).
     * @param title Le titre de la boîte de dialogue.
     * @param message Le message à afficher.
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showStatusMessage(String message, String styleClass, int durationSeconds) {
        statusMessageLabel.setText(message);
        statusMessageLabel.getStyleClass().clear();
        statusMessageLabel.getStyleClass().add("status-message"); // Base style
        statusMessageLabel.getStyleClass().add(styleClass);

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(durationSeconds), event -> {
            statusMessageLabel.setText("");
            statusMessageLabel.getStyleClass().clear();
            statusMessageLabel.getStyleClass().add("status-message");
        }));
        timeline.play();
    }
}