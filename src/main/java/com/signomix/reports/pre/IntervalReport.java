package com.signomix.reports.pre;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

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

public class IntervalReport extends Report implements ReportIface {

    private static final Logger logger = Logger.getLogger(DqlReport.class);

    private int defaultLimit = 500;
    private static final String DATASET_NAME = "dataset0";

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
        String reportName = DATASET_NAME;
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
        int defaultLimit = 500;
        try {
            defaultLimit = (Integer) options.get("result.limit");
        } catch (Exception e) {
            logger.error("Error getting default limit: " + e.getMessage());
        }

        ReportResult result;
        if (query.getEui() != null) {
            DeviceDto device = getDevice(oltpDs, query.getEui(), user.uid);
            result = getDeviceData(olapDs, oltpDs, logsDs, query, user, defaultLimit, device);
        } else {
            result = new ReportResult();
            result.contentType = "application/json";
            result.error("No data source specified");
        }
        return result;
    }

    private ReportResult getDeviceData(AgroalDataSource olapDs, AgroalDataSource oltpDs,
            AgroalDataSource logsDs, DataQuery query, User user, int defaultLimit, DeviceDto device) {

        ReportResult result = new ReportResult();
        result.setQuery("default", query);
        result.contentType = "application/json";
        result.setId(-1L);
        result.setTitle("");
        result.setDescription("");
        result.setTimestamp(new Timestamp(System.currentTimeMillis()));

        Dataset dataset = new Dataset(query.getEui());
        dataset.eui = query.getEui();
        dataset.size = 0L;
        DatasetHeader header = new DatasetHeader(query.getEui());
        header.columns.add("delta");
        result.addDatasetHeader(header);

        // get data
        String sql1 = getSqlQuery(168);
        String sql0 = getSqlQuery(169);
        ArrayList<DatasetRow> rows1 = new ArrayList<>();
        ArrayList<DatasetRow> rows0 = new ArrayList<>(); // 1 row more to calculate delta
        logger.info("SQL1 query: " + sql1);

        try (Connection conn = olapDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql1)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DatasetRow row = new DatasetRow();
                    row.timestamp = rs.getTimestamp("ts").getTime();
                    row.values.add(rs.getDouble("d1"));
                    rows1.add(row);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error getting data: " + ex.getMessage());
            result.error("Error getting data: " + ex.getMessage());
        }
        try (Connection conn = olapDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql0)) {
            try (ResultSet rs = stmt.executeQuery()) {
                double value;
                while (rs.next()) {
                    DatasetRow row = new DatasetRow();
                    row.timestamp = rs.getTimestamp("ts").getTime();
                    row.values.add(rs.getDouble("d1"));
                    rows0.add(row);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error getting data: " + ex.getMessage());
            result.error("Error getting data: " + ex.getMessage());
        }
        // calculate delta
        double delta = 0;
        for (int i = 0; i < rows1.size(); i++) {
            if (i < rows0.size() - 1) {
                delta = (double) rows1.get(i).values.get(0) - (double) rows0.get(i + 1).values.get(0);
            } else {
                delta = 0;
            }
            DatasetRow row = new DatasetRow();
            row.timestamp = rows1.get(i).timestamp;
            row.values.add(delta);
            dataset.data.add(row);
        }
        dataset.size = (long) dataset.data.size();
        result.addDataset(dataset);
        return result;
    }

    /**
     * Creates a SQL query to get data from the database.
     * The query is created based on the given DataQuery object.
     * Supported query parameters:
     * - eui
     * - group
     * - channel (from channelColumnNames parameter, not DQL channel keyword )
     * - last
     * - from
     * - to
     * - project
     * - notnull
     * - ascending
     * - descending
     * Not supported:
     * - sback
     * - minimum
     * - maximum
     * - average
     * - sum
     * - new
     * - state
     * - tag
     * - virtual (won't be supported by this type of report)
     * - sort
     * 
     * @param query
     * @param useDefaultLimit
     * @param channelColumnNames
     * @return
     */
    private String getSqlQuery(int hours) {

        String sql = "SELECT time_bucket('1 hour', tstamp) AS ts," + //
                "  last(d1, tstamp) as d1 " + //
                "FROM analyticdata " + //
                "WHERE eui='0018B240000068D4' AND tstamp > now () - INTERVAL '" + hours + " hours' " + //
                "GROUP BY eui, ts " + //
                "ORDER BY ts DESC;";

        return sql;
    }

    private DeviceDto getDevice(AgroalDataSource oltpDs, String eui, String userId) {
        DeviceDto device = null;
        String sql = "SELECT eui,name,latitude,longitude,altitude FROM devices WHERE eui = ? AND (userid = ? OR team LIKE ? OR administrators LIKE ?)";
        try (Connection conn = oltpDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, eui);
            stmt.setString(2, userId);
            stmt.setString(3, "%," + userId + ",%");
            stmt.setString(4, "%," + userId + ",%");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    device = new DeviceDto();
                    device.eui = rs.getString("eui");
                    device.name = rs.getString("name");
                    device.latitude = rs.getDouble("latitude");
                    device.longitude = rs.getDouble("longitude");
                    device.altitude = rs.getDouble("altitude");
                }
            }
        } catch (SQLException ex) {
            logger.error("Error getting device: " + ex.getMessage());
        }
        return device;
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
