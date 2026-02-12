package tn.finhub.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import tn.finhub.model.MarketModel;
import tn.finhub.model.MarketPrice;
import tn.finhub.model.PortfolioItem;
import tn.finhub.model.User;
import tn.finhub.util.DialogUtil;
import tn.finhub.util.ViewUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AdminUserPortfolioController {

    @FXML
    private Label userNameLabel;
    @FXML
    private Label totalValueLabel;
    @FXML
    private ListView<PortfolioItem> assetListView;

    private User currentUser;
    private final MarketModel marketModel = new MarketModel();

    // Image Cache
    private static final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    public void setUser(User user) {
        this.currentUser = user;
        if (currentUser != null) {
            userNameLabel.setText(currentUser.getFullName() + "'s Portfolio");
            loadPortfolio();
        }
    }

    private void loadPortfolio() {
        if (currentUser == null)
            return;

        totalValueLabel.setText("Loading...");
        assetListView.setPlaceholder(new Label("Loading data..."));

        new Thread(() -> {
            try {
                // 1. Fetch Portfolio
                List<PortfolioItem> items = marketModel.getPortfolio(currentUser.getId());

                // 2. Fetch Prices for calculation
                BigDecimal totalValueTnd = BigDecimal.ZERO;
                BigDecimal rate = marketModel.getUsdToTndRate();

                for (PortfolioItem item : items) {
                    // 1. Price Calculation
                    MarketPrice price = marketModel.getPrice(item.getSymbol());
                    if (price != null) {
                        BigDecimal valUsd = price.getPrice().multiply(item.getQuantity());
                        totalValueTnd = totalValueTnd.add(valUsd.multiply(rate));
                    }

                    // 2. Pre-load Image (Blocking in this thread to ensure ready before UI)
                    if (!imageCache.containsKey(item.getSymbol())) {
                        String ticker = getTicker(item.getSymbol());
                        String iconUrl = "https://assets.coincap.io/assets/icons/" + ticker + "@2x.png";
                        try {
                            // false = synchronous loading
                            Image img = new Image(iconUrl, false);
                            // Force loading by checking width (optional, but ensures it's really read)
                            if (img.getWidth() > 0) {
                                imageCache.put(item.getSymbol(), img);
                            } else {
                                // If failed or empty, maybe just let it be, or put it anyway
                                imageCache.put(item.getSymbol(), img);
                            }
                        } catch (Exception ignored) {
                            // Fallback will handle it
                        }
                    }
                }

                BigDecimal finalTotal = totalValueTnd.setScale(2, RoundingMode.HALF_UP);

                javafx.application.Platform.runLater(() -> {
                    assetListView.setPlaceholder(new Label("No assets found in this portfolio."));
                    assetListView.getItems().setAll(items);
                    totalValueLabel.setText(finalTotal + " TND");
                });

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    assetListView.setPlaceholder(new Label("Failed to load data."));
                    DialogUtil.showError("Error", "Failed to load portfolio.");
                });
            }
        }).start();
    }

    @FXML
    public void initialize() {
        setupListView();
    }

    private void setupListView() {
        assetListView.setCellFactory(lv -> new ListCell<>() {
            private final HBox root = new HBox();
            private final ImageView iconView = new ImageView();
            private final Circle clip = new Circle(16, 16, 16);

            private final VBox nameBox = new VBox();
            private final Label symbolLabel = new Label();
            private final Label nameLabel = new Label(); // Could be full name if available, using symbol for now

            private final Region spacer = new Region();

            private final VBox statsBox = new VBox();
            private final Label qtyLabel = new Label();
            private final Label valueLabel = new Label();

            {
                root.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                root.setSpacing(15);
                root.setPadding(new javafx.geometry.Insets(10));
                root.setStyle(
                        "-fx-background-color: -color-card-bg; -fx-background-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 0);");

                // Icon
                iconView.setFitWidth(32);
                iconView.setFitHeight(32);
                iconView.setClip(clip);
                clip.setRadius(16);
                clip.setCenterX(16);
                clip.setCenterY(16);

                // Name
                nameBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                symbolLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-primary; -fx-font-size: 14px;");
                nameLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 12px;");
                nameBox.getChildren().addAll(symbolLabel, nameLabel);

                // Spacer
                HBox.setHgrow(spacer, Priority.ALWAYS);

                // Stats
                statsBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                valueLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-primary; -fx-font-size: 14px;");
                qtyLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 12px;");
                statsBox.getChildren().addAll(valueLabel, qtyLabel);

                root.getChildren().addAll(iconView, nameBox, spacer, statsBox);
            }

            @Override
            protected void updateItem(PortfolioItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 5; -fx-border-width: 0;");
                } else {
                    // Symbol
                    String symbol = item.getSymbol().toUpperCase();
                    symbolLabel.setText(symbol);
                    nameLabel.setText("Crypto Asset"); // Placeholder or map symbol to name if possible

                    // Icon
                    String ticker = getTicker(item.getSymbol());
                    String iconUrl = "https://assets.coincap.io/assets/icons/" + ticker + "@2x.png";

                    Image image = imageCache.get(item.getSymbol());
                    if (image == null) {
                        image = new Image(iconUrl, true); // Background loading
                        imageCache.put(item.getSymbol(), image);
                    }
                    iconView.setImage(image);

                    // Calculations (async fetch potential needed for perfect scroll, but doing
                    // naive for now or pre-fetch)
                    // We need price to show value.
                    // Optimization: We could reuse valid cached prices from MarketModel.

                    BigDecimal qty = item.getQuantity();
                    qtyLabel.setText(qty + " " + symbol);

                    // Fetch price non-blocking or use what is available
                    // Ideally we should have prices pre-fetched in loadPortfolio and passed in a
                    // wrapper/DTO
                    // For now, we'll try to get cached price from MarketModel
                    MarketPrice price = marketModel.getPrice(item.getSymbol());
                    if (price != null) {
                        BigDecimal rate = marketModel.getUsdToTndRate();
                        BigDecimal valTnd = price.getPrice().multiply(qty).multiply(rate).setScale(2,
                                RoundingMode.HALF_UP);
                        valueLabel.setText(valTnd + " TND");
                    } else {
                        valueLabel.setText("--- TND");
                    }

                    setGraphic(root);
                    setText(null);
                    // Reset style for list cell container if needed, but root has its own style
                    setStyle("-fx-background-color: transparent; -fx-padding: 5; -fx-border-width: 0;");
                }
            }
        });
    }

    private String getTicker(String symbol) {
        if (symbol == null)
            return "";
        return switch (symbol.toLowerCase()) {
            case "bitcoin" -> "btc";
            case "ethereum" -> "eth";
            case "binancecoin" -> "bnb";
            case "solana" -> "sol";
            case "ripple" -> "xrp";
            case "cardano" -> "ada";
            case "avalanche-2" -> "avax";
            case "dogecoin" -> "doge";
            case "polkadot" -> "dot";
            case "tron" -> "trx";
            case "chainlink" -> "link";
            case "matic-network" -> "matic";
            case "shiba-inu" -> "shib";
            case "litecoin" -> "ltc";
            case "bitcoin-cash" -> "bch";
            case "uniswap" -> "uni";
            case "stellar" -> "xlm";
            case "monero" -> "xmr";
            case "ethereum-classic" -> "etc";
            case "near" -> "near";
            case "filecoin" -> "fil";
            case "internet-computer" -> "icp";
            case "apecoin" -> "ape";
            case "algorand" -> "algo";
            case "vechain" -> "vet";
            case "cosmos" -> "atom";
            case "decentraland" -> "mana";
            case "the-sandbox" -> "sand";
            case "aave" -> "aave";
            case "tezos" -> "xtz";
            default -> symbol.toLowerCase().substring(0, Math.min(3, symbol.length()));
        };
    }

    @FXML
    private void handleBack() {
        StackPane contentArea = (StackPane) userNameLabel.getScene().lookup("#adminContentArea");
        if (contentArea != null) {
            // Load the view and get the loader
            javafx.fxml.FXMLLoader loader = ViewUtils.loadContent(contentArea, "/view/admin_user_details.fxml");

            // Get controller and pass data
            if (loader != null) {
                Object controller = loader.getController();
                if (controller instanceof AdminUserDetailsController detailsController) {
                    detailsController.setUser(currentUser);
                }
            }
        }
    }
}
