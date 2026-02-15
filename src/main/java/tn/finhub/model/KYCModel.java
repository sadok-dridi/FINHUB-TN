package tn.finhub.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.finhub.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KYCModel {

    private static final Logger logger = LoggerFactory.getLogger(KYCModel.class);

    private Connection getConnection() {
        return DBConnection.getInstance();
    }

    public void createRequest(KYCRequest request) {
        String sql = "INSERT INTO kyc_requests (user_id, document_type, document_url, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, request.getUserId());
            ps.setString(2, request.getDocumentType());
            ps.setString(3, request.getDocumentUrl());
            ps.setString(4, "PENDING");
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error creating KYC request", e);
        }
    }

    public List<KYCRequest> findPendingRequests() {
        List<KYCRequest> requests = new ArrayList<>();
        String sql = """
                    SELECT k.*, u.email
                    FROM kyc_requests k
                    JOIN users_local u ON k.user_id = u.user_id
                    WHERE k.status = 'PENDING'
                    ORDER BY k.submission_date ASC
                """;

        try (Statement st = getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                KYCRequest req = mapResultSetToRequest(rs);
                req.setUserEmail(rs.getString("email"));
                requests.add(req);
            }
        } catch (SQLException e) {
            logger.error("Error fetching pending KYC requests", e);
        }
        return requests;
    }

    public List<KYCRequest> findAllRequests() {
        List<KYCRequest> requests = new ArrayList<>();
        String sql = """
                    SELECT k.*, u.email
                    FROM kyc_requests k
                    JOIN users_local u ON k.user_id = u.user_id
                    ORDER BY CASE WHEN k.status = 'PENDING' THEN 1 ELSE 2 END, k.submission_date DESC
                """;

        try (Statement st = getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                KYCRequest req = mapResultSetToRequest(rs);
                req.setUserEmail(rs.getString("email"));
                requests.add(req);
            }
        } catch (SQLException e) {
            logger.error("Error fetching all KYC requests", e);
        }
        return requests;
    }

    public List<KYCRequest> findByUserId(int userId) {
        List<KYCRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM kyc_requests WHERE user_id = ? ORDER BY submission_date DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapResultSetToRequest(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching user KYC requests", e);
        }
        return requests;
    }

    public void updateStatus(int requestId, String status) {
        String sql = "UPDATE kyc_requests SET status = ? WHERE request_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating KYC request status", e);
        }
    }

    private KYCRequest mapResultSetToRequest(ResultSet rs) throws SQLException {
        return new KYCRequest(
                rs.getInt("request_id"),
                rs.getInt("user_id"),
                rs.getString("document_type"),
                rs.getString("document_url"),
                rs.getString("status"),
                rs.getTimestamp("submission_date"));
    }
}
