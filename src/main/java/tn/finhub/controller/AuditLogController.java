package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.beans.property.SimpleStringProperty;
import tn.finhub.model.LedgerAuditLog;
<<<<<<< HEAD
import tn.finhub.service.WalletService;
=======

import tn.finhub.model.WalletModel;
>>>>>>> cd680ce (crud+controle de saisie)
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AuditLogController {

    @FXML
    private TableView<LedgerAuditLog> logTable;
    @FXML
    private TableColumn<LedgerAuditLog, String> dateCol;
    @FXML
    private TableColumn<LedgerAuditLog, String> statusCol;
    @FXML
    private TableColumn<LedgerAuditLog, String> messageCol;

<<<<<<< HEAD
    private WalletService walletService = new WalletService();
=======
    private final WalletModel walletModel = new WalletModel();
>>>>>>> cd680ce (crud+controle de saisie)
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void loadLogs(int walletId) {
<<<<<<< HEAD
        List<LedgerAuditLog> logs = walletService.getAuditLogs(walletId);
=======
        List<LedgerAuditLog> logs = walletModel.getAuditLogs(walletId);
>>>>>>> cd680ce (crud+controle de saisie)

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        dateCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCheckedAt().format(formatter)));

        statusCol.setCellValueFactory(cell -> {
            boolean verified = cell.getValue().isVerified();
            return new SimpleStringProperty(verified ? "PASS" : "FAIL");
        });

        messageCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMessage()));

        logTable.getItems().setAll(logs);

        // Highlight failed rows
        logTable.setRowFactory(tv -> new TableRow<LedgerAuditLog>() {
            @Override
            protected void updateItem(LedgerAuditLog item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (!item.isVerified()) {
                    setStyle("-fx-background-color: #FEE2E2;"); // Light red for fail
                } else {
                    setStyle("");
                }
            }
        });
    }

    @FXML
    private void handleClose() {
        if (stage != null)
            stage.close();
        else if (logTable.getScene().getWindow() instanceof Stage) {
            ((Stage) logTable.getScene().getWindow()).close();
        }
    }
}
