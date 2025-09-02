package com.example.gestionnotes.controller;

import com.example.gestionnotes.dao.EtudiantDAO;
import com.example.gestionnotes.dao.NoteDAO;
import com.example.gestionnotes.model.Etudiant;
import com.example.gestionnotes.model.Note;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.text.DecimalFormat; // Pour formater la moyenne
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import javafx.stage.FileChooser;
import javafx.util.Pair;

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
    private Button exportNotesButton;
    @FXML
    private Label totalStudentsLabel; // Nouveau label pour les stats du footer
    @FXML
    private Label globalAverageLabel; // Nouveau label pour les stats du footer
    @FXML
    private Label successRateLabel; // Nouveau label pour les stats du footer

    @FXML
    private Label statusMessageLabel;
    @FXML
    private ProgressIndicator mainLoadingSpinner;

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

    private PauseTransition debounceTimer;

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

        debounceTimer = new PauseTransition(Duration.millis(400));
        debounceTimer.setOnFinished(event -> loadStudentsTask());

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
                (observable, oldValue, newValue) -> loadStudentsTask());

        

        // Configurer l'écouteur pour le champ de recherche
        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> debounceTimer.playFromStart());

        // Écouteur de sélection sur le TableView des notes pour la modification/suppression
        tableViewNotes.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showNoteDetails(newValue));

        // Écouteurs pour les champs de note pour le calcul en temps réel
        noteDevoirTextField.textProperty().addListener((obs, oldVal, newVal) -> handleCalculateButtonAction());
        noteExamenTextField.textProperty().addListener((obs, oldVal, newVal) -> handleCalculateButtonAction());

        // Afficher tous les étudiants au démarrage
        loadStudentsTask();

        // Désactiver les boutons de gestion des notes au démarrage
        setNoteButtonsDisabled(true);

        // Calculer et afficher les statistiques du footer
        updateFooterStatisticsTask();

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
    private void loadStudentsTask() {
        Task<List<Etudiant>> task = new Task<>() {
            @Override
            protected List<Etudiant> call() throws Exception {
                String selectedFiliere = filiereFilterComboBox.getSelectionModel().getSelectedItem();
                String searchText = searchField.getText().toLowerCase();

                List<Etudiant> allStudents = etudiantDAO.getAllEtudiants();
                return allStudents.stream()
                        .filter(etudiant -> {
                            boolean matchesFiliere = (selectedFiliere == null || selectedFiliere.equals("Toutes les filières") || etudiant.getFiliere().equals(selectedFiliere));
                            boolean matchesSearch = (searchText.isEmpty() ||
                                                     etudiant.getNom().toLowerCase().contains(searchText) ||
                                                     etudiant.getPrenom().toLowerCase().contains(searchText) ||
                                                     etudiant.getFiliere().toLowerCase().contains(searchText));
                            return matchesFiliere && matchesSearch;
                        })
                        .collect(Collectors.toList());
            }
        };

        task.setOnSucceeded(event -> {
            List<Etudiant> students = task.getValue();
            updateStudentCards(students);
            unbindStudentListControls();
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            showStatusMessage("Erreur lors du chargement des étudiants.", "status-error", 5);
            unbindStudentListControls();
        });

        bindStudentListControlsToTask(task);
        new Thread(task).start();
    }

    private void bindStudentListControlsToTask(Task<?> task) {
        mainLoadingSpinner.visibleProperty().bind(task.runningProperty());
        filiereFilterComboBox.disableProperty().bind(task.runningProperty());
        searchField.disableProperty().bind(task.runningProperty());
        studentCardsContainer.disableProperty().bind(task.runningProperty());
    }

    private void unbindStudentListControls() {
        mainLoadingSpinner.visibleProperty().unbind();
        filiereFilterComboBox.disableProperty().unbind();
        searchField.disableProperty().unbind();
        studentCardsContainer.disableProperty().unbind();
    }

    private void updateStudentCards(List<Etudiant> students) {
        studentCardsContainer.getChildren().clear();
        studentCountLabel.setText("(" + students.size() + ")");

        if (students.isEmpty()) {
            FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
            icon.setSize("3em");
            icon.getStyleClass().add("glyph-icon");

            Label placeholderLabel = new Label("Aucun étudiant trouvé.");
            VBox placeholder = new VBox(10, icon, placeholderLabel);
            placeholder.getStyleClass().add("empty-placeholder");
            placeholder.setAlignment(Pos.CENTER);
            studentCardsContainer.getChildren().add(placeholder);
        } else {
            for (Etudiant etudiant : students) {
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
        VBox card = new VBox(5);
        card.getStyleClass().add("student-card");
        card.setPrefSize(150, 80); // Taille fixe pour les cartes
        card.setAlignment(Pos.CENTER);

        Label nameLabel = new Label(etudiant.getPrenom() + " " + etudiant.getNom());
        nameLabel.getStyleClass().add("student-card-name");
        nameLabel.setWrapText(true);

        Label filiereLabel = new Label(etudiant.getFiliere());
        filiereLabel.getStyleClass().add("student-card-filiere");

        card.getChildren().addAll(nameLabel, filiereLabel);

        // Gérer la sélection de la carte
        card.setOnMouseClicked(event -> {
            // Désélectionner toutes les autres cartes
            for (Node node : studentCardsContainer.getChildren()) {
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
        this.selectedEtudiantForNotes = etudiant;
        selectedStudentLabel.setText("Saisie des notes pour :");
        studentInfoLabel.setText(etudiant.getPrenom() + " " + etudiant.getNom() + " (" + etudiant.getFiliere() + ")");
        
        loadNotesForStudentTask(etudiant);
        setNoteButtonsDisabled(false); // Activer les boutons de gestion des notes
        clearNoteFields();
    }

    /**
     * Charge les notes de l'étudiant actuellement sélectionné dans le TableView.
     */
    private void loadNotesForStudentTask(Etudiant etudiant) {
        Task<Pair<List<Note>, Double>> task = new Task<>() {
            @Override
            protected Pair<List<Note>, Double> call() throws Exception {
                List<Note> notes = noteDAO.getAllNotes().stream()
                        .filter(note -> note.getEtudiantId() == etudiant.getId())
                        .collect(Collectors.toList());
                double average = notes.stream()
                        .mapToDouble(Note::getMoyenne)
                        .average()
                        .orElse(0.0);
                return new Pair<>(notes, average);
            }
        };

        task.setOnSucceeded(event -> {
            Pair<List<Note>, Double> result = task.getValue();
            noteList = FXCollections.observableArrayList(result.getKey());
            tableViewNotes.setItems(noteList);
            notesCountLabel.setText("(" + noteList.size() + " notes)");

            double overallAverage = result.getValue();
            studentAverageLabel.setText("Moyenne générale: " + df.format(overallAverage));
            studentAverageLabel.getStyleClass().clear();
            studentAverageLabel.getStyleClass().add(overallAverage >= 10 ? "status-valide" : "status-non-valide");
            mainLoadingSpinner.visibleProperty().unbind();
            tableViewNotes.disableProperty().unbind();
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            showStatusMessage("Erreur lors du chargement des notes de l'étudiant.", "status-error", 5);
            mainLoadingSpinner.visibleProperty().unbind();
            tableViewNotes.disableProperty().unbind();
        });

        mainLoadingSpinner.visibleProperty().bind(task.runningProperty());
        tableViewNotes.disableProperty().bind(task.runningProperty());

        new Thread(task).start();
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

    private void runNoteCUDTask(Supplier<Boolean> action, String successMessage, String failureMessage) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return action.get();
            }
        };

        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                showStatusMessage(successMessage, "status-success", 3);
                loadNotesForStudentTask(selectedEtudiantForNotes);
                clearNoteFields();
                updateFooterStatisticsTask();
            } else {
                showStatusMessage(failureMessage, "status-error", 3);
            }
            mainLoadingSpinner.visibleProperty().unbind();
            tabPane.disableProperty().unbind();
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            showStatusMessage("Erreur lors de l'opération sur la note.", "status-error", 5);
            mainLoadingSpinner.visibleProperty().unbind();
            tabPane.disableProperty().unbind();
        });

        mainLoadingSpinner.visibleProperty().bind(task.runningProperty());
        tabPane.disableProperty().bind(task.runningProperty());

        new Thread(task).start();
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
            Note newNote = new Note(0, selectedEtudiantForNotes.getId(),
                    selectedEtudiantForNotes.getNom(), selectedEtudiantForNotes.getPrenom(),
                    matiereTextField.getText(), Double.parseDouble(noteDevoirTextField.getText()), Double.parseDouble(noteExamenTextField.getText()));

            runNoteCUDTask(() -> noteDAO.addNote(newNote), "Note ajoutée avec succès !", "Échec de l'ajout de la note.");
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

                runNoteCUDTask(() -> noteDAO.updateNote(selectedNote), "Note modifiée avec succès !", "Échec de la modification de la note.");
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
                runNoteCUDTask(() -> noteDAO.deleteNote(selectedNote.getId()), "Note supprimée avec succès !", "Échec de la suppression de la note.");
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
        loadStudentsTask();
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
        addButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PLUS_CIRCLE));
        updateButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.EDIT));
        deleteButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
        clearButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.ERASER));
        calculateButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.CALCULATOR));
        exportNotesButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.DOWNLOAD));
    }

    /**
     * Met à jour les statistiques affichées dans le pied de page.
     */
    private void updateFooterStatisticsTask() {
        Task<Object[]> task = new Task<>() {
            @Override
            protected Object[] call() throws Exception {
                List<Etudiant> allEtudiants = etudiantDAO.getAllEtudiants();
                List<Note> allNotes = noteDAO.getAllNotes();

                double totalMoyenne = 0.0;
                int validatedCount = 0;
                if (!allNotes.isEmpty()) {
                    for (Note note : allNotes) {
                        totalMoyenne += note.getMoyenne();
                        if (note.getMoyenne() >= 10) {
                            validatedCount++;
                        }
                    }
                }
                return new Object[]{allEtudiants.size(), totalMoyenne, validatedCount, allNotes.size()};
            }
        };

        task.setOnSucceeded(event -> {
            Object[] results = task.getValue();
            totalStudentsLabel.setText(String.valueOf(results[0]));
            double totalMoyenne = (double) results[1];
            int validatedCount = (int) results[2];
            int allNotesSize = (int) results[3];

            if (allNotesSize > 0) {
                globalAverageLabel.setText(df.format(totalMoyenne / allNotesSize));
                successRateLabel.setText(df.format((double) validatedCount / allNotesSize * 100) + "%");
            } else {
                globalAverageLabel.setText("0.00");
                successRateLabel.setText("0%");
            }
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            showStatusMessage("Erreur lors de la mise à jour des statistiques.", "status-error", 5);
        });

        new Thread(task).start();
    }

    @FXML
    private void handleExportNotes() {
        String selectedFiliere = filiereFilterComboBox.getSelectionModel().getSelectedItem();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le fichier CSV des notes");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
        fileChooser.setInitialFileName("notes_" + (selectedFiliere != null && !selectedFiliere.equals("Toutes les filières") ? selectedFiliere : "toutes_filieres") + ".csv");
        File file = fileChooser.showSaveDialog(new Stage());

        if (file != null) {
            Task<Void> exportTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    List<Note> notesToExport;
                    if (selectedFiliere == null || selectedFiliere.equals("Toutes les filières")) {
                        notesToExport = noteDAO.getAllNotes();
                    } else {
                        List<Etudiant> studentsInFiliere = etudiantDAO.getAllEtudiants().stream()
                                .filter(e -> e.getFiliere().equals(selectedFiliere))
                                .collect(Collectors.toList());
                        notesToExport = noteDAO.getAllNotes().stream()
                                .filter(note -> studentsInFiliere.stream().anyMatch(s -> s.getId() == note.getEtudiantId()))
                                .collect(Collectors.toList());
                    }

                    if (notesToExport.isEmpty()) {
                        // This message will not be visible as it's on a background thread.
                        // We should handle this in onSucceeded or onFailed.
                        return null;
                    }

                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                        writer.write("Nom Etudiant,Prenom Etudiant,Filiere,Matiere,Note Devoir,Note Examen,Moyenne,Statut");
                        writer.newLine();

                        for (Note note : notesToExport) {
                            Optional<Etudiant> student = etudiantDAO.getAllEtudiants().stream()
                                    .filter(e -> e.getId() == note.getEtudiantId())
                                    .findFirst();

                            if (student.isPresent()) {
                                writer.write(String.join(",",
                                        student.get().getNom(),
                                        student.get().getPrenom(),
                                        student.get().getFiliere(),
                                        note.getMatiere(),
                                        String.valueOf(note.getNoteDevoir()),
                                        String.valueOf(note.getNoteExamen()),
                                        df.format(note.getMoyenne()),
                                        note.getStatutValidation()));
                                writer.newLine();
                            }
                        }
                    }
                    return null;
                }
            };

            exportTask.setOnSucceeded(event -> {
                showStatusMessage("Exportation des notes terminée avec succès !", "status-success", 3);
                unbindExportControls();
            });

            exportTask.setOnFailed(event -> {
                exportTask.getException().printStackTrace();
                showStatusMessage("Erreur lors de l'exportation du fichier de notes.", "status-error", 5);
                unbindExportControls();
            });

            bindExportControlsToTask(exportTask);
            new Thread(exportTask).start();
        } else {
            showStatusMessage("Exportation des notes annulée.", "status-warning", 3);
        }
    }

    private void bindExportControlsToTask(Task<?> task) {
        mainLoadingSpinner.visibleProperty().bind(task.runningProperty());
        exportNotesButton.disableProperty().bind(task.runningProperty());
    }

    private void unbindExportControls() {
        mainLoadingSpinner.visibleProperty().unbind();
        exportNotesButton.disableProperty().unbind();
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