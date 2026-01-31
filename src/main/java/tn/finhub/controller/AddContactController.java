package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.finhub.dao.SavedContactDAO;
import tn.finhub.dao.UserDAO;
import tn.finhub.dao.impl.UserDAOImpl;
import tn.finhub.model.SavedContact;
import tn.finhub.model.User;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.UserSession;

public class AddContactController {

    @FXML
    private TextField emailField;

    private final SavedContactDAO contactDAO = new SavedContactDAO();
    private final UserDAO userDAO = new UserDAOImpl();
    private Runnable onContactAdded;

    public void setOnContactAdded(Runnable onContactAdded) {
        this.onContactAdded = onContactAdded;
    }

    @FXML
    private void handleSave() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            DialogUtil.showError("Validation Error", "Please enter an email address.");
            return;
        }

        // Validate email format
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            DialogUtil.showError("Validation Error", "Invalid email format.");
            return;
        }

        int userId = UserSession.getInstance().getUser().getId();

        // Check if self
        if (email.equalsIgnoreCase(UserSession.getInstance().getUser().getEmail())) {
            DialogUtil.showError("Error", "You cannot save yourself as a contact.");
            return;
        }

        if (contactDAO.exists(userId, email)) {
            DialogUtil.showError("Error", "Contact with this email already exists.");
            return;
        }

        // Fetch user name
        User user = userDAO.findByEmail(email);

        if (user == null) {
            try {
                if (emailField.getScene() != null) {
                    emailField.getScene().setCursor(javafx.scene.Cursor.WAIT);
                }
                tn.finhub.service.UserService userService = new tn.finhub.service.UserService();
                userService.syncUsersFromServer(); // Fetches all users and updates local DB
                user = userDAO.findByEmail(email); // Retry lookup
            } catch (Exception e) {
                e.printStackTrace();
                DialogUtil.showError("Sync Error", "Could not verify user with server: " + e.getMessage());
                // We return here because if sync fails, we can't be sure about existence.
                // However, user might persist if they think it's local.
                // But prompted behavior says "only if found".
                return;
            } finally {
                if (emailField.getScene() != null) {
                    emailField.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
                }
            }
        }

        if (user == null) {
            DialogUtil.showError("Error", "No user found with this email in FinHub.");
            return;
        }

        // Check if user has a wallet
        tn.finhub.service.WalletService walletService = new tn.finhub.service.WalletService();
        if (!walletService.hasWallet(user.getId())) {
            DialogUtil.showError("Error", "User exists but has no active wallet.");
            return;
        }

        SavedContact contact = new SavedContact(userId, email, user.getFullName());
        try {
            contactDAO.addContact(contact);
            if (onContactAdded != null) {
                onContactAdded.run();
            }
            handleCancel();
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.showError("Error", "Could not save contact.");
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
    }
}
