package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import tn.finhub.dao.FinancialProfileDAO;
import tn.finhub.dao.impl.FinancialProfileDAOImpl;
import tn.finhub.model.FinancialProfile;
import tn.finhub.service.AIAdvisoryService;
import tn.finhub.service.FinancialTwinService;
import tn.finhub.service.MarketDataService;
import tn.finhub.service.SimulationService;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.UserSession;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FinancialTwinController {

    @FXML
    private Slider incomeSlider;
    @FXML
    private TextField incomeField;
    @FXML
    private Slider expenseSlider;
    @FXML
    private TextField expenseField;
    @FXML
    private TextField goalField;

    @FXML
    private ProgressIndicator healthIndicator;
    @FXML
    private Label healthScoreLabel;
    @FXML
    private Label healthStatusLabel;

    @FXML
    private LineChart<String, Number> projectionsChart;
    @FXML
    private TextArea insightsArea;

    private final FinancialTwinService twinService = new FinancialTwinService();
    private final FinancialProfileDAO profileDAO = new FinancialProfileDAOImpl();
    private FinancialProfile baseProfile;

    // Simulation State
    private double currentIncome = 0;
    private double currentExpenses = 0;

    @FXML
    private TabPane mainTabPane;
    @FXML
    private Tab marketTab;

    @FXML
    public void initialize() {
        setupListeners();
        loadUserProfile();
        setupMarket();

        // Default to Market Tab
        if (mainTabPane != null && marketTab != null) {
            mainTabPane.getSelectionModel().select(marketTab);
        }
    }

    private void setupListeners() {
        // Bi-directional binding logic for sliders and text fields using listeners
        incomeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!incomeField.isFocused()) {
                incomeField.setText(String.format("%.0f", newVal.doubleValue()));
                currentIncome = newVal.doubleValue();
            }
        });

        incomeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (incomeField.isFocused()) {
                try {
                    double val = Double.parseDouble(newVal);
                    if (val >= 0 && val <= incomeSlider.getMax()) {
                        incomeSlider.setValue(val);
                        currentIncome = val;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        });

        expenseSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!expenseField.isFocused()) {
                expenseField.setText(String.format("%.0f", newVal.doubleValue()));
                currentExpenses = newVal.doubleValue();
            }
        });

        expenseField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (expenseField.isFocused()) {
                try {
                    double val = Double.parseDouble(newVal);
                    if (val >= 0 && val <= expenseSlider.getMax()) {
                        expenseSlider.setValue(val);
                        currentExpenses = val;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        });
    }

    private void loadUserProfile() {
        int userId = UserSession.getInstance().getUser() != null ? UserSession.getInstance().getUser().getId() : 0;
        baseProfile = profileDAO.findByUserId(userId);

        if (baseProfile == null) {
            // Initialize default/fresh profile
            baseProfile = new FinancialProfile(userId, 0, 0, 0, "MEDIUM", "TND", false);
            // In a real app, maybe prompt to complete profile first
        }

        resetInputsToProfile();
        runSimulation();
    }

    @FXML
    private void handleReset() {
        resetInputsToProfile();
        runSimulation();
    }

    private void resetInputsToProfile() {
        currentIncome = baseProfile.getMonthlyIncome();
        currentExpenses = baseProfile.getMonthlyExpenses();

        incomeSlider.setValue(currentIncome);
        incomeField.setText(String.valueOf((int) currentIncome));

        expenseSlider.setValue(currentExpenses);
        expenseField.setText(String.valueOf((int) currentExpenses));

        goalField.setText(String.valueOf((int) baseProfile.getSavingsGoal()));
    }

    @FXML
    private void handleRunSimulation() {
        // Parse manual changes from text fields just in case
        try {
            currentIncome = Double.parseDouble(incomeField.getText());
            currentExpenses = Double.parseDouble(expenseField.getText());
        } catch (NumberFormatException e) {
            DialogUtil.showError("Invalid Input", "Please enter valid numeric values.");
            return;
        }

        runSimulation();
    }

    private void runSimulation() {
        double savingsGoal = 0;
        try {
            savingsGoal = Double.parseDouble(goalField.getText());
        } catch (Exception ignored) {
        }

        // 1. Calculate Health Score
        int score = twinService.calculateHealthScore(currentIncome, currentExpenses);
        updateHealthUI(score);

        // 2. Run Projection
        List<Double> projection = twinService.runSimulation(currentIncome, currentExpenses, 12);
        updateChart(projection);

        // 3. Generate Insights
        String insights = twinService.generateKeyInsights(currentIncome, currentExpenses, savingsGoal);
        insightsArea.setText(insights);
    }

    private void updateHealthUI(int score) {
        healthScoreLabel.setText(String.valueOf(score));
        healthIndicator.setProgress(score / 100.0);

        healthStatusLabel.setText(twinService.getHealthStatus(score));

        String colorStyle = twinService.getHealthColor(score);
        // This requires the color variable name to be resolved or set via style class
        // Mapping variable name to hardcoded color for safety or use styleClass
        healthStatusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: " + colorStyle + ";");

        // Dynamic styling for indicator (JavaFX ProgressIndicator styling is tricky,
        // using CSS lookup mainly)
        healthIndicator.setStyle("-fx-progress-color: " + colorStyle + ";");
    }

    private void updateChart(List<Double> data) {
        projectionsChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("projected Savings");

        LocalDate date = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM");

        for (int i = 0; i < data.size(); i++) {
            date = date.plusMonths(1);
            series.getData().add(new XYChart.Data<>(date.format(formatter), data.get(i)));
        }

        projectionsChart.getData().add(series);
    }

    // --- MARKET SIMULATION LOGIC ---

    @FXML
    private ListView<String> assetListView;
    @FXML
    private Label selectedAssetLabel;
    @FXML
    private Label selectedPriceLabel;
    @FXML
    private Label selectedChangeLabel;
    @FXML
    private HBox aiAdviceBox;
    @FXML
    private Label aiAdviceLabel;
    @FXML
    private javafx.scene.layout.VBox chartContainer;

    // ... (keep fields same until marketChart) ...
    // Removed LineChart field

    private tn.finhub.util.CandleStickChart candleStickChart;

    @FXML
    private TextField quantityField;
    @FXML
    private Label estCostLabel;
    @FXML
    private Label ownedQuantityLabel;
    @FXML
    private Label avgCostLabel;
    @FXML
    private Label walletBalanceLabel;

    private final MarketDataService marketDataService = new MarketDataService();
    private final SimulationService simulationService = new SimulationService();
    private final AIAdvisoryService aiService = new AIAdvisoryService();
    private final tn.finhub.dao.WalletDAO walletDAO = new tn.finhub.dao.WalletDAO();
    private final tn.finhub.dao.MarketDAO marketDAO = new tn.finhub.dao.MarketDAO();

    private String selectedAsset = null;

    private void setupMarket() {
        // Initialize Chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("");
        yAxis.setLabel("");
        yAxis.setForceZeroInRange(false);

        candleStickChart = new tn.finhub.util.CandleStickChart(xAxis, yAxis);
        candleStickChart.setLegendVisible(false);
        candleStickChart.setOnScroll(this::handleChartScroll);
        candleStickChart.enableCrosshair();
        chartContainer.getChildren().add(candleStickChart);

        // Initialize List with Custom Cell Factory
        assetListView.getItems().addAll(TRACKED_ASSETS);
        assetListView.setCellFactory(lv -> new ListCell<String>() {
            // Layout Components
            private final HBox root = new HBox();
            private final javafx.scene.image.ImageView iconView = new javafx.scene.image.ImageView();
            private final javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(16, 16, 16);

            private final javafx.scene.layout.VBox nameBox = new javafx.scene.layout.VBox();
            private final Label symbolLabel = new Label();
            private final Label ownedLabel = new Label("OWNED");

            private final javafx.scene.layout.VBox infoBox = new javafx.scene.layout.VBox();
            private final Label priceLabel = new Label();
            private final Label changeLabel = new Label();

            private final javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();

            {
                // Root Layout
                root.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                root.setSpacing(10); // Reduced spacing
                root.setPadding(new javafx.geometry.Insets(6, 10, 6, 10)); // Compact padding
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                // Icon Config
                iconView.setFitWidth(24); // Smaller Icon
                iconView.setFitHeight(24); // Smaller Icon
                iconView.setClip(clip);
                clip.setRadius(12); // Adjust clip radius (1/2 of 24)
                clip.setCenterX(12);
                clip.setCenterY(12);

                // Name Box
                nameBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                nameBox.setSpacing(0); // Tighter name box
                nameBox.getChildren().addAll(symbolLabel, ownedLabel);

                // Owned Badge Config
                ownedLabel.setVisible(false);
                ownedLabel.setManaged(false);
                ownedLabel.setStyle(
                        "-fx-text-fill: #10b981; -fx-font-size: 8px; -fx-font-weight: bold; -fx-border-color: #10b981; -fx-border-radius: 3px; -fx-padding: 0 3; -fx-border-width: 0.5px;");

                // Info Box (Right Side) - REMOVED per user request
                // infoBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                // infoBox.getChildren().addAll(priceLabel, changeLabel);

                // Add only icon, namebox (includes symbol + owned badge) and spacer
                // Spacer pushes content to left if we had right-side content, but now it just
                // fills space
                root.getChildren().addAll(iconView, nameBox, spacer);

                // Styles
                symbolLabel.getStyleClass().add("asset-symbol");
                // priceLabel.getStyleClass().add("asset-price");
                // changeLabel.getStyleClass().add("asset-change");
            }

            @Override
            protected void updateItem(String symbol, boolean empty) {
                super.updateItem(symbol, empty);

                if (empty || symbol == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    // 1. Icon (Fetch from CDN)
                    String ticker = getTicker(symbol);
                    String iconUrl = "https://assets.coincap.io/assets/icons/" + ticker + "@2x.png";
                    // Using background loading
                    javafx.scene.image.Image image = new javafx.scene.image.Image(iconUrl, true);
                    iconView.setImage(image);

                    // 2. Name
                    symbolLabel.setText(symbol.substring(0, 1).toUpperCase() + symbol.substring(1));

                    // 3. Price & Change - REMOVED
                    // tn.finhub.model.MarketPrice price = marketDataService.getPrice(symbol);
                    // if (price != null) {
                    // priceLabel.setText(String.format("$%.2f", price.getPrice()));
                    // double change = price.getChangePercent().doubleValue();
                    // changeLabel.setText(String.format("%+.2f%%", change));

                    // changeLabel.getStyleClass().removeAll("asset-change-pos",
                    // "asset-change-neg");
                    // if (change >= 0) {
                    // changeLabel.getStyleClass().add("asset-change-pos");
                    // } else {
                    // changeLabel.getStyleClass().add("asset-change-neg");
                    // }
                    // } else {
                    // priceLabel.setText("---");
                    // changeLabel.setText("");
                    // }

                    // 4. Check Ownership
                    int userId = UserSession.getInstance().getUser() != null
                            ? UserSession.getInstance().getUser().getId()
                            : 0;
                    tn.finhub.model.PortfolioItem item = marketDAO.getPortfolioItem(userId, symbol);
                    boolean isOwned = item != null && item.getQuantity().doubleValue() > 0;

                    ownedLabel.setVisible(isOwned);
                    ownedLabel.setManaged(isOwned);

                    setGraphic(root);
                    setText(null);
                }
            }
        });

        // Initialize Selection Listener
        assetListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectAsset(newVal);
            }
        });

        // Quantity Listener for Est Cost
        quantityField.textProperty().addListener((obs, old, newVal) -> {
            updateEstCost();
        });

        // Initial fetch
        handleRefreshMarket();
        updatePortfolioSummary();

        // Auto-Select Bitcoin
        assetListView.getSelectionModel().select("bitcoin");

        startAutoRefresh();
    }

    // Auto-Refresh Logic
    private void startAutoRefresh() {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(30), e -> handleRefreshMarket()));
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }

    private void handleRefreshMarket() {
        new Thread(() -> {
            // FORCE refresh in background to bypass the 60s read cache
            marketDataService.getPrices(TRACKED_ASSETS, true);
            javafx.application.Platform.runLater(() -> {
                if (selectedAsset != null)
                    selectAsset(selectedAsset); // Will read from fresh cache
                assetListView.refresh();
            });
        }).start();
    }

    @FXML
    public void handleRefreshMarket(javafx.event.ActionEvent actionEvent) {
        // Disabled manual refresh as per user request
    }

    private void selectAsset(String symbol) {
        this.selectedAsset = symbol;
        selectedAssetLabel.setText(symbol);

        tn.finhub.model.MarketPrice price = marketDataService.getPrice(symbol);
        if (price != null) {
            java.math.BigDecimal rate = marketDataService.getUsdToTndRate();
            java.math.BigDecimal priceTnd = price.getPrice().multiply(rate);
            selectedPriceLabel.setText(String.format("%.2f TND", priceTnd));
            selectedChangeLabel.setText(String.format("%.2f%%", price.getChangePercent()));

            // Color for change
            if (price.getChangePercent().doubleValue() >= 0) {
                selectedChangeLabel.setStyle("-fx-text-fill: -color-success; -fx-font-size: 14px;");
            } else {
                selectedChangeLabel.setStyle("-fx-text-fill: -color-error; -fx-font-size: 14px;");
            }

            // AI Advice
            String advice = aiService.getRecommendation(price, baseProfile.getRiskTolerance());
            aiAdviceBox.setVisible(true);
            aiAdviceLabel.setText(advice);

            // Populate CandleStickChart
            new Thread(() -> {
                java.util.List<tn.finhub.model.CandleData> ohlc = marketDataService.getOHLC(symbol);
                javafx.application.Platform.runLater(() -> {
                    this.fullCandleData = ohlc;
                    this.visibleCandleCount = ohlc.size(); // Reset zoom to full
                    renderChart();
                });
            }).start();
        }

        updatePortfolioUIForAsset(symbol);
        updateEstCost();
    }

    private void renderChart() {
        if (fullCandleData == null || fullCandleData.isEmpty())
            return;

        candleStickChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("OHLC");

        // Zoom Logic: Slice the data
        int size = fullCandleData.size();
        int safeCount = Math.min(visibleCandleCount, size);
        int startIndex = size - safeCount;

        java.util.List<tn.finhub.model.CandleData> viewData = fullCandleData.subList(Math.max(0, startIndex), size);

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        for (tn.finhub.model.CandleData d : viewData) {
            String timeStr = d.getTimestamp().toLocalDateTime().format(fmt);
            XYChart.Data<String, Number> data = new XYChart.Data<>(timeStr, d.getClose());
            data.setExtraValue(d);
            series.getData().add(data);
        }
        candleStickChart.getData().add(series);
    }

    private java.util.List<tn.finhub.model.CandleData> fullCandleData = new java.util.ArrayList<>();
    private int visibleCandleCount = 50;

    private void handleChartScroll(javafx.scene.input.ScrollEvent event) {
        if (fullCandleData == null || fullCandleData.isEmpty())
            return;
        event.consume();

        double deltaY = event.getDeltaY();
        // Zoom In (Scroll Up) -> Show fewer candles
        // Zoom Out (Scroll Down) -> Show more candles
        int zoomFactor = 2;

        if (deltaY > 0) {
            visibleCandleCount = Math.max(10, visibleCandleCount - zoomFactor);
        } else {
            visibleCandleCount = Math.min(fullCandleData.size(), visibleCandleCount + zoomFactor);
        }
        renderChart();
    }

    private void updatePortfolioUIForAsset(String symbol) {
        int userId = UserSession.getInstance().getUser().getId();
        tn.finhub.model.PortfolioItem item = marketDAO.getPortfolioItem(userId, symbol);

        if (item != null) {
            ownedQuantityLabel.setText(item.getQuantity().toString());
            // Convert Avg Cost to TND for display
            java.math.BigDecimal rate = marketDataService.getUsdToTndRate();
            java.math.BigDecimal avgCostTnd = item.getAverageCost().multiply(rate);
            avgCostLabel.setText(String.format("%.2f TND", avgCostTnd));
        } else {
            ownedQuantityLabel.setText("0");
            avgCostLabel.setText("0.00");
        }
    }

    private void updatePortfolioSummary() {
        int userId = UserSession.getInstance().getUser().getId();
        tn.finhub.model.Wallet wallet = walletDAO.findByUserId(userId);
        if (wallet != null) {
            walletBalanceLabel.setText(wallet.getBalance() + " " + wallet.getCurrency());
        }
    }

    private void updateEstCost() {
        if (selectedAsset == null)
            return;
        try {
            double qty = Double.parseDouble(quantityField.getText());
            tn.finhub.model.MarketPrice price = marketDataService.getPrice(selectedAsset);
            if (price != null) {
                java.math.BigDecimal rate = marketDataService.getUsdToTndRate();
                double cost = price.getPrice().multiply(rate).doubleValue() * qty;
                estCostLabel.setText(String.format("%.2f TND", cost));
            }
        } catch (NumberFormatException e) {
            estCostLabel.setText("0.00");
        }
    }

    @FXML
    private void handleBuy() {
        if (selectedAsset == null) {
            DialogUtil.showError("Market", "Please select an asset.");
            return;
        }
        try {
            java.math.BigDecimal qty = new java.math.BigDecimal(quantityField.getText());
            int userId = UserSession.getInstance().getUser().getId();

            String result = simulationService.buyAsset(userId, selectedAsset, qty);

            if ("SUCCESS".equals(result)) {
                DialogUtil.showInfo("Trade Executed", "Bought " + qty + " of " + selectedAsset);
                updatePortfolioUIForAsset(selectedAsset);
                updatePortfolioSummary();
                handleRefreshMarket(); // Refresh prices
            } else {
                DialogUtil.showError("Trade Failed", result);
            }
        } catch (Exception e) {
            DialogUtil.showError("Error", "Invalid quantity or system error: " + e.getMessage());
        }
    }

    @FXML
    private void handleSell() {
        if (selectedAsset == null) {
            DialogUtil.showError("Market", "Please select an asset.");
            return;
        }
        try {
            java.math.BigDecimal qty = new java.math.BigDecimal(quantityField.getText());
            int userId = UserSession.getInstance().getUser().getId();

            String result = simulationService.sellAsset(userId, selectedAsset, qty);

            if ("SUCCESS".equals(result)) {
                DialogUtil.showInfo("Trade Executed", "Sold " + qty + " of " + selectedAsset);
                updatePortfolioUIForAsset(selectedAsset);
                updatePortfolioSummary();
                handleRefreshMarket();
            } else {
                DialogUtil.showError("Trade Failed", result);
            }
        } catch (Exception e) {
            DialogUtil.showError("Error", "Invalid quantity or system error: " + e.getMessage());
        }
    }

    // Default assets to track (CoinGecko IDs)
    private final String[] TRACKED_ASSETS = {
            "bitcoin", "ethereum", "binancecoin", "solana", "ripple",
            "cardano", "avalanche-2", "dogecoin", "polkadot", "tron",
            "chainlink", "shiba-inu", "litecoin", "bitcoin-cash",
            "uniswap", "stellar", "monero", "ethereum-classic", "near",
            "filecoin", "internet-computer", "apecoin", "algorand", "vechain",
            "cosmos", "decentraland", "the-sandbox", "aave", "tezos"
    };

    private String getTicker(String symbol) {
        if (symbol == null)
            return "";
        switch (symbol.toLowerCase()) {
            case "bitcoin":
                return "btc";
            case "ethereum":
                return "eth";
            case "binancecoin":
                return "bnb";
            case "solana":
                return "sol";
            case "ripple":
                return "xrp";
            case "cardano":
                return "ada";
            case "avalanche-2":
                return "avax";
            case "dogecoin":
                return "doge";
            case "polkadot":
                return "dot";
            case "tron":
                return "trx";
            case "chainlink":
                return "link";
            case "matic-network":
                return "matic";
            case "shiba-inu":
                return "shib";
            case "litecoin":
                return "ltc";
            case "bitcoin-cash":
                return "bch";
            case "uniswap":
                return "uni";
            case "stellar":
                return "xlm";
            case "monero":
                return "xmr";
            case "ethereum-classic":
                return "etc";
            case "near":
                return "near";
            case "filecoin":
                return "fil";
            case "internet-computer":
                return "icp";
            case "apecoin":
                return "ape";
            case "algorand":
                return "algo";
            case "vechain":
                return "vet";
            case "cosmos":
                return "atom";
            case "decentraland":
                return "mana";
            case "the-sandbox":
                return "sand";
            case "aave":
                return "aave";
            case "tezos":
                return "xtz";
            default:
                return symbol.toLowerCase().substring(0, Math.min(3, symbol.length()));
        }
    }
}
