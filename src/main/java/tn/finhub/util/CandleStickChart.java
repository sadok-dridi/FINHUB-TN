package tn.finhub.util;

import javafx.animation.FadeTransition;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.util.Duration;
import tn.finhub.model.CandleData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CandleStickChart extends XYChart<String, Number> {

    public CandleStickChart(Axis<String> xAxis, Axis<Number> yAxis) {
        super(xAxis, yAxis);
        setAnimated(false);
        xAxis.setAnimated(false);
        yAxis.setAnimated(false);
        setData(javafx.collections.FXCollections.observableArrayList());
    }

    @Override
    protected void dataItemAdded(Series<String, Number> series, int itemIndex, Data<String, Number> item) {
        Node candle = createCandle(getData().indexOf(series), item, itemIndex);
        if (shouldAnimate()) {
            candle.setOpacity(0);
            getPlotChildren().add(candle);
            FadeTransition ft = new FadeTransition(Duration.millis(500), candle);
            ft.setToValue(1);
            ft.play();
        } else {
            getPlotChildren().add(candle);
        }
    }

    @Override
    protected void dataItemRemoved(Data<String, Number> item, Series<String, Number> series) {
        final Node candle = item.getNode();
        if (shouldAnimate()) {
            FadeTransition ft = new FadeTransition(Duration.millis(500), candle);
            ft.setToValue(0);
            ft.setOnFinished(actionEvent -> getPlotChildren().remove(candle));
            ft.play();
        } else {
            getPlotChildren().remove(candle);
        }
    }

    @Override
    protected void dataItemChanged(Data<String, Number> item) {
    }

    @Override
    protected void seriesAdded(Series<String, Number> series, int seriesIndex) {
        for (int j = 0; j < series.getData().size(); j++) {
            Data<String, Number> item = series.getData().get(j);
            Node candle = createCandle(seriesIndex, item, j);
            if (shouldAnimate()) {
                candle.setOpacity(0);
                getPlotChildren().add(candle);
                FadeTransition ft = new FadeTransition(Duration.millis(500), candle);
                ft.setToValue(1);
                ft.play();
            } else {
                getPlotChildren().add(candle);
            }
        }
    }

    @Override
    protected void seriesRemoved(Series<String, Number> series) {
        for (Data<String, Number> item : series.getData()) {
            final Node candle = item.getNode();
            if (shouldAnimate()) {
                FadeTransition ft = new FadeTransition(Duration.millis(500), candle);
                ft.setToValue(0);
                ft.setOnFinished(actionEvent -> getPlotChildren().remove(candle));
                ft.play();
            } else {
                getPlotChildren().remove(candle);
            }
        }
    }

    private Node createCandle(int seriesIndex, final Data<String, Number> item, int itemIndex) {
        Node candle = item.getNode();
        if (candle instanceof Candle) {
            ((Candle) candle).update(item);
        } else {
            candle = new Candle(seriesIndex, item);
            item.setNode(candle);
        }
        return candle;
    }

    @Override
    protected void layoutPlotChildren() {
        if (getData() == null)
            return;

        for (int seriesIndex = 0; seriesIndex < getData().size(); seriesIndex++) {
            Series<String, Number> series = getData().get(seriesIndex);
            Iterator<Data<String, Number>> iter = getDisplayedDataIterator(series);

            while (iter.hasNext()) {
                Data<String, Number> item = iter.next();
                double x = getXAxis().getDisplayPosition(item.getXValue());
                double y = getYAxis().getDisplayPosition(item.getYValue());

                Node candle = item.getNode();
                if (candle instanceof Candle) {
                    ((Candle) candle).update(item);
                }
            }
        }
        attachListeners();
    }

    private class Candle extends Region {
        private Line highLowLine = new Line();
        private Region body = new Region();

        public Candle(int seriesIndex, Data<String, Number> item) {
            getChildren().addAll(highLowLine, body);
            setMouseTransparent(true); // Allow mouse events to fall through to chart background
            update(item);
        }

        public void update(Data<String, Number> item) {
            CandleData extra = (CandleData) item.getExtraValue();
            if (extra == null)
                return;

            double open = getYAxis().getDisplayPosition(extra.getOpen());
            double close = getYAxis().getDisplayPosition(extra.getClose());
            double high = getYAxis().getDisplayPosition(extra.getHigh());
            double low = getYAxis().getDisplayPosition(extra.getLow());

            // Determine if Bullish or Bearish
            boolean isBullish = extra.getClose().compareTo(extra.getOpen()) >= 0;

            // Apply styles
            String styleClass = isBullish ? "candle-bullish" : "candle-bearish";
            body.getStyleClass().setAll(styleClass, "candle-body");
            highLowLine.getStyleClass().setAll(styleClass, "candle-line");

            // Stick to X position
            double x = getXAxis().getDisplayPosition(item.getXValue());

            // Layout Line
            highLowLine.setStartX(x);
            highLowLine.setEndX(x);
            highLowLine.setStartY(high);
            highLowLine.setEndY(low);

            // Layout Body
            // Layout Body
            double candleWidth = 6; // Fixed width or calculated

            // Check spacing
            if (getXAxis() instanceof Axis) {
                // Try to dynamically scale spacing if needed, but for now fixed width
            }

            body.setLayoutX(x - candleWidth / 2);
            body.setPrefWidth(candleWidth);

            // Y position of body
            if (open > close) {
                // Open is Lower value (higher Y pixels), Close is Higher value (lower Y pixels)
                // WAIT: JavaFX Y-axis: 0 is top.
                // So Higher Price = Lower Pixel value.
                // Standard Cartesian: Y increases upwards.
                // JavaFX Chart: Axis handles mapping.
                // getDisplayPosition(Price): Higher price -> Lower Y value.
            }

            double min = Math.min(open, close);
            double max = Math.max(open, close);
            double height = max - min;
            if (height < 1)
                height = 1; // Ensure visible

            body.setLayoutY(min);
            body.setPrefHeight(height);
        }
    }

    // --- CROSSHAIR & TOOLTIP IMPLEMENTATION ---
    private Line crosshairX;
    private Line crosshairY;
    private javafx.scene.control.Label yAxisLabel;
    private javafx.scene.control.Label xAxisLabel;
    private javafx.scene.control.Label infoLabel; // Top-left OHLC info

    public void enableCrosshair() {
        if (crosshairX != null)
            return; // Already enabled

        crosshairX = new Line();
        crosshairX.getStyleClass().add("crosshair-line");
        crosshairX.setStroke(javafx.scene.paint.Color.web("#ffffff", 0.4));
        crosshairX.getStrokeDashArray().addAll(3d, 3d);
        crosshairX.setMouseTransparent(true);

        crosshairY = new Line();
        crosshairY.getStyleClass().add("crosshair-line");
        crosshairY.setStroke(javafx.scene.paint.Color.web("#ffffff", 0.4));
        crosshairY.getStrokeDashArray().addAll(3d, 3d);
        crosshairY.setMouseTransparent(true);

        yAxisLabel = new javafx.scene.control.Label();
        yAxisLabel.setStyle(
                "-fx-background-color: #1f2937; -fx-text-fill: white; -fx-padding: 2px 4px; -fx-background-radius: 2px; -fx-font-size: 10px;");
        yAxisLabel.setMouseTransparent(true);
        yAxisLabel.setManaged(false);

        xAxisLabel = new javafx.scene.control.Label();
        xAxisLabel.setStyle(
                "-fx-background-color: #1f2937; -fx-text-fill: white; -fx-padding: 2px 4px; -fx-background-radius: 2px; -fx-font-size: 10px;");
        xAxisLabel.setMouseTransparent(true);
        xAxisLabel.setManaged(false);

        infoLabel = new javafx.scene.control.Label(); // Shows O H L C
        infoLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-family: 'Monospaced'; -fx-font-size: 11px;");
        infoLabel.setLayoutX(10);
        infoLabel.setLayoutY(10);
        infoLabel.setMouseTransparent(true);
        infoLabel.setManaged(false);

        getPlotChildren().addAll(crosshairX, crosshairY, yAxisLabel, xAxisLabel, infoLabel);

        // Hide initially
        setCrosshairVisible(false);

        // Note: Listeners are attached in layoutPlotChildren to ensure background node
        // exists
    }

    private void setCrosshairVisible(boolean visible) {
        crosshairX.setVisible(visible);
        crosshairY.setVisible(visible);
        yAxisLabel.setVisible(visible);
        xAxisLabel.setVisible(visible);
        infoLabel.setVisible(visible);
    }

    // Attach listeners to the background so coordinates match plot area
    private boolean listenersAttached = false;

    private void attachListeners() {
        // Ensure nodes are in plot children if cleared
        if (crosshairX != null && !getPlotChildren().contains(crosshairX)) {
            getPlotChildren().addAll(crosshairX, crosshairY, yAxisLabel, xAxisLabel, infoLabel);
        }

        // Z-Order: Bring to front (Always call this to ensure on top of re-drawn
        // candles)
        if (crosshairX != null) {
            crosshairX.toFront();
            crosshairY.toFront();
            yAxisLabel.toFront();
            xAxisLabel.toFront();
            infoLabel.toFront();
        }

        if (listenersAttached)
            return;

        Node background = lookup(".chart-plot-background");
        if (background != null) {
            background.setOnMouseMoved(e -> updateCrosshair(e.getX(), e.getY()));
            background.setOnMouseExited(e -> setCrosshairVisible(false));
            listenersAttached = true;
        }
    }

    private void updateCrosshair(double x, double y) {
        setCrosshairVisible(true);

        // 1. Position Lines
        // Clamp to plot area
        double width = getXAxis().getWidth();
        double height = getYAxis().getHeight();

        if (x < 0 || x > width || y < 0 || y > height) {
            setCrosshairVisible(false);
            return;
        }

        crosshairX.setStartX(0);
        crosshairX.setEndX(width);
        crosshairX.setStartY(y);
        crosshairX.setEndY(y);

        crosshairY.setStartX(x);
        crosshairY.setEndX(x);
        crosshairY.setStartY(0);
        crosshairY.setEndY(height);

        // 2. Y-Axis Label (Price)
        double price = getYAxis().getValueForDisplay(y).doubleValue();
        yAxisLabel.setText(String.format("%.2f", price));
        yAxisLabel.autosize(); // Required since managed=false
        yAxisLabel.setLayoutX(width - yAxisLabel.getWidth() - 2); // Align right
        yAxisLabel.setLayoutY(y - 10);

        // 3. Find Nearest Candle (X-Axis)
        Data<String, Number> nearest = null;
        double minDistance = Double.MAX_VALUE;

        // Iterate visible data to find closest X
        for (Series<String, Number> series : getData()) {
            for (Data<String, Number> data : series.getData()) {
                double dataX = getXAxis().getDisplayPosition(data.getXValue());
                double dist = Math.abs(dataX - x);
                if (dist < minDistance) {
                    minDistance = dist;
                    nearest = data;
                }
            }
        }

        if (nearest != null) {
            // Always snap to nearest candle
            double snapX = getXAxis().getDisplayPosition(nearest.getXValue());
            crosshairY.setStartX(snapX);
            crosshairY.setEndX(snapX);

            xAxisLabel.setText(nearest.getXValue());
            xAxisLabel.autosize(); // Required since managed=false
            xAxisLabel.setLayoutX(snapX - 15);
            xAxisLabel.setLayoutY(height - 15);

            // Update Info Header: O H L C
            CandleData extra = (CandleData) nearest.getExtraValue();
            if (extra != null) {
                double open = extra.getOpen().doubleValue();
                double high = extra.getHigh().doubleValue();
                double low = extra.getLow().doubleValue();
                double close = extra.getClose().doubleValue();

                double change = close - open;
                double pct = (change / open) * 100;
                // Determine color based on trend
                String color = change >= 0 ? "#10b981" : "#ef4444";

                infoLabel.toFront();

                // Dynamic Styling based on Chart Width
                double chartWidth = getXAxis().getWidth();
                boolean isSmall = chartWidth < 600;

                String format = isSmall
                        ? "O:%.2f H:%.2f L:%.2f C:%.2f (%.2f%%)"
                        : "Open: %.2f  High: %.2f  Low: %.2f  Close: %.2f  (%.2f%%)";

                String fontSize = isSmall ? "9px" : "11px";
                String padding = isSmall ? "3px" : "5px";

                infoLabel.setText(String.format(format, open, high, low, close, pct));

                // Add background to ensure visibility, use Dynamic Trend Color
                infoLabel.setStyle(String.format(
                        "-fx-font-family: 'Monospaced'; -fx-font-size: %s; -fx-font-weight: bold; -fx-text-fill: %s; -fx-background-color: rgba(20,20,30,0.85); -fx-padding: %s; -fx-background-radius: 4px; -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1px;",
                        fontSize, color, padding));

                infoLabel.autosize(); // Required since managed=false
                infoLabel.setLayoutX(10);
                infoLabel.setLayoutY(10);
                infoLabel.setVisible(true);
                infoLabel.toFront();
            }
        } else {
            // Not near a candle? Just show time if possible or hide info
            xAxisLabel.setText("");
            infoLabel.setText("");
        }
    }
}
