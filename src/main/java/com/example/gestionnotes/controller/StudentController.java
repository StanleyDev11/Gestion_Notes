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
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.util.Optional;
import javafx.scene.control.ButtonType;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

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
    private Label statusMessageLabel;
    
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
    }

    /**
     * Charge la liste des étudiants depuis la base de données et remplit le TableView.
     */
    private void loadStudents() {
        etudiantList = FXCollections.observableArrayList(etudiantDAO.getAllEtudiants());
        studentTableView.setItems(etudiantList);
        // Réinitialiser les boutons après le chargement
        addStudentButton.setDisable(false);
        updateStudentButton.setDisable(true);
        deleteStudentButton.setDisable(true);
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
        String nom = nomTextField.getText();
        String prenom = prenomTextField.getText();
        String filiere = filiereTextField.getText();

        if (nom == null || nom.trim().isEmpty() || prenom == null || prenom.trim().isEmpty() || filiere == null || filiere.trim().isEmpty()) {
            showStatusMessage("Le nom, le prénom et la filière ne peuvent pas être vides.", "status-error", 3);
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
    }

    /**
     * Gère l'action du bouton "Modifier Étudiant".
     * Met à jour l'étudiant sélectionné dans la base de données.
     */
    @FXML
    private void handleUpdateStudent() {
        Etudiant selectedEtudiant = studentTableView.getSelectionModel().getSelectedItem();
        if (selectedEtudiant != null) {
            String nom = nomTextField.getText();
            String prenom = prenomTextField.getText();
            String filiere = filiereTextField.getText();

            if (nom == null || nom.trim().isEmpty() || prenom == null || prenom.trim().isEmpty() || filiere == null || filiere.trim().isEmpty()) {
                showStatusMessage("Le nom, le prénom et la filière ne peuvent pas être vides.", "status-error", 3);
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
    }

    /**
     * Gère l'action du bouton "Supprimer Étudiant".
     * Supprime l'étudiant sélectionné de la base de données.
     */
    @FXML
    private void handleDeleteStudent() {
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