package com.example.gestionnotes.controller;

import com.example.gestionnotes.dao.EtudiantDAO;
import com.example.gestionnotes.model.Etudiant;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.util.Optional;
import javafx.scene.control.ButtonType;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Contrôleur pour la fenêtre de gestion des étudiants (ajout, affichage, modification, suppression).
 */
public class StudentController {

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
    private Label statusMessageLabel;
    @FXML
    private ProgressIndicator loadingSpinner;
    
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

    private EtudiantDAO etudiantDAO;
    private ObservableList<Etudiant> etudiantList;
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * Initialise le contrôleur. Appelé après le chargement du FXML.
     */
    @FXML
    public void initialize() {
        etudiantDAO = new EtudiantDAO();

        // Initialisation des colonnes du TableView
        studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        studentNomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        studentPrenomColumn.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        studentFiliereColumn.setCellValueFactory(new PropertyValueFactory<>("filiere"));

        // Charger les étudiants dans le TableView
        loadStudents();

        // Écouteur de sélection sur le TableView
        studentTableView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showStudentDetails(newValue));

        // Setup icons
        setupIcons();
    }

    private void setupIcons() {
        addStudentButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PLUS));
        updateStudentButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PENCIL));
        deleteStudentButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
        clearFieldsButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TIMES));
        importCsvButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.UPLOAD));
        exportCsvButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.DOWNLOAD));
    }

    /**
     * Charge la liste des étudiants depuis la base de données et remplit le TableView.
     */
    private void loadStudents() {
        showSpinner();
        etudiantList = FXCollections.observableArrayList(etudiantDAO.getAllEtudiants());
        studentTableView.setItems(etudiantList);
        // Réinitialiser les boutons après le chargement
        addStudentButton.setDisable(false);
        updateStudentButton.setDisable(true);
        deleteStudentButton.setDisable(true);
        hideSpinner();
    }

    /**
     * Affiche les détails de l'étudiant sélectionné dans les champs de saisie.
     * @param etudiant L'étudiant sélectionné dans le TableView.
     */
    private void showStudentDetails(Etudiant etudiant) {
        if (etudiant != null) {
            nomTextField.setText(etudiant.getNom());
            prenomTextField.setText(etudiant.getPrenom());
            filiereTextField.setText(etudiant.getFiliere());
            addStudentButton.setDisable(true); // Désactive le bouton Ajouter en mode modification/suppression
            updateStudentButton.setDisable(false);
            deleteStudentButton.setDisable(false);
        } else {
            clearFields();
            addStudentButton.setDisable(false);
            updateStudentButton.setDisable(true);
            deleteStudentButton.setDisable(true);
        }
    }

    /**
     * Gère l'action du bouton "Ajouter Étudiant".
     * Ajoute un nouvel étudiant à la base de données.
     */
    @FXML
    private void handleAddStudent() {
        showSpinner();
        // Clear previous invalid states
        setFieldInvalid(nomTextField, false);
        setFieldInvalid(prenomTextField, false);
        setFieldInvalid(filiereTextField, false);

        String nom = nomTextField.getText();
        String prenom = prenomTextField.getText();
        String filiere = filiereTextField.getText();

        boolean isValid = true;
        if (nom == null || nom.trim().isEmpty()) {
            setFieldInvalid(nomTextField, true);
            isValid = false;
        }
        if (prenom == null || prenom.trim().isEmpty()) {
            setFieldInvalid(prenomTextField, true);
            isValid = false;
        }
        if (filiere == null || filiere.trim().isEmpty()) {
            setFieldInvalid(filiereTextField, true);
            isValid = false;
        }

        if (!isValid) {
            showStatusMessage("Le nom, le prénom et la filière ne peuvent pas être vides.", "status-error", 3);
            hideSpinner(); // Hide spinner on validation error
            return;
        }

        Etudiant newEtudiant = new Etudiant(0, nom, prenom, filiere); // ID sera généré par la BD

        if (etudiantDAO.addEtudiant(newEtudiant)) {
            showStatusMessage("Étudiant ajouté avec succès !", "status-success", 3);
            clearFields();
            loadStudents(); // Rafraîchir le TableView après l'ajout
            if (mainController != null) {
                mainController.refreshStudentView();
            }
        } else {
            showStatusMessage("Échec de l'ajout de l'étudiant.", "status-error", 3);
        }
        hideSpinner();
    }

    /**
     * Gère l'action du bouton "Modifier Étudiant".
     * Met à jour l'étudiant sélectionné dans la base de données.
     */
    @FXML
    private void handleUpdateStudent() {
        showSpinner();
        // Clear previous invalid states
        setFieldInvalid(nomTextField, false);
        setFieldInvalid(prenomTextField, false);
        setFieldInvalid(filiereTextField, false);

        Etudiant selectedEtudiant = studentTableView.getSelectionModel().getSelectedItem();
        if (selectedEtudiant != null) {
            String nom = nomTextField.getText();
            String prenom = prenomTextField.getText();
            String filiere = filiereTextField.getText();

            boolean isValid = true;
            if (nom == null || nom.trim().isEmpty()) {
                setFieldInvalid(nomTextField, true);
                isValid = false;
            }
            if (prenom == null || prenom.trim().isEmpty()) {
                setFieldInvalid(prenomTextField, true);
                isValid = false;
            }
            if (filiere == null || filiere.trim().isEmpty()) {
                setFieldInvalid(filiereTextField, true);
                isValid = false;
            }

            if (!isValid) {
                showStatusMessage("Le nom, le prénom et la filière ne peuvent pas être vides.", "status-error", 3);
                hideSpinner(); // Hide spinner on validation error
                return;
            }

            selectedEtudiant.setNom(nom);
            selectedEtudiant.setPrenom(prenom);
            selectedEtudiant.setFiliere(filiere);

            if (etudiantDAO.updateEtudiant(selectedEtudiant)) {
                showStatusMessage("Étudiant modifié avec succès !", "status-success", 3);
                clearFields();
                loadStudents();
                if (mainController != null) {
                    mainController.refreshStudentView();
                }
            } else {
                showStatusMessage("Échec de la modification de l'étudiant.", "status-error", 3);
            }
        } else {
            showStatusMessage("Veuillez sélectionner un étudiant à modifier dans le tableau.", "status-warning", 3);
        }
        hideSpinner();
    }

    /**
     * Gère l'action du bouton "Supprimer Étudiant".
     * Supprime l'étudiant sélectionné de la base de données.
     */
    @FXML
    private void handleDeleteStudent() {
        showSpinner();
        Etudiant selectedEtudiant = studentTableView.getSelectionModel().getSelectedItem();
        if (selectedEtudiant != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation de suppression");
            alert.setHeaderText("Supprimer l'étudiant ?");
            alert.setContentText("Êtes-vous sûr de vouloir supprimer " + selectedEtudiant.getPrenom() + " " + selectedEtudiant.getNom() + " ?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (etudiantDAO.deleteEtudiant(selectedEtudiant.getId())) {
                    showStatusMessage("Étudiant supprimé avec succès !", "status-success", 3);
                    clearFields();
                    loadStudents();
                    if (mainController != null) {
                        mainController.refreshStudentView();
                    }
                } else {
                    showStatusMessage("Échec de la suppression de l'étudiant.", "status-error", 3);
                }
            }
        } else {
            showStatusMessage("Veuillez sélectionner un étudiant à supprimer dans le tableau.", "status-warning", 3);
        }
        hideSpinner();
    }

    /**
     * Gère l'action du bouton "Effacer les champs".
     * Réinitialise les champs de saisie et la sélection du tableau.
     */
    @FXML
    private void handleClearFields() {
        clearFields();
        studentTableView.getSelectionModel().clearSelection();
    }

    /**
     * Réinitialise tous les champs de saisie.
     */
    private void clearFields() {
        nomTextField.setText("");
        prenomTextField.setText("");
        filiereTextField.setText("");
        addStudentButton.setDisable(false);
        updateStudentButton.setDisable(true);
        deleteStudentButton.setDisable(true);
        // Clear invalid states
        setFieldInvalid(nomTextField, false);
        setFieldInvalid(prenomTextField, false);
        setFieldInvalid(filiereTextField, false);
    }

    private void showSpinner() {
        if (loadingSpinner != null) {
            loadingSpinner.setVisible(true);
            // Disable UI elements
            nomTextField.setDisable(true);
            prenomTextField.setDisable(true);
            filiereTextField.setDisable(true);
            addStudentButton.setDisable(true);
            updateStudentButton.setDisable(true);
            deleteStudentButton.setDisable(true);
            clearFieldsButton.setDisable(true);
            importCsvButton.setDisable(true);
            studentTableView.setDisable(true);
        }
    }

    private void hideSpinner() {
        if (loadingSpinner != null) {
            loadingSpinner.setVisible(false);
            // Re-enable UI elements
            nomTextField.setDisable(false);
            prenomTextField.setDisable(false);
            filiereTextField.setDisable(false);
            // Buttons will be re-enabled by clearFields() or showStudentDetails()
            // based on selection, so we only re-enable the ones that are always active
            clearFieldsButton.setDisable(false);
            importCsvButton.setDisable(false);
            studentTableView.setDisable(false);

            // Re-enable add/update/delete based on current state
            Etudiant selectedEtudiant = studentTableView.getSelectionModel().getSelectedItem();
            if (selectedEtudiant != null) {
                addStudentButton.setDisable(true);
                updateStudentButton.setDisable(false);
                deleteStudentButton.setDisable(false);
            }
            else {
                addStudentButton.setDisable(false);
                updateStudentButton.setDisable(true);
                deleteStudentButton.setDisable(true);
            }
        }
    }

    private void setFieldInvalid(TextField field, boolean invalid) {
        if (invalid) {
            field.getStyleClass().add("invalid-field");
        }
        else {
            field.getStyleClass().remove("invalid-field");
        }
    }

    /**
     * Gère l'action du bouton "Importer CSV".
     * Permet d'importer des étudiants depuis un fichier CSV.
     */
    @FXML
    private void handleImportCsv() {
        showSpinner();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(new Stage()); // Use a new Stage for the dialog

        if (selectedFile != null) {
            int importedCount = 0;
            int failedCount = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length == 3) { // Expecting nom,prenom,filiere
                        String nom = parts[0].trim();
                        String prenom = parts[1].trim();
                        String filiere = parts[2].trim();

                        // Basic validation
                        if (nom.isEmpty() || prenom.isEmpty() || filiere.isEmpty()) {
                            failedCount++;
                            continue;
                        }

                        Etudiant newEtudiant = new Etudiant(0, nom, prenom, filiere);
                        if (etudiantDAO.addEtudiant(newEtudiant)) {
                            importedCount++;
                        }
                        else {
                            failedCount++;
                        }
                    }
                    else {
                        failedCount++; // Malformed line
                    }
                }
                showStatusMessage("Importation terminée. " + importedCount + " étudiants importés, " + failedCount + " échecs.", "status-info", 5);
                loadStudents(); // Refresh the TableView
                if (mainController != null) {
                    mainController.refreshStudentView();
                }
            }
            catch (IOException e) {
                showStatusMessage("Erreur de lecture du fichier : " + e.getMessage(), "status-error", 5);
                e.printStackTrace();
            }
            catch (Exception e) { // Catch any other unexpected errors during parsing/DB ops
                showStatusMessage("Une erreur inattendue est survenue lors de l'importation : " + e.getMessage(), "status-error", 5);
                e.printStackTrace();
            }
        }
        else {
            showStatusMessage("Aucun fichier sélectionné.", "status-warning", 3);
        }
        hideSpinner();
    }

    @FXML
    private void handleExportCsv() {
        showSpinner();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le fichier CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
        fileChooser.setInitialFileName("etudiants.csv");
        File file = fileChooser.showSaveDialog(new Stage());

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                // Write CSV header
                writer.write("Nom,Prenom,Filiere");
                writer.newLine();

                // Write student data
                for (Etudiant etudiant : etudiantList) {
                    writer.write(String.join(",",
                            etudiant.getNom(),
                            etudiant.getPrenom(),
                            etudiant.getFiliere()));
                    writer.newLine();
                }
                showStatusMessage("Exportation terminée avec succès !", "status-success", 3);
            } catch (IOException e) {
                showStatusMessage("Erreur lors de l'exportation du fichier : " + e.getMessage(), "status-error", 5);
                e.printStackTrace();
            }
        } else {
            showStatusMessage("Exportation annulée : aucun fichier sélectionné.", "status-warning", 3);
        }
        hideSpinner();
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