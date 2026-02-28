package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.Node;
import java.io.IOException;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import tn.finhub.model.Expense;
import tn.finhub.model.ExpenseModel;
import tn.finhub.model.ExpenseItem;
import tn.finhub.util.UserSession;
import tn.finhub.util.DialogUtil;

public class ExpenseTrackerController {

    @FXML
    private StackPane rootStackPane;

    // Linked View Components (to be injected when view is loaded)
    @FXML
    private Label totalSpentLabel;
    @FXML
    private VBox expensesContainer;

    private final ExpenseModel expenseModel = new ExpenseModel();

    private boolean isInitializing = false;
    private Timeline pollingTimeline;

    @FXML
    public void initialize() {
        if (isInitializing) {
            return;
        }
        isInitializing = true;
        try {
            checkTelegramLinkage();
        } finally {
            isInitializing = false;
        }
    }

    private void checkTelegramLinkage() {
        int userId = UserSession.getInstance().getUser().getId();
        boolean isLinked = expenseModel.isTelegramLinked(userId);

        rootStackPane.getChildren().clear();

        if (isLinked) {
            System.out.println("User linked! Showing linked view.");
            showLinkedView(userId);
        } else {
            System.out.println("User not linked! Showing unlinked view.");
            showUnlinkedView();
        }
    }

    private void showUnlinkedView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/financial_twin_expenses_unlinked.fxml"));
            loader.setController(this); // specific controller or self?
            // Better to keep separate small FXMLs. Let's assume unlinked view is static.
            Node view = loader.load();
            rootStackPane.getChildren().add(view);
        } catch (IOException e) {
            e.printStackTrace();
            DialogUtil.showError("Error", "Failed to load expenses view.");
        }
    }

    // Action for the 'I have linked my account' button in unlinked view
    @FXML
    private void handleRefreshLinkStatus() {
        checkTelegramLinkage();
    }

    private void showLinkedView(int userId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/financial_twin_expenses_linked.fxml"));
            loader.setController(this);
            Node view = loader.load();

            // Re-bind controls after loading the view
            // No need for lookup if @FXML injection worked via loader.load() with this
            // controller
            // totalSpentLabel = (Label) view.lookup("#totalSpentLabel");
            // expensesContainer = (FlowPane) view.lookup("#expensesContainer");

            rootStackPane.getChildren().add(view);

            loadExpenses(userId);
            startPolling(userId);

        } catch (IOException e) {
            e.printStackTrace();
            DialogUtil.showError("Error", "Failed to load expenses view.");
        }
    }

    private void startPolling(int userId) {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
        }
        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            loadExpenses(userId);
        }));
        pollingTimeline.setCycleCount(Animation.INDEFINITE);
        pollingTimeline.play();
    }

    private void loadExpenses(int userId) {
        // 1. Update Total
        double total = expenseModel.getTotalExpenses(userId);
        System.out.println("Total Expenses: " + total);
        if (totalSpentLabel != null) {
            totalSpentLabel.setText(String.format("%.2f TND", total));
        } else {
            System.out.println("ERROR: totalSpentLabel is null!");
        }

        // 2. Load Cards
        List<Expense> expenses = expenseModel.getExpenses(userId);
        System.out.println("Loading " + expenses.size() + " expenses...");

        if (expensesContainer != null) {
            expensesContainer.getChildren().clear();
            if (expenses.isEmpty()) {
                Label emptyLabel = new Label("No expenses found yet.");
                emptyLabel.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-size: 14px;");
                expensesContainer.getChildren().add(emptyLabel);
            }
            for (Expense e : expenses) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/expense_card.fxml"));
                    // Create a simple controller for the card if needed, or just data bind here
                    // For simplicity, let's look up nodes and set data

                    Node card = loader.load();

                    Label merchantLbl = (Label) card.lookup("#merchantLabel");
                    Label dateLbl = (Label) card.lookup("#dateLabel");
                    Label amountLbl = (Label) card.lookup("#amountLabel");

                    if (merchantLbl != null)
                        merchantLbl.setText(e.getMerchant());
                    if (dateLbl != null && e.getCreatedAt() != null) {
                        // Format: YYYY-MM-DD HH:mm
                        String dateStr = e.getCreatedAt().toString();
                        if (dateStr.length() >= 16) {
                            dateLbl.setText(dateStr.substring(0, 16));
                        } else {
                            dateLbl.setText(dateStr);
                        }
                    }
                    if (amountLbl != null)
                        amountLbl.setText(String.format("%.2f TND", e.getTotalAmount()));

                    // Add click listener for details
                    card.setOnMouseClicked(event -> showExpenseDetails(e));

                    expensesContainer.getChildren().add(card);
                    System.out.println("Added card for: " + e.getMerchant());

                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.out.println("Error loading card: " + ex.getMessage());
                }
            }
        } else {
            System.out.println("ERROR: expensesContainer is null!");
        }
    }

    private void showExpenseDetails(Expense expense) {
        // Lazy load items if they haven't been fetched yet
        if (expense.getItems().isEmpty()) {
            List<ExpenseItem> items = expenseModel.getExpenseItems(expense.getId());
            expense.setItems(items);
        }

        StringBuilder details = new StringBuilder();
        details.append("Merchant: ").append(expense.getMerchant()).append("\n");
        details.append("Total: ").append(String.format("%.2f TND", expense.getTotalAmount())).append("\n");
        if (expense.getCreatedAt() != null) {
            details.append("Date: ").append(expense.getCreatedAt().toString().substring(0, 16)).append("\n");
        }
        details.append("\nItems (").append(expense.getItems().size()).append("):\n");

        if (expense.getItems().isEmpty()) {
            details.append("- No individual items found.");
        } else {
            for (ExpenseItem item : expense.getItems()) {
                details.append("- ").append(item.getItemName())
                        .append(": ").append(item.getPrice()).append(" TND\n");
            }
        }

        DialogUtil.showInfo("Expense Details", details.toString());
    }
}
