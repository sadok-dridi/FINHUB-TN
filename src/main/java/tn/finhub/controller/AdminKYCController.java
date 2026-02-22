package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.finhub.model.KYCModel;
import tn.finhub.model.KYCRequest;
import tn.finhub.model.User;
import tn.finhub.model.UserModel;

public class AdminKYCController {

    @FXML
    private FlowPane requestsContainer;
    @FXML
    private VBox requestsListContainer;

    @FXML
    private VBox detailsView;
    @FXML
    private Label detailUserLabel; // Now Full Name
    @FXML
    private Label userEmailLabel;
    @FXML
    private Label userRoleLabel;
    @FXML
    private Label userPhoneLabel;
    @FXML
    private Label userTrustLabel;
    @FXML
    private ImageView userProfileImage;

    @FXML
    private Label detailTypeLabel;
    @FXML
    private Label detailDateLabel;
    @FXML
    private ImageView detailImageView;
    @FXML
    private Button btnOpenVideo;

    @FXML
    private VBox actionsContainer; // Changed from HBox to VBox in FXML
    @FXML
    private Button btnApprove;
    @FXML
    private Button btnReject;

    private KYCModel kycModel = new KYCModel();
    private UserModel userModel = new UserModel();
    private KYCRequest currentRequest;

    private static java.util.List<KYCRequest> cachedRequests;

    public static void setCachedRequests(java.util.List<KYCRequest> requests) {
        cachedRequests = requests;
    }

    @FXML
    public void initialize() {
        loadRequests();
    }

    @FXML
    private void handleRefresh() {
        loadRequests();
    }

    private void loadRequests() {
        requestsContainer.getChildren().clear();
        java.util.List<KYCRequest> requests;

        if (cachedRequests != null) {
            requests = cachedRequests;
            cachedRequests = null;
        } else {
            requests = kycModel.findAllRequests();
        }

        for (KYCRequest req : requests) {
            requestsContainer.getChildren().add(createRequestCard(req));
        }
    }

    private Node createRequestCard(KYCRequest req) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: -color-card-bg; -fx-background-radius: 12; -fx-padding: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0); -fx-cursor: hand;");
        card.setPrefWidth(280);
        card.setMinWidth(280);
        card.setAlignment(Pos.CENTER_LEFT);

        // Hover Effect
        card.setOnMouseEntered(e -> card
                .setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 12; -fx-padding: 15; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(139, 92, 246, 0.4), 8, 0, 0, 0); -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card
                .setStyle("-fx-background-color: -color-card-bg; -fx-background-radius: 12; -fx-padding: 15; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0); -fx-cursor: hand;"));

        card.setOnMouseClicked(e -> showDetails(req));

        // Header: Type + Status
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(req.getDocumentType());
        typeBadge.setStyle("-fx-background-color: -color-primary-transparent; -fx-text-fill: -color-primary; " +
                "-fx-padding: 4 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 10px;");

        Label statusBadge = new Label(req.getStatus());
        String statusStyle = switch (req.getStatus()) {
            case "APPROVED" -> "-fx-background-color: -color-success-transparent; -fx-text-fill: -color-success;";
            case "REJECTED" -> "-fx-background-color: -color-error-transparent; -fx-text-fill: -color-error;";
            default -> "-fx-background-color: -color-warning-transparent; -fx-text-fill: -color-warning;";
        };
        statusBadge.setStyle(statusStyle
                + " -fx-padding: 4 8; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 10px;");

        header.getChildren().addAll(typeBadge, statusBadge);

        // User Info
        Label userLabel = new Label(req.getUserEmail());
        userLabel.setStyle("-fx-text-fill: -color-text-primary; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label dateLabel = new Label("Submitted: "
                + (req.getSubmissionDate() != null ? req.getSubmissionDate().toString().substring(0, 16) : "N/A"));
        dateLabel.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-size: 12px;");

        card.getChildren().addAll(header, userLabel, dateLabel);
        return card;
    }

    private void showDetails(KYCRequest req) {
        this.currentRequest = req;
        detailsView.setVisible(true);
        requestsListContainer.setVisible(false);

        // Fetch Full User Details
        User user = userModel.findById(req.getUserId());

        // Populate User Info Side
        if (user != null) {
            detailUserLabel.setText(user.getFullName() != null ? user.getFullName() : "Unknown User");
            userEmailLabel.setText(user.getEmail());
            userRoleLabel.setText(user.getRole());
            userPhoneLabel.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "No Phone");
            userTrustLabel.setText(String.valueOf(user.getTrustScore()));

            if (user.getProfilePhotoUrl() != null && !user.getProfilePhotoUrl().isEmpty()) {
                try {
                    userProfileImage.setImage(new Image(user.getProfilePhotoUrl(), true));
                } catch (Exception e) {
                    // Keep default placeholder if fails
                }
            } else {
                userProfileImage.setImage(null); // Will show background circle
            }
        } else {
            detailUserLabel.setText("User Not Found");
            userEmailLabel.setText(req.getUserEmail());
        }

        // Populate Request Info
        detailTypeLabel.setText(req.getDocumentType());
        detailDateLabel.setText(
                "Submitted: " + (req.getSubmissionDate() != null ? req.getSubmissionDate().toString() : "N/A"));

        // Manage visibility of action buttons
        if (actionsContainer != null) {
            boolean isPending = "PENDING".equalsIgnoreCase(req.getStatus());
            actionsContainer.setVisible(isPending);
            actionsContainer.setManaged(isPending);
        }

        // Handle Document/Video
        if ("ID_CARD".equals(req.getDocumentType())) {
            detailImageView.setVisible(true);
            detailImageView.setManaged(true);
            btnOpenVideo.setVisible(false);
            btnOpenVideo.setManaged(false);
            try {
                detailImageView.setImage(new Image(req.getDocumentUrl(), true));
            } catch (Exception e) {
            }
        } else {
            detailImageView.setVisible(false);
            detailImageView.setManaged(false);
            btnOpenVideo.setVisible(true);
            btnOpenVideo.setManaged(true);
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
    private void handleBack() {
        detailsView.setVisible(false);
        requestsListContainer.setVisible(true);
        currentRequest = null;
    }

    @FXML
    private void handleApprove() {
        if (currentRequest != null) {
            kycModel.updateStatus(currentRequest.getRequestId(), "APPROVED");
            handleBack();
            loadRequests();
        }
    }

    @FXML
    private void handleReject() {
        if (currentRequest != null) {
            kycModel.updateStatus(currentRequest.getRequestId(), "REJECTED");
            handleBack();
            loadRequests();
        }
    }
}
