package com.example.gestionnotes.controller;

import com.example.gestionnotes.dao.EtudiantDAO;
import com.example.gestionnotes.model.Etudiant;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
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
    private MainController mainController;

    private Etudiant selectedEtudiant;

    @FXML
    public void initialize() {
        etudiantDAO = new EtudiantDAO();

        studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        studentNomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        studentPrenomColumn.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        studentFiliereColumn.setCellValueFactory(new PropertyValueFactory<>("filiere"));

        studentTableView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showStudentDetails(newValue));

        loadStudentsTask();
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
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            // Show error message
        });

        loadingSpinner.visibleProperty().bind(task.runningProperty());
        studentTableView.disableProperty().bind(task.runningProperty());

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
                // show success message
                loadStudentsTask(); // Refresh this table
                if (mainController != null) {
                    mainController.refreshStudentView(); // Refresh main view
                }
                clearFields();
            } else {
                // show failure message
            }
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            // show error message
        });

        loadingSpinner.visibleProperty().bind(task.runningProperty());
        studentTableView.disableProperty().bind(task.runningProperty());
        nomTextField.disableProperty().bind(task.runningProperty());
        prenomTextField.disableProperty().bind(task.runningProperty());
        filiereTextField.disableProperty().bind(task.runningProperty());
        addStudentButton.disableProperty().bind(task.runningProperty());
        updateStudentButton.disableProperty().bind(task.runningProperty());
        deleteStudentButton.disableProperty().bind(task.runningProperty());

        new Thread(task).start();
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
        // Implementation for CSV import
    }

    @FXML
    private void handleExportCsv() {
        // Implementation for CSV export
    }
}
