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
import com.signomix.common.db.ReportResult;
import com.signomix.reports.domain.Report;
import com.signomix.reports.domain.ReportIface;

import io.agroal.api.AgroalDataSource;

public class UserLoginReport extends Report implements ReportIface {

    private static final Logger logger = Logger.getLogger(UserLoginReport.class);

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

        return result;

    }

    @Override
    public ReportResult getReportResult(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            User user) {
        String reportName = "dataset0";
        ReportResult result = new ReportResult();
        result.setQuery("default", query);
        result.contentType = "application/json";
        result.setId(-1L);
        result.setTitle("User " + user.uid + " login history report");
        result.setDescription("");
        result.setTimestamp(new Timestamp(System.currentTimeMillis()));
        result.setQuery(reportName, query);

        String dbQuery = "SELECT * FROM account_events WHERE uid=? AND event_type=? AND error_code=0 ";   
        String dbOrder = " ORDER BY ts ";
        String dbLimit = "";
        String dbFromTS = "";
        String dbToTS = "";

        if (query.getFromTs() != null) {
            dbFromTS = " AND ts >= ? ";
        }
        if (query.getToTs() != null) {
            dbToTS = " AND ts <= ? ";
        }
        if (query.getLimit() > 0 && query.getFromTs() == null && query.getToTs() == null) {
            dbLimit = " LIMIT ?";
            dbOrder += " DESC ";
        }

        dbQuery += dbFromTS + dbToTS + dbOrder + dbLimit;
        logger.debug("SQL query: " + dbQuery );
        try (Connection conn = oltpDs.getConnection();
                PreparedStatement ps = conn.prepareStatement(dbQuery)) {
            ps.setString(1, user.uid);
            ps.setObject(2, "login", java.sql.Types.OTHER);
            int paramNo = 3;
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
            if (query.getLimit() > 0 && query.getFromTs() == null && query.getToTs() == null) {
                ps.setLong(paramNo, query.getLimit());
            }
            try (ResultSet rs = ps.executeQuery()) {
                DatasetHeader header = new DatasetHeader(reportName);
                header.columns.add("adres IP");
                header.columns.add("kod błedu");
                result.addDatasetHeader(header);

                Dataset data = new Dataset(reportName);
                data.eui = "";
                data.size = 0L;
                while (rs.next()) {
                    DatasetRow row = new DatasetRow();
                    row.timestamp = rs.getTimestamp("ts").getTime();
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

}
