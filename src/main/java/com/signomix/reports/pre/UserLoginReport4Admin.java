package com.signomix.reports.pre;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.Dataset;
import com.signomix.common.db.DatasetHeader;
import com.signomix.common.db.DatasetRow;
import com.signomix.common.db.Report;
import com.signomix.common.db.ReportIface;
import com.signomix.common.db.ReportResult;

import io.agroal.api.AgroalDataSource;

public class UserLoginReport4Admin extends Report implements ReportIface {

    private static final Logger logger = Logger.getLogger(DqlReport.class);

    @Override
    public ReportResult getReportResult(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            Integer organization,
            Integer tenant,
            String path,
            User user) {
        String reportName = "dataset0";
        ReportResult result = new ReportResult();
        result.setQuery("default", query);
        result.contentType = "application/json";
        result.setId(-1L);
        result.setTitle("User login history report");
        result.setDescription("");
        result.setTimestamp(new Timestamp(System.currentTimeMillis()));
        result.setQuery(reportName, query);

        DatasetHeader header = new DatasetHeader(reportName);
        header.columns.add("login");
        header.columns.add("result_code");
        result.addDatasetHeader(header);

        Dataset data = new Dataset(reportName);
        data.eui = "";
        data.size = 0L;
        result.addDataset(data);

        result = sortResult(result, reportName);
        return result;

    }

    @Override
    public ReportResult getReportResult(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            User user) {

        // Check if user is authorized
        if (user.type != User.OWNER) {
            return new ReportResult().error(403, "Not authorized");
        }
        int defaultLimit = 10;
        String reportName = "dataset0";
        ReportResult result = new ReportResult();
        result.setQuery("default", query);
        result.contentType = "application/json";
        result.setId(-1L);
        result.setTitle("Account events report");
        result.setDescription("");
        result.setTimestamp(new Timestamp(System.currentTimeMillis()));
        result.setQuery(reportName, query);

        String dbQuery = "SELECT * FROM account_events WHERE event_type IS NOT NULL";
        String dbOrder = " ORDER BY ts DESC";
        String dbLimit = "";
        String dbFromTS = "";
        String dbToTS = "";

        if (query.getFromTs() != null) {
            dbFromTS = " AND ts >= ? ";
        }
        if (query.getToTs() != null) {
            dbToTS = " AND ts <= ? ";
        }
        if (query.getFromTs() == null && query.getToTs() == null) {
            dbLimit = " LIMIT ?";
        }

        dbQuery += dbFromTS + dbToTS + dbOrder + dbLimit;
        logger.debug("SQL query: " + dbQuery);
        try (Connection conn = oltpDs.getConnection();
                PreparedStatement ps = conn.prepareStatement(dbQuery)) {
            int paramNo = 1;
            if (query.getFromTs() != null) {
                ps.setTimestamp(paramNo, query.getFromTs());
                logger.debug("FromTS: " + query.getFromTs());
                paramNo++;
            }
            if (query.getToTs() != null) {
                ps.setTimestamp(paramNo, query.getToTs());
                logger.debug("ToTS: " + query.getToTs());
                paramNo++;
            }
            if (query.getFromTs() == null && query.getToTs() == null) {
                if (query.getLimit() > 0) {
                    ps.setLong(paramNo, query.getLimit());
                } else {
                    ps.setLong(paramNo, defaultLimit);
                }
                ps.setLong(paramNo, query.getLimit());
            }
            try (ResultSet rs = ps.executeQuery()) {
                DatasetHeader header = new DatasetHeader(reportName);
                header.columns.add("login");
                header.columns.add("organization");
                header.columns.add("event");
                header.columns.add("IP address");
                header.columns.add("result code");
                result.addDatasetHeader(header);

                Dataset data = new Dataset(reportName);
                data.eui = "";
                data.size = 0L;
                while (rs.next()) {
                    DatasetRow row = new DatasetRow();
                    row.timestamp = rs.getTimestamp("ts").getTime();
                    row.values.add(rs.getString("uid"));
                    row.values.add(rs.getString("organization_id"));
                    row.values.add(rs.getString("event_type"));
                    row.values.add(rs.getString("client_ip"));
                    row.values.add(rs.getString("error_code"));
                    data.data.add(row);
                    data.size++;
                }
                result.addDataset(data);
                rs.close();
            }
        } catch (SQLException e) {
            result.error(e.getMessage());
            return result;
        }

        return result;
    }

    @Override
    public String getReportHtml(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, Integer organization, Integer tenant, String path, User user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReportHtml'");
    }

    @Override
    public String getReportHtml(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, User user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReportHtml'");
    }

    @Override
    public String getReportCsv(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, Integer organization, Integer tenant, String path, User user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReportCsv'");
    }

    @Override
    public String getReportCsv(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, User user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReportCsv'");
    }

}
