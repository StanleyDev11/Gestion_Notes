package com.example.gestionnotes.controller;

import com.example.gestionnotes.dao.EtudiantDAO;
import com.example.gestionnotes.dao.NoteDAO;
import com.example.gestionnotes.model.Etudiant;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class StudentController {

    @FXML
    private TableView<Etudiant> studentTableView;
    @FXML
    private TableColumn<Etudiant, Integer> studentIdColumn;
    @FXML
    private TableColumn<Etudiant, String> studentNomColumn;
    @FXML
    private TableColumn<Etudiant, String> studentPrenomColumn;
    @FXML
    private TableColumn<Etudiant, String> studentFiliereColumn;

    @FXML
    private TextField nomTextField;
    @FXML
    private TextField prenomTextField;
    @FXML
    private TextField filiereTextField;

    @FXML
    private Button addStudentButton;
    @FXML
    private Button updateStudentButton;
    @FXML
    private Button deleteStudentButton;
    @FXML
    private Button clearFieldsButton;
    @FXML
    private Button importCsvButton;
    @FXML
    private Button exportCsvButton;

    @FXML
    private ProgressIndicator loadingSpinner;

    @FXML
    private Label statusMessageLabel;

    private EtudiantDAO etudiantDAO;
    private NoteDAO noteDAO;
    private MainController mainController;

    private Etudiant selectedEtudiant;

    @FXML
    public void initialize() {
        etudiantDAO = new EtudiantDAO();
        noteDAO = new NoteDAO();

        setupTable();
        setupIcons();
        setupPlaceholders();

        studentTableView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showStudentDetails(newValue));

        loadStudentsTask();
    }

    private void setupTable() {
        studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        studentNomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        studentPrenomColumn.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        studentFiliereColumn.setCellValueFactory(new PropertyValueFactory<>("filiere"));
    }

    private void setupIcons() {
        addStudentButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.USER_PLUS));
        updateStudentButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.EDIT));
        deleteStudentButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.USER_TIMES));
        clearFieldsButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.ERASER));
        importCsvButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.UPLOAD));
        exportCsvButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.DOWNLOAD));
    }

    private void setupPlaceholders() {
        FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.USERS);
        icon.setSize("3em");
        icon.getStyleClass().add("glyph-icon");

        Label placeholderLabel = new Label("Aucun étudiant à afficher.");
        VBox placeholder = new VBox(10, icon, placeholderLabel);
        placeholder.getStyleClass().add("empty-placeholder");
        placeholder.setAlignment(Pos.CENTER);
        studentTableView.setPlaceholder(placeholder);
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    private void loadStudentsTask() {
        Task<List<Etudiant>> task = new Task<>() {
            @Override
            protected List<Etudiant> call() throws Exception {
                return etudiantDAO.getAllEtudiants();
            }
        };

        task.setOnSucceeded(event -> {
            studentTableView.setItems(FXCollections.observableArrayList(task.getValue()));
            unbindControls();
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            showStatusMessage("Erreur: Impossible de charger les étudiants.", "status-error", 5);
            unbindControls();
        });

        bindControlsToTask(task);
        new Thread(task).start();
    }

    private void runCUDTask(Supplier<Boolean> action, String successMessage) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return action.get();
            }
        };

        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                showStatusMessage(successMessage, "status-success", 3);
                loadStudentsTask();
                if (mainController != null) {
                    mainController.refreshStudentView();
                }
                clearFields();
            } else {
                showStatusMessage("Erreur: L'opération a échoué.", "status-error", 3);
            }
            unbindControls();
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            showStatusMessage("Erreur: L'opération a échoué.", "status-error", 5);
            unbindControls();
        });

        bindControlsToTask(task);
        new Thread(task).start();
    }

    private void bindControlsToTask(Task<?> task) {
        loadingSpinner.visibleProperty().bind(task.runningProperty());
        studentTableView.disableProperty().bind(task.runningProperty());
        nomTextField.disableProperty().bind(task.runningProperty());
        prenomTextField.disableProperty().bind(task.runningProperty());
        filiereTextField.disableProperty().bind(task.runningProperty());
        addStudentButton.disableProperty().bind(task.runningProperty());
        updateStudentButton.disableProperty().bind(task.runningProperty());
        deleteStudentButton.disableProperty().bind(task.runningProperty());
        importCsvButton.disableProperty().bind(task.runningProperty());
        exportCsvButton.disableProperty().bind(task.runningProperty());
    }

    private void unbindControls() {
        loadingSpinner.visibleProperty().unbind();
        studentTableView.disableProperty().unbind();
        nomTextField.disableProperty().unbind();
        prenomTextField.disableProperty().unbind();
        filiereTextField.disableProperty().unbind();
        addStudentButton.disableProperty().unbind();
        updateStudentButton.disableProperty().unbind();
        deleteStudentButton.disableProperty().unbind();
        importCsvButton.disableProperty().unbind();
        exportCsvButton.disableProperty().unbind();
    }

    @FXML
    private void handleAddStudent() {
        if (isInputValid()) {
            Etudiant newEtudiant = new Etudiant(0, nomTextField.getText(), prenomTextField.getText(), filiereTextField.getText());
            runCUDTask(() -> etudiantDAO.addEtudiant(newEtudiant), "Etudiant ajouté avec succès.");
        }
    }

    @FXML
    private void handleUpdateStudent() {
        if (selectedEtudiant != null && isInputValid()) {
            selectedEtudiant.setNom(nomTextField.getText());
            selectedEtudiant.setPrenom(prenomTextField.getText());
            selectedEtudiant.setFiliere(filiereTextField.getText());
            runCUDTask(() -> etudiantDAO.updateEtudiant(selectedEtudiant), "Etudiant mis à jour avec succès.");
        }
    }

    @FXML
    private void handleDeleteStudent() {
        if (selectedEtudiant != null) {
            if (noteDAO.getNoteCountForStudent(selectedEtudiant.getId()) > 0) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur de suppression");
                alert.setHeaderText("Impossible de supprimer l'étudiant");
                alert.setContentText("Cet étudiant a des notes associées. Veuillez d'abord supprimer ses notes.");
                alert.showAndWait();
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation de suppression");
            alert.setHeaderText("Supprimer l'étudiant ?");
            alert.setContentText("Êtes-vous sûr de vouloir supprimer " + selectedEtudiant.getPrenom() + " " + selectedEtudiant.getNom() + " ?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                runCUDTask(() -> etudiantDAO.deleteEtudiant(selectedEtudiant.getId()), "Etudiant supprimé avec succès.");
            }
        }
    }

    @FXML
    private void handleClearFields() {
        clearFields();
    }

    private void clearFields() {
        studentTableView.getSelectionModel().clearSelection();
        nomTextField.clear();
        prenomTextField.clear();
        filiereTextField.clear();
        selectedEtudiant = null;
        updateStudentButton.setDisable(true);
        deleteStudentButton.setDisable(true);
        addStudentButton.setDisable(false);
    }

    private void showStudentDetails(Etudiant etudiant) {
        this.selectedEtudiant = etudiant;
        if (etudiant != null) {
            nomTextField.setText(etudiant.getNom());
            prenomTextField.setText(etudiant.getPrenom());
            filiereTextField.setText(etudiant.getFiliere());
            updateStudentButton.setDisable(false);
            deleteStudentButton.setDisable(false);
            addStudentButton.setDisable(true);
        } else {
            clearFields();
        }
    }

    private boolean isInputValid() {
        // Simple validation, can be improved
        return !nomTextField.getText().isEmpty() && !prenomTextField.getText().isEmpty() && !filiereTextField.getText().isEmpty();
    }

    @FXML
    private void handleImportCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importer un fichier CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
        File file = fileChooser.showOpenDialog(new Stage());

        if (file != null) {
            Task<Integer> importTask = new Task<>() {
                @Override
                protected Integer call() throws Exception {
                    List<Etudiant> etudiants = new ArrayList<>();
                    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        String line;
                        br.readLine(); // Skip header
                        while ((line = br.readLine()) != null) {
                            String[] values = line.split(",");
                            if (values.length >= 3) {
                                etudiants.add(new Etudiant(0, values[0].trim(), values[1].trim(), values[2].trim()));
                            }
                        }
                    }
                    if (!etudiants.isEmpty()) {
                        etudiantDAO.addEtudiants(etudiants);
                    }
                    return etudiants.size();
                }
            };

            importTask.setOnSucceeded(event -> {
                int count = importTask.getValue();
                showStatusMessage(count + " étudiant(s) importé(s) avec succès.", "status-success", 3);
                loadStudentsTask();
                if (mainController != null) {
                    mainController.refreshStudentView();
                }
                unbindControls();
            });

            importTask.setOnFailed(event -> {
                importTask.getException().printStackTrace();
                showStatusMessage("Erreur lors de l'importation du CSV.", "status-error", 5);
                unbindControls();
            });

            bindControlsToTask(importTask);
            new Thread(importTask).start();
        }
    }

    @FXML
    private void handleExportCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter vers un fichier CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
        File file = fileChooser.showSaveDialog(new Stage());

        if (file != null) {
            Task<Void> exportTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    List<Etudiant> etudiants = etudiantDAO.getAllEtudiants();
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                        writer.write("Nom,Prenom,Filiere");
                        writer.newLine();
                        for (Etudiant etudiant : etudiants) {
                            writer.write(String.join(",", etudiant.getNom(), etudiant.getPrenom(), etudiant.getFiliere()));
                            writer.newLine();
                        }
                    }
                    return null;
                }
            };

            exportTask.setOnSucceeded(event -> {
                showStatusMessage("Exportation CSV réussie.", "status-success", 3);
                unbindControls();
            });

            exportTask.setOnFailed(event -> {
                exportTask.getException().printStackTrace();
                showStatusMessage("Erreur lors de l'exportation CSV.", "status-error", 5);
                unbindControls();
            });

            bindControlsToTask(exportTask);
            new Thread(exportTask).start();
        }
    }

    private void showStatusMessage(String message, String styleClass, int durationSeconds) {
        statusMessageLabel.setText(message);
        statusMessageLabel.getStyleClass().clear();
        statusMessageLabel.getStyleClass().add("status-message");
        statusMessageLabel.getStyleClass().add(styleClass);

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(durationSeconds), event -> {
            statusMessageLabel.setText("");
            statusMessageLabel.getStyleClass().clear();
            statusMessageLabel.getStyleClass().add("status-message");
        }));
        timeline.play();
    }
}
