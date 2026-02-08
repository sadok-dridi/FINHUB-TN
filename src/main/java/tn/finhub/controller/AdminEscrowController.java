package tn.finhub.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import tn.finhub.model.Escrow;
import tn.finhub.model.EscrowManager;
import tn.finhub.model.User;
import tn.finhub.model.UserModel;
import tn.finhub.model.Wallet;
import tn.finhub.model.WalletModel;
import tn.finhub.util.UserSession;
import tn.finhub.util.ViewUtils;

import java.math.BigDecimal;
import java.util.Optional;

public class AdminEscrowController {

    @FXML
    private TableView<Escrow> escrowTable;
    @FXML
    private TableColumn<Escrow, Integer> idColumn;
    @FXML
    private TableColumn<Escrow, String> senderColumn;
    @FXML
    private TableColumn<Escrow, String> receiverColumn;
    @FXML
    private TableColumn<Escrow, BigDecimal> amountColumn;
    @FXML
    private TableColumn<Escrow, String> typeColumn;
    @FXML
    private TableColumn<Escrow, String> statusColumn;
    @FXML
    private TableColumn<Escrow, String> conditionColumn;
    @FXML
    private TableColumn<Escrow, Void> actionsColumn;

    @FXML
    private Label statusLabel; // For feedback messages

    private final EscrowManager escrowManager = new EscrowManager();
    private final WalletModel walletModel = new WalletModel();
    private final UserModel userModel = new UserModel();
    private ObservableList<Escrow> escrowList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadData();
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("escrowType"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        conditionColumn.setCellValueFactory(new PropertyValueFactory<>("conditionText"));

        senderColumn.setCellValueFactory(cellData -> {
            int walletId = cellData.getValue().getSenderWalletId();
            return new SimpleStringProperty(getUserNameByWalletId(walletId));
        });

        receiverColumn.setCellValueFactory(cellData -> {
            int walletId = cellData.getValue().getReceiverWalletId();
            return new SimpleStringProperty(getUserNameByWalletId(walletId));
        });

        // Custom Actions Column
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button btnRelease = new Button("Release");
            private final Button btnRefund = new Button("Refund");
            private final HBox pane = new HBox(5, btnRelease, btnRefund);

            {
                btnRelease.getStyleClass().add("button-success-small");
                btnRefund.getStyleClass().add("button-danger-small");

                // Styles if class not waiting
                btnRelease.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 10px;");
                btnRefund.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 10px;");

                btnRelease.setOnAction(event -> handleRelease(getTableView().getItems().get(getIndex())));
                btnRefund.setOnAction(event -> handleRefund(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Escrow e = getTableView().getItems().get(getIndex());
                    boolean isActive = "LOCKED".equals(e.getStatus()) || "DISPUTED".equals(e.getStatus());
                    btnRelease.setDisable(!isActive);
                    btnRefund.setDisable(!isActive);
                    setGraphic(pane);
                }
            }
        });
    }

    private String getUserNameByWalletId(int walletId) {
        Wallet w = walletModel.findById(walletId);
        if (w != null) {
            User u = userModel.findById(w.getUserId());
            if (u != null)
                return u.getFullName();
        }
        return "Unknown (" + walletId + ")";
    }

    private void loadData() {
        escrowList.setAll(escrowManager.getEscrowsForAdmin());
        escrowTable.setItems(escrowList);
    }

    private void handleRelease(Escrow e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Release");
        alert.setHeaderText("Release Funds for Escrow #" + e.getId());
        alert.setContentText("This will transfer " + e.getAmount() + " TND to the Receiver. Are you sure?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int adminId = UserSession.getInstance().getUser().getId(); // Current Admin
                escrowManager.releaseEscrowByAdmin(e.getId(), adminId);
                showInfo("Success", "Funds released successfully.");
                loadData();
            } catch (Exception ex) {
                showError("Error releasing funds: " + ex.getMessage());
            }
        }
    }

    private void handleRefund(Escrow e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Refund");
        alert.setHeaderText("Refund Escrow #" + e.getId());
        alert.setContentText("This will return " + e.getAmount() + " TND to the Sender. Are you sure?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                escrowManager.refundEscrow(e.getId());
                showInfo("Success", "Funds refunded successfully.");
                loadData();
            } catch (Exception ex) {
                showError("Error refunding funds: " + ex.getMessage());
            }
        }
    }

    // Navigation Handlers (Copy from AdminDashboard or use Base Controller if
    // exists)
    @FXML
    private void handleBack() {
        // Assuming we navigate back to main dashboard
        ViewUtils.setView(escrowTable, "/view/admin_dashboard.fxml");
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
