package com.signomix.reports.pre;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.DataQueryException;
import com.signomix.common.db.Dataset;
import com.signomix.common.db.DatasetHeader;
import com.signomix.common.db.DatasetRow;
import com.signomix.common.db.Report;
import com.signomix.common.db.ReportIface;
import com.signomix.common.db.ReportResult;

import io.agroal.api.AgroalDataSource;

public class DqlTvReport extends Report implements ReportIface {

    private static final Logger logger = Logger.getLogger(StatusReport2.class);

    private static final String DATASET_NAME = "dataset0";
    private static final String QUERY_NAME = "default";

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
        throw new UnsupportedOperationException("Unimplemented method 'getReportHtml'");
    }

    @Override
    public ReportResult getReportResult(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            User user) {

        ReportResult result;
        if (query.getEui() != null) {
            DeviceDto device = getDevice(oltpDs, query.getEui(), user.uid);
            if (device == null) {
                result = new ReportResult();
                result.contentType = "application/json";
                result.error(404, "No device found: " + query.getEui());
                return result;
            }
            result = getDeviceData(olapDs, oltpDs, logsDs, query, user, device);
        } else {
            result = new ReportResult();
            result.contentType = "application/json";
            result.error("No data source specified");
        }
        return result;
    }

    private ReportResult getDeviceData(AgroalDataSource olapDs, AgroalDataSource oltpDs,
            AgroalDataSource logsDs, DataQuery query, User user, DeviceDto device) {

        String reportName = DATASET_NAME;
        ReportResult result = new ReportResult();
        result.setQuery("default", query);
        result.contentType = "application/json";
        result.setId(-1L);
        result.setTitle("");
        result.setDescription("");
        result.setTimestamp(new Timestamp(System.currentTimeMillis()));

        Dataset dataset = new Dataset(query.getEui());
        dataset.name = reportName;
        dataset.eui = query.getEui();
        dataset.size = 0L;

        // get channel names
        String[] requestedChannelNames = {};
        requestedChannelNames = query.getChannelName().split(",");
        if (requestedChannelNames.length != 1) {
            result.error("Only one channel name is supported");
            return result;
        }

        DatasetHeader header = new DatasetHeader(query.getEui());
        for (int i = 0; i < requestedChannelNames.length; i++) {
            header.columns.add(requestedChannelNames[i]);
        }
        result.addDatasetHeader(header);

        // get data

        String sql = getSqlQuery(query, requestedChannelNames[0], user);
        logger.info("SQL query: " + sql);
        try (Connection conn = olapDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, query.getEui());
            int idx = 2;
            if (query.getFromTs() != null) {
                stmt.setTimestamp(idx++, query.getFromTs());
                if (query.getToTs() != null) {
                    stmt.setTimestamp(idx++, query.getToTs());
                }
            }
            if (query.getProject() != null) {
                stmt.setString(idx++, query.getProject());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DatasetRow row = new DatasetRow();
                    row.timestamp = rs.getTimestamp("tstamp").getTime();
                    row.values.add(rs.getString("textvalue"));
                    dataset.data.add(row);
                }
                dataset.size = (long) dataset.data.size();
                result.addDataset(dataset);
            }
        } catch (SQLException ex) {
            logger.error("Error getting data: " + ex.getMessage());
            result.error("Error getting data: " + ex.getMessage());
        }
        // when last X values are requested (unable to get result sorted by database),
        // sort the result if ascending order is requested (descending order is default)
        if (query.isSortingForced()) {
            result = sortResult(result, DATASET_NAME, QUERY_NAME, true);
        }
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
     * @param channelColumnNames
     * @return
     */
    private String getSqlQuery(DataQuery query, String columnName, User user) {

        String sql = "SELECT tstamp, textvalues->'" + query.getChannelName()
                + "' as textvalue FROM analyticdata WHERE eui = ? ";
        if (query.getFromTs() != null) {
            sql += " AND tstamp >= ? ";
            if (query.getToTs() != null) {
                sql += " AND tstamp <= ? ";
            }
        }
        sql += getTimestampCondition(user);

        if (query.getProject() != null) {
            sql += " AND project = ? ";
        }

        sql += "AND textvalues->>'" + columnName + "' IS NOT NULL ";
        sql += " ORDER BY tstamp DESC ";
        sql += " LIMIT 1 ";

        return sql;
    }

    private ReportResult getGroupData(AgroalDataSource olapDs, AgroalDataSource oltpDs,
            AgroalDataSource logsDs, DataQuery query, User user, int defaultLimit) {
        throw new UnsupportedOperationException("Unimplemented method 'getReportHtml'");

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
            DataQuery query, Integer organization, Integer tenant, String path, User user, Boolean withHeader) {
        return super.getAsHtml(getReportResult(olapDs, oltpDs, logsDs, query, organization, tenant, path,
                user), 0, withHeader);
    }

    @Override
    public String getReportHtml(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, User user, Boolean withHeader) {
        return super.getAsHtml(getReportResult(olapDs, oltpDs, logsDs, query, user), 0, withHeader);
    }

    @Override
    public String getReportCsv(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, Integer organization, Integer tenant, String path, User user) {
        throw new UnsupportedOperationException("Unimplemented method 'getReportCsv'");
    }

    @Override
    public String getReportCsv(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, User user) {
        ReportResult result = getReportResult(olapDs, oltpDs, logsDs, query, user);
        return super.getAsCsv(result, 0, "\r\n",
                ",", true);
    }

    @Override
    public String getReportFormat(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs, DataQuery query, User user, String format) {
        // TODO: Implement this method
        throw new UnsupportedOperationException("Unimplemented method 'getReportCsv'");
    }

    @Override
    public String getReportFormat(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs, DataQuery query, Integer organization, Integer tenant, String path, User user, String format) {
        // TODO: Implement this method
        throw new UnsupportedOperationException("Unimplemented method 'getReportCsv'");
    }

}
