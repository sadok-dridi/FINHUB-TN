package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import tn.finhub.model.FinancialProfile;
import tn.finhub.model.FinancialProfileModel;

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

    private final FinancialProfileModel profileModel = new FinancialProfileModel();
    // private final FinancialTwinService twinService = new FinancialTwinService();
    // private final FinancialProfileDAO profileDAO = new FinancialProfileDAOImpl();
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
        baseProfile = profileModel.findByUserId(userId);

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
        int score = profileModel.calculateHealthScore(currentIncome, currentExpenses);
        updateHealthUI(score);

        // 2. Run Projection
        List<Double> projection = profileModel.runSimulation(currentIncome, currentExpenses, 12);
        updateChart(projection);

        // 3. Generate Insights
        String insights = profileModel.generateKeyInsights(currentIncome, currentExpenses, savingsGoal);
        insightsArea.setText(insights);
    }

    private void updateHealthUI(int score) {
        healthScoreLabel.setText(String.valueOf(score));
        healthIndicator.setProgress(score / 100.0);

        healthStatusLabel.setText(profileModel.getHealthStatus(score));

        String colorStyle = profileModel.getHealthColor(score);
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

    private final tn.finhub.model.MarketModel marketModel = new tn.finhub.model.MarketModel();
    // private final SimulationService simulationService = new SimulationService();
    // // Removed
    // private final AIAdvisoryService aiService = new AIAdvisoryService(); //
    // Removed
    private final tn.finhub.model.WalletModel walletModel = new tn.finhub.model.WalletModel();
    // private final tn.finhub.dao.MarketDAO marketDAO = new
    // tn.finhub.dao.MarketDAO(); // Removed

    private String selectedAsset = null;

    // --- Market Memory Cache ---
    private static final java.util.Map<String, tn.finhub.model.PortfolioItem> portfolioCache = new java.util.concurrent.ConcurrentHashMap<>();

    public static void setCachedPortfolio(java.util.Map<String, tn.finhub.model.PortfolioItem> cache) {
        portfolioCache.clear();
        portfolioCache.putAll(cache);
    }

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

            private final javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();

            {
                // Root Layout
                root.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                root.setSpacing(10);
                root.setPadding(new javafx.geometry.Insets(6, 10, 6, 10));
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                // Icon Config
                iconView.setFitWidth(24);
                iconView.setFitHeight(24);
                iconView.setClip(clip);
                clip.setRadius(12);
                clip.setCenterX(12);
                clip.setCenterY(12);

                // Name Box
                nameBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                nameBox.setSpacing(0);
                nameBox.getChildren().addAll(symbolLabel, ownedLabel);

                // Owned Badge Config
                ownedLabel.setVisible(false);
                ownedLabel.setManaged(false);
                ownedLabel.setStyle(
                        "-fx-text-fill: #10b981; -fx-font-size: 8px; -fx-font-weight: bold; -fx-border-color: #10b981; -fx-border-radius: 3px; -fx-padding: 0 3; -fx-border-width: 0.5px;");

                root.getChildren().addAll(iconView, nameBox, spacer);

                // Styles
                symbolLabel.getStyleClass().add("asset-symbol");
            }

            @Override
            protected void updateItem(String symbol, boolean empty) {
                super.updateItem(symbol, empty);

                if (empty || symbol == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    // 1. Icon (Fetch from CDN) - Background loading by JavaFX Image
                    String ticker = getTicker(symbol);
                    String iconUrl = "https://assets.coincap.io/assets/icons/" + ticker + "@2x.png";
                    javafx.scene.image.Image image = new javafx.scene.image.Image(iconUrl, true);
                    iconView.setImage(image);

                    // 2. Name
                    symbolLabel.setText(symbol.substring(0, 1).toUpperCase() + symbol.substring(1));

                    // 3. Check Ownership (FROM CACHE - NON-BLOCKING)
                    tn.finhub.model.PortfolioItem item = portfolioCache.get(symbol);
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
        int userId = UserSession.getInstance().getUser() != null ? UserSession.getInstance().getUser().getId() : 0;

        javafx.concurrent.Task<Void> refreshTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                // 1. Refresh Portfolio Cache (Batch Fetch)
                List<tn.finhub.model.PortfolioItem> items = marketModel.getPortfolio(userId);
                portfolioCache.clear();
                for (tn.finhub.model.PortfolioItem item : items) {
                    portfolioCache.put(item.getSymbol(), item);
                }

                // 2. Refresh Market Prices (Bypass Cache for Fresh Data)
                marketModel.getPrices(TRACKED_ASSETS, true);
                return null;
            }
        };

        refreshTask.setOnSucceeded(e -> {
            // Updated UI on FX Thread
            if (selectedAsset != null)
                selectAsset(selectedAsset); // Will use cached data now

            assetListView.refresh(); // Triggers updateItem which now uses cache
        });

        new Thread(refreshTask).start();
    }

    @FXML
    public void handleRefreshMarket(javafx.event.ActionEvent actionEvent) {
        // Disabled manual refresh as per user request
    }

    private void selectAsset(String symbol) {
        this.selectedAsset = symbol;
        selectedAssetLabel.setText(symbol);

        // Fetch Price and Rate in Background
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                tn.finhub.model.MarketPrice price = marketModel.getPrice(symbol);
                java.math.BigDecimal rate = marketModel.getUsdToTndRate();

                javafx.application.Platform.runLater(() -> {
                    if (price != null) {
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
                        String advice = marketModel.getRecommendation(price, baseProfile.getRiskTolerance());
                        aiAdviceBox.setVisible(true);
                        aiAdviceLabel.setText(advice);

                    }
                });
                return null;
            }
        };
        new Thread(task).start();

        // Candle Stick Chart
        new Thread(() -> {
            java.util.List<tn.finhub.model.CandleData> ohlc = marketModel.getOHLC(symbol);
            javafx.application.Platform.runLater(() -> {
                this.fullCandleData = ohlc;
                this.visibleCandleCount = ohlc.size(); // Reset zoom to full
                renderChart();
            });
        }).start();

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
        // Optimistic UI update from Cache first
        tn.finhub.model.PortfolioItem cachedItem = portfolioCache.get(symbol);
        if (cachedItem != null) {
            ownedQuantityLabel.setText(cachedItem.getQuantity().toString());
            // For cost we need rate, might still need bg fetch or cache rate
        } else {
            ownedQuantityLabel.setText("0");
        }

        int userId = UserSession.getInstance().getUser().getId();

        // Use a background task to fetch portfolio data (Fresh)
        javafx.concurrent.Task<tn.finhub.model.PortfolioItem> task = new javafx.concurrent.Task<>() {
            @Override
            protected tn.finhub.model.PortfolioItem call() throws Exception {
                // Update Cache on specific fetch
                tn.finhub.model.PortfolioItem item = marketModel.getPortfolioItem(userId, symbol);
                if (item != null)
                    portfolioCache.put(symbol, item);
                else
                    portfolioCache.remove(symbol);
                return item;
            }
        };

        task.setOnSucceeded(e -> {
            tn.finhub.model.PortfolioItem item = task.getValue();
            if (item != null) {
                ownedQuantityLabel.setText(item.getQuantity().toString());

                // Convert Avg Cost to TND for display
                // Need rate - fetch in BG or separate task?
                // Let's do a quick nested task or just fetch rate in the main task
                new Thread(() -> {
                    java.math.BigDecimal rate = marketModel.getUsdToTndRate();
                    javafx.application.Platform.runLater(() -> {
                        java.math.BigDecimal avgCostTnd = item.getAverageCost().multiply(rate);
                        avgCostLabel.setText(String.format("%.2f TND", avgCostTnd));
                    });
                }).start();

            } else {
                ownedQuantityLabel.setText("0");
                avgCostLabel.setText("0.00");
            }
        });

        task.setOnFailed(e -> {
            // Silently fail or log, but reset UI to valid state
            ownedQuantityLabel.setText("0");
            avgCostLabel.setText("0.00");
        });

        new Thread(task).start();
    }

    private void updatePortfolioSummary() {
        int userId = UserSession.getInstance().getUser().getId();

        javafx.concurrent.Task<tn.finhub.model.Wallet> task = new javafx.concurrent.Task<>() {
            @Override
            protected tn.finhub.model.Wallet call() throws Exception {
                return walletModel.findByUserId(userId);
            }
        };

        task.setOnSucceeded(e -> {
            tn.finhub.model.Wallet wallet = task.getValue();
            if (wallet != null) {
                walletBalanceLabel.setText(wallet.getBalance() + " " + wallet.getCurrency());
            }
        });

        new Thread(task).start();
    }

    // Track estimation task to cancel previous ones if typing fast
    private javafx.concurrent.Task<String> estCostTask;

    private void updateEstCost() {
        if (selectedAsset == null)
            return;

        // Cancel previous calculation
        if (estCostTask != null && estCostTask.isRunning()) {
            estCostTask.cancel();
        }

        String qtyText = quantityField.getText();
        if (qtyText.isEmpty()) {
            estCostLabel.setText("0.00");
            return;
        }

        estCostTask = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                double qty = Double.parseDouble(qtyText);
                tn.finhub.model.MarketPrice price = marketModel.getPrice(selectedAsset);
                if (price != null) {
                    java.math.BigDecimal rate = marketModel.getUsdToTndRate();
                    double cost = price.getPrice().multiply(rate).doubleValue() * qty;
                    return String.format("%.2f TND", cost);
                }
                return "0.00";
            }
        };

        estCostTask.setOnSucceeded(e -> estCostLabel.setText(estCostTask.getValue()));
        estCostTask.setOnFailed(e -> estCostLabel.setText("0.00")); // likely number format exception

        new Thread(estCostTask).start();
    }

    @FXML
    private void handleBuy() {
        if (selectedAsset == null) {
            DialogUtil.showError("Market", "Please select an asset.");
            return;
        }

        String quantityText = quantityField.getText();
        java.math.BigDecimal qty;
        try {
            qty = new java.math.BigDecimal(quantityText);
        } catch (NumberFormatException e) {
            DialogUtil.showError("Error", "Invalid quantity.");
            return;
        }

        int userId = UserSession.getInstance().getUser().getId();

        // Show loading state (optional: disable button)
        // Button buyBtn = ... (if bound)

        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                return marketModel.buyAsset(userId, selectedAsset, qty);
            }
        };

        task.setOnSucceeded(e -> {
            String result = task.getValue();
            if ("SUCCESS".equals(result)) {
                DialogUtil.showInfo("Trade Executed", "Bought " + qty + " of " + selectedAsset);
                updatePortfolioUIForAsset(selectedAsset);
                updatePortfolioSummary();
                handleRefreshMarket(); // Refresh prices
            } else {
                DialogUtil.showError("Trade Failed", result);
            }
        });

        task.setOnFailed(e -> {
            DialogUtil.showError("Error", "System error: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    private void handleSell() {
        if (selectedAsset == null) {
            DialogUtil.showError("Market", "Please select an asset.");
            return;
        }

        String quantityText = quantityField.getText();
        java.math.BigDecimal qty;
        try {
            qty = new java.math.BigDecimal(quantityText);
        } catch (NumberFormatException e) {
            DialogUtil.showError("Error", "Invalid quantity.");
            return;
        }

        int userId = UserSession.getInstance().getUser().getId();

        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                return marketModel.sellAsset(userId, selectedAsset, qty);
            }
        };

        task.setOnSucceeded(e -> {
            String result = task.getValue();
            if ("SUCCESS".equals(result)) {
                DialogUtil.showInfo("Trade Executed", "Sold " + qty + " of " + selectedAsset);
                updatePortfolioUIForAsset(selectedAsset);
                updatePortfolioSummary();
                handleRefreshMarket();
            } else {
                DialogUtil.showError("Trade Failed", result);
            }
        });

        task.setOnFailed(e -> {
            DialogUtil.showError("Error", "System error: " + task.getException().getMessage());
        });

        new Thread(task).start();
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
