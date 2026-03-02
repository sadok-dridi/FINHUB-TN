package tn.finhub.util;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
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
import java.io.FileOutputStream;
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

        public static void generateUserDetailsReport(File destFile, String userName, String email, String role,
                        String id,
                        String balance, String currency, String status, WritableImage sparklineSnapshot)
                        throws Exception {

                PdfWriter writer = new PdfWriter(destFile);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);

                DeviceRgb primaryColor = new DeviceRgb(56, 189, 248);
                DeviceRgb headerBgColor = new DeviceRgb(30, 27, 46);

                // Header
                document.add(new Paragraph("FINHUB TN").setFontSize(24).setBold().setFontColor(primaryColor)
                                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(0));
                document.add(new Paragraph("User Details Overview").setFontSize(16).setBold()
                                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));

                String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                document.add(new Paragraph("Generated on: " + dateStr).setFontSize(10).setFontColor(ColorConstants.GRAY)
                                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(30));

                // Personal Information
                document.add(new Paragraph("Personal Information").setFontSize(14).setBold().setFontColor(headerBgColor)
                                .setMarginBottom(10));
                Table infoTable = new Table(UnitValue.createPercentArray(new float[] { 30, 70 })).useAllAvailableWidth()
                                .setMarginBottom(20);
                infoTable.addCell(new Cell().add(new Paragraph("Full Name:").setBold()).setBorder(Border.NO_BORDER)
                                .setPadding(5));
                infoTable.addCell(new Cell().add(new Paragraph(userName)).setBorder(Border.NO_BORDER).setPadding(5));
                infoTable.addCell(new Cell().add(new Paragraph("Email:").setBold()).setBorder(Border.NO_BORDER)
                                .setPadding(5));
                infoTable.addCell(new Cell().add(new Paragraph(email)).setBorder(Border.NO_BORDER).setPadding(5));
                infoTable.addCell(new Cell().add(new Paragraph("Role:").setBold()).setBorder(Border.NO_BORDER)
                                .setPadding(5));
                infoTable.addCell(new Cell().add(new Paragraph(role)).setBorder(Border.NO_BORDER).setPadding(5));
                infoTable.addCell(new Cell().add(new Paragraph("ID:").setBold()).setBorder(Border.NO_BORDER)
                                .setPadding(5));
                infoTable.addCell(new Cell().add(new Paragraph(id)).setBorder(Border.NO_BORDER).setPadding(5));
                document.add(infoTable);

                // Wallet Status
                document.add(new Paragraph("Wallet Status").setFontSize(14).setBold().setFontColor(headerBgColor)
                                .setMarginBottom(10));
                Table walletTable = new Table(UnitValue.createPercentArray(new float[] { 30, 70 }))
                                .useAllAvailableWidth().setMarginBottom(20);
                walletTable.addCell(new Cell().add(new Paragraph("Balance:").setBold()).setBorder(Border.NO_BORDER)
                                .setPadding(5));
                walletTable.addCell(new Cell().add(new Paragraph(balance + " " + currency).setFontSize(16).setBold()
                                .setFontColor(primaryColor)).setBorder(Border.NO_BORDER).setPadding(5));
                walletTable.addCell(new Cell().add(new Paragraph("Status:").setBold()).setBorder(Border.NO_BORDER)
                                .setPadding(5));
                walletTable.addCell(new Cell()
                                .add(new Paragraph(status).setBold().setFontColor(
                                                status.equals("FROZEN") ? ColorConstants.RED : ColorConstants.GREEN))
                                .setBorder(Border.NO_BORDER).setPadding(5));
                document.add(walletTable);

                // Sparkline Snapshot
                if (sparklineSnapshot != null) {
                        document.add(new Paragraph("Recent Activity Trend").setFontSize(14).setBold()
                                        .setFontColor(headerBgColor).setMarginBottom(10));
                        Image sparkImage = new Image(convertImageToImageData(sparklineSnapshot));
                        sparkImage.scaleToFit(500, 150);
                        sparkImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                        document.add(sparkImage);
                }

                document.close();
        }

        public static void generateUserWalletStatisticsReport(File destFile, String userName, String totalSent,
                        String totalReceived, String txCount,
                        WritableImage flowChartSnapshot, WritableImage typeChartSnapshot) throws Exception {

                PdfWriter writer = new PdfWriter(destFile);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);

                DeviceRgb primaryColor = new DeviceRgb(56, 189, 248);
                DeviceRgb headerBgColor = new DeviceRgb(30, 27, 46);

                // Header
                document.add(new Paragraph("FINHUB TN").setFontSize(24).setBold().setFontColor(primaryColor)
                                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(0));
                document.add(new Paragraph("Advanced Wallet Statistics: " + userName).setFontSize(16).setBold()
                                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));

                String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                document.add(new Paragraph("Generated on: " + dateStr).setFontSize(10).setFontColor(ColorConstants.GRAY)
                                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(30));

                // KPI Summary Table
                Table kpiTable = new Table(UnitValue.createPercentArray(new float[] { 33, 33, 34 }))
                                .useAllAvailableWidth().setMarginBottom(30);

                String[] headers = { "Total Sent", "Total Received", "Total Transactions" };
                for (String h : headers) {
                        kpiTable.addHeaderCell(
                                        new Cell().add(new Paragraph(h).setBold().setFontColor(ColorConstants.WHITE))
                                                        .setBackgroundColor(headerBgColor)
                                                        .setTextAlignment(TextAlignment.CENTER).setPadding(10));
                }

                String[] data = { totalSent, totalReceived, txCount };
                for (String d : data) {
                        kpiTable.addCell(new Cell().add(new Paragraph(d).setFontSize(14).setBold())
                                        .setTextAlignment(TextAlignment.CENTER).setPadding(15));
                }
                document.add(kpiTable);

                // Charts
                document.add(new Paragraph("Visual Analytics").setFontSize(16).setBold().setFontColor(headerBgColor)
                                .setMarginBottom(15));

                if (flowChartSnapshot != null) {
                        document.add(new Paragraph("Income vs. Expenses Flow").setBold().setMarginBottom(5));
                        Image flowImage = new Image(convertImageToImageData(flowChartSnapshot));
                        flowImage.scaleToFit(400, 300);
                        flowImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                        document.add(flowImage);
                        document.add(new Paragraph("\n"));
                }

                if (typeChartSnapshot != null) {
                        document.add(new Paragraph("Transaction Types Breakdown").setBold().setMarginBottom(5));
                        Image typeImage = new Image(convertImageToImageData(typeChartSnapshot));
                        typeImage.scaleToFit(500, 350);
                        typeImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                        document.add(typeImage);
                }

                document.close();
        }

        public static void generateSupportStatisticsReport(java.io.File destFile, String totalTickets,
                        String openTickets,
                        String resolvedTickets, javafx.scene.image.WritableImage statusChartSnapshot,
                        javafx.scene.image.WritableImage categoryChartSnapshot) throws Exception {

                PdfWriter writer = new PdfWriter(new FileOutputStream(destFile));
                PdfDocument pdfDocument = new PdfDocument(writer);
                Document document = new Document(pdfDocument);

                // Colors
                Color primaryColor = new DeviceRgb(26, 35, 126); // Indigo 900
                Color headerBgColor = new DeviceRgb(33, 150, 243); // Blue 500

                // Title
                document.add(new Paragraph("FINHUB TN").setFontSize(24).setBold().setFontColor(primaryColor)
                                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(0));
                document.add(new Paragraph("Global Support Dashboard Report").setFontSize(16).setBold()
                                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));

                String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                document.add(new Paragraph("Generated on: " + dateStr).setFontSize(10).setFontColor(ColorConstants.GRAY)
                                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(30));

                // KPI Table
                document.add(new Paragraph("Helpdesk Metrics").setFontSize(14).setBold().setFontColor(headerBgColor)
                                .setMarginBottom(10));

                Table kpiTable = new Table(UnitValue.createPercentArray(new float[] { 33, 33, 34 }))
                                .useAllAvailableWidth().setMarginBottom(20);

                // Headers
                kpiTable.addHeaderCell(new Cell()
                                .add(new Paragraph("Total Tickets").setBold().setTextAlignment(TextAlignment.CENTER))
                                .setBackgroundColor(ColorConstants.LIGHT_GRAY));
                kpiTable.addHeaderCell(new Cell()
                                .add(new Paragraph("Open Tickets").setBold().setTextAlignment(TextAlignment.CENTER))
                                .setBackgroundColor(ColorConstants.LIGHT_GRAY));
                kpiTable.addHeaderCell(new Cell()
                                .add(new Paragraph("Resolved Tickets").setBold().setTextAlignment(TextAlignment.CENTER))
                                .setBackgroundColor(ColorConstants.LIGHT_GRAY));

                // Values
                kpiTable.addCell(new Cell().add(new Paragraph(totalTickets).setFontSize(18).setBold()
                                .setTextAlignment(TextAlignment.CENTER)));
                kpiTable.addCell(new Cell().add(new Paragraph(openTickets).setFontSize(18).setBold()
                                .setFontColor(new DeviceRgb(239, 68, 68)).setTextAlignment(TextAlignment.CENTER)));
                kpiTable.addCell(new Cell().add(new Paragraph(resolvedTickets).setFontSize(18).setBold()
                                .setFontColor(new DeviceRgb(16, 185, 129)).setTextAlignment(TextAlignment.CENTER)));

                document.add(kpiTable);

                // Charts
                document.add(new Paragraph("Visual Analytics").setFontSize(16).setBold().setFontColor(headerBgColor)
                                .setMarginBottom(15));

                if (statusChartSnapshot != null) {
                        document.add(new Paragraph("Resolution Status Flow").setBold().setMarginBottom(5));
                        Image statusImage = new Image(convertImageToImageData(statusChartSnapshot));
                        statusImage.scaleToFit(400, 300);
                        statusImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                        document.add(statusImage);
                        document.add(new Paragraph("\n"));
                }

                if (categoryChartSnapshot != null) {
                        document.add(new Paragraph("Tickets Categorization Breakdown").setBold().setMarginBottom(5));
                        Image catImage = new Image(convertImageToImageData(categoryChartSnapshot));
                        catImage.scaleToFit(500, 350);
                        catImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                        document.add(catImage);
                }

                document.close();
        }

        public static void generateEscrowStatisticsReport(java.io.File destFile, String totalEscrows,
                        String lockedEscrows,
                        String disputedEscrows, String completedEscrows,
                        javafx.scene.image.WritableImage statusChartSnapshot,
                        javafx.scene.image.WritableImage typeChartSnapshot) throws Exception {

                PdfWriter writer = new PdfWriter(new FileOutputStream(destFile));
                PdfDocument pdfDocument = new PdfDocument(writer);
                Document document = new Document(pdfDocument);

                // Colors
                Color primaryColor = new DeviceRgb(26, 35, 126); // Indigo 900
                Color headerBgColor = new DeviceRgb(33, 150, 243); // Blue 500

                // Title
                document.add(new Paragraph("FINHUB TN").setFontSize(24).setBold().setFontColor(primaryColor)
                                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(0));
                document.add(new Paragraph("Global Escrow Dashboard Report").setFontSize(16).setBold()
                                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));

                String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                document.add(new Paragraph("Generated on: " + dateStr).setFontSize(10).setFontColor(ColorConstants.GRAY)
                                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(30));

                // KPI Table
                document.add(new Paragraph("Escrow Metrics").setFontSize(14).setBold().setFontColor(headerBgColor)
                                .setMarginBottom(10));

                Table kpiTable = new Table(UnitValue.createPercentArray(new float[] { 25, 25, 25, 25 }))
                                .useAllAvailableWidth().setMarginBottom(20);

                // Headers
                kpiTable.addHeaderCell(new Cell()
                                .add(new Paragraph("Total Escrows").setBold().setTextAlignment(TextAlignment.CENTER))
                                .setBackgroundColor(ColorConstants.LIGHT_GRAY));
                kpiTable.addHeaderCell(new Cell()
                                .add(new Paragraph("Active Locked").setBold().setTextAlignment(TextAlignment.CENTER))
                                .setBackgroundColor(ColorConstants.LIGHT_GRAY));
                kpiTable.addHeaderCell(new Cell()
                                .add(new Paragraph("Disputed").setBold().setTextAlignment(TextAlignment.CENTER))
                                .setBackgroundColor(ColorConstants.LIGHT_GRAY));
                kpiTable.addHeaderCell(new Cell()
                                .add(new Paragraph("Completed").setBold().setTextAlignment(TextAlignment.CENTER))
                                .setBackgroundColor(ColorConstants.LIGHT_GRAY));

                // Values
                kpiTable.addCell(new Cell().add(new Paragraph(totalEscrows).setFontSize(18).setBold()
                                .setTextAlignment(TextAlignment.CENTER)));
                kpiTable.addCell(new Cell().add(new Paragraph(lockedEscrows).setFontSize(18).setBold()
                                .setFontColor(new DeviceRgb(59, 130, 246)).setTextAlignment(TextAlignment.CENTER))); // Blue
                kpiTable.addCell(new Cell().add(new Paragraph(disputedEscrows).setFontSize(18).setBold()
                                .setFontColor(new DeviceRgb(239, 68, 68)).setTextAlignment(TextAlignment.CENTER))); // Red
                kpiTable.addCell(new Cell().add(new Paragraph(completedEscrows).setFontSize(18).setBold()
                                .setFontColor(new DeviceRgb(16, 185, 129)).setTextAlignment(TextAlignment.CENTER))); // Green

                document.add(kpiTable);

                // Charts
                document.add(new Paragraph("Visual Analytics").setFontSize(16).setBold().setFontColor(headerBgColor)
                                .setMarginBottom(15));

                if (statusChartSnapshot != null) {
                        document.add(new Paragraph("Escrow Status Flow").setBold().setMarginBottom(5));
                        Image statusImage = new Image(convertImageToImageData(statusChartSnapshot));
                        statusImage.scaleToFit(400, 300);
                        statusImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                        document.add(statusImage);
                        document.add(new Paragraph("\n"));
                }

                if (typeChartSnapshot != null) {
                        document.add(new Paragraph("Transaction Types Breakdown").setBold().setMarginBottom(5));
                        Image catImage = new Image(convertImageToImageData(typeChartSnapshot));
                        catImage.scaleToFit(500, 350);
                        catImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                        document.add(catImage);
                }

                document.close();
        }

        private static ImageData convertImageToImageData(WritableImage fxImage) throws Exception {
                java.awt.image.BufferedImage bImage = SwingFXUtils.fromFXImage(fxImage, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bImage, "png", baos);
                return ImageDataFactory.create(baos.toByteArray());
        }
}
