package tn.finhub.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import tn.finhub.model.KYCModel;
import tn.finhub.model.KYCRequest;

public class AdminKYCController {

    @FXML
    private TableView<KYCRequest> requestTable;
    @FXML
    private TableColumn<KYCRequest, Integer> colId;
    @FXML
    private TableColumn<KYCRequest, String> colUser;
    @FXML
    private TableColumn<KYCRequest, String> colType;
    @FXML
    private TableColumn<KYCRequest, String> colDate;
    @FXML
    private TableColumn<KYCRequest, String> colStatus;

    @FXML
    private VBox detailsBox;
    @FXML
    private Label detailUserLabel;
    @FXML
    private Label detailTypeLabel;
    @FXML
    private ImageView detailImageView;
    @FXML
    private Button btnOpenVideo; // To open browser for video if needed
    @FXML
    private Button btnApprove;
    @FXML
    private Button btnReject;

    private KYCModel kycModel = new KYCModel();
    private ObservableList<KYCRequest> requestList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadRequests();

        requestTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            showDetails(newSelection);
        });
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("userEmail"));
        colType.setCellValueFactory(new PropertyValueFactory<>("documentType"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("submissionDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        requestTable.setItems(requestList);
    }

    private void loadRequests() {
        requestList.setAll(kycModel.findPendingRequests());
    }

    private void showDetails(KYCRequest req) {
        if (req == null) {
            detailsBox.setVisible(false);
            return;
        }
        detailsBox.setVisible(true);
        detailUserLabel.setText("User: " + req.getUserEmail());
        detailTypeLabel.setText("Type: " + req.getDocumentType());

        if ("ID_CARD".equals(req.getDocumentType())) {
            detailImageView.setVisible(true);
            btnOpenVideo.setVisible(false);
            try {
                detailImageView.setImage(new Image(req.getDocumentUrl()));
            } catch (Exception e) {
                // handle error
            }
        } else {
            detailImageView.setVisible(false);
            btnOpenVideo.setVisible(true);
            btnOpenVideo.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(req.getDocumentUrl()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
    }

    @FXML
    private void handleApprove() {
        KYCRequest req = requestTable.getSelectionModel().getSelectedItem();
        if (req != null) {
            kycModel.updateStatus(req.getRequestId(), "APPROVED");
            // Update Trust Score? Maybe later.
            loadRequests();
            detailsBox.setVisible(false);
        }
    }

    @FXML
    private void handleReject() {
        KYCRequest req = requestTable.getSelectionModel().getSelectedItem();
        if (req != null) {
            kycModel.updateStatus(req.getRequestId(), "REJECTED");
            loadRequests();
            detailsBox.setVisible(false);
        }
    }
}
