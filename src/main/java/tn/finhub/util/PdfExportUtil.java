package tn.finhub.util;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PdfExportUtil {

    public static void generateStatisticsReport(File destFile, String totalUsers, String adminCount,
            String avgTrust, String lowTrustAlerts,
            WritableImage roleChartSnapshot,
            WritableImage trustChartSnapshot) throws Exception {

        // Initialize PDF writer and document
        PdfWriter writer = new PdfWriter(destFile);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Brand Colors
        DeviceRgb primaryColor = new DeviceRgb(56, 189, 248); // #38bdf8
        DeviceRgb headerBgColor = new DeviceRgb(30, 27, 46); // #1e1b2e

        // 1. Header
        Paragraph title = new Paragraph("FINHUB TN")
                .setFontSize(24)
                .setBold()
                .setFontColor(primaryColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(0);

        Paragraph subtitle = new Paragraph("Advanced User Statistics Report")
                .setFontSize(16)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);

        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Paragraph date = new Paragraph("Generated on: " + dateStr)
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30);

        document.add(title);
        document.add(subtitle);
        document.add(date);

        // 2. KPI Summary Table
        Table kpiTable = new Table(UnitValue.createPercentArray(new float[] { 25, 25, 25, 25 }))
                .useAllAvailableWidth()
                .setMarginBottom(30);

        // Header Row
        String[] headers = { "Total Users", "Administrators", "Avg Trust Score", "Low Trust Alerts" };
        for (String h : headers) {
            Cell cell = new Cell()
                    .add(new Paragraph(h).setBold().setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(headerBgColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(10)
                    .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1));
            kpiTable.addHeaderCell(cell);
        }

        // Data Row
        String[] data = { totalUsers, adminCount, avgTrust, lowTrustAlerts };
        for (String d : data) {
            Cell cell = new Cell()
                    .add(new Paragraph(d).setFontSize(14).setBold())
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(15)
                    .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1));
            kpiTable.addCell(cell);
        }

        document.add(kpiTable);

        // 3. Charts Section
        Paragraph chartsHeader = new Paragraph("Visual Analytics")
                .setFontSize(16)
                .setBold()
                .setFontColor(headerBgColor)
                .setMarginBottom(15);
        document.add(chartsHeader);

        // Embed Role Pie Chart
        if (roleChartSnapshot != null) {
            document.add(new Paragraph("Role Distribution").setBold().setMarginBottom(5));
            Image roleImage = new Image(convertImageToImageData(roleChartSnapshot));
            roleImage.scaleToFit(400, 300);
            roleImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            document.add(roleImage);
        }

        // Add some spacing
        document.add(new Paragraph("\n"));

        // Embed Trust Score Bar Chart
        if (trustChartSnapshot != null) {
            document.add(new Paragraph("Trust Score Distribution").setBold().setMarginBottom(5));
            Image trustImage = new Image(convertImageToImageData(trustChartSnapshot));
            trustImage.scaleToFit(500, 350);
            trustImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            document.add(trustImage);
        }

        // Close Document
        document.close();
    }

    private static ImageData convertImageToImageData(WritableImage fxImage) throws Exception {
        java.awt.image.BufferedImage bImage = SwingFXUtils.fromFXImage(fxImage, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bImage, "png", baos);
        return ImageDataFactory.create(baos.toByteArray());
    }
}
