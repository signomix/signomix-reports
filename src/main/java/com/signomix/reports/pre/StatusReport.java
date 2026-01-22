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

public class StatusReport extends Report implements ReportIface {

    private static final Logger logger = Logger.getLogger(StatusReport.class);

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
        String reportName = DATASET_NAME;
        ReportResult result = new ReportResult();
        result.setQuery(QUERY_NAME, query);
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
            DeviceDto device = getDevice(oltpDs, query.getEui(), user);
            if (device == null) {
                result = new ReportResult();
                result.contentType = "application/json";
                result.error(404, "No device found: " + query.getEui());
                return result;
            }
            result = getDeviceData(
                    olapDs,
                    oltpDs,
                    logsDs,
                    query,
                    user,
                    defaultLimit,
                    device);
        } else if (query.getGroup() != null) {
            result = getGroupData(
                    olapDs,
                    oltpDs,
                    logsDs,
                    query,
                    user,
                    defaultLimit);
        } else {
            result = new ReportResult();
            result.contentType = "application/json";
            result.error("No data source specified");
        }
        return result;
    }

    private ReportResult getDeviceData(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            User user,
            int defaultLimit,
            DeviceDto device) {
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
        String[] channelNames = { "status", "alert", "name" };
        String[] requestedChannelNames = {};
        if (query.getChannelName() == null ||
                query.getChannelName().isEmpty() ||
                query.getChannelName().equals("*")) {
            requestedChannelNames = channelNames;
        } else {
            requestedChannelNames = query.getChannelName().split(",");
        }

        DatasetHeader header = new DatasetHeader(query.getEui());
        for (int i = 0; i < requestedChannelNames.length; i++) {
            header.columns.add(requestedChannelNames[i]);
        }
        result.addDatasetHeader(header);

        // get data
        boolean intervalOnly = false;
        if (query.getChannelName() != null && query.getChannelName().equalsIgnoreCase("interval")) {
            intervalOnly = true;
        }
        String sql = getSqlQuery(query, intervalOnly);
        logger.debug("SQL query: " + sql);
        try (
                Connection conn = olapDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (intervalOnly) {
                stmt.setString(1, query.getEui());
            } else {
                stmt.setString(1, query.getEui());
                stmt.setInt(
                        2,
                        query.getLimit() > 0 ? query.getLimit() : defaultLimit);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                double value;
                while (rs.next()) {
                    try {
                        dataset.name = rs.getString("name");
                    } catch (Exception e) {
                        logger.warn("Error getting device name: " + e.getMessage());
                        dataset.name = device.name;
                    }
                    DatasetRow row = new DatasetRow();
                    row.timestamp = rs.getTimestamp("tstamp").getTime();
                    for (int i = 0; i < requestedChannelNames.length; i++) {
                        try {
                            value = rs.getDouble(requestedChannelNames[i]);
                            if (rs.wasNull()) {
                                row.values.add(null);
                            } else {
                                row.values.add(value);
                            }
                        } catch (Exception ex) {
                            logger.warn(
                                    "Error getting value: " + ex.getMessage());
                            // probably NaN value
                            // row.values.add(null);
                            try {
                                row.values.add(rs.getString(requestedChannelNames[i]));
                            } catch (Exception e) {
                                logger.warn("Error getting value as string: " + e.getMessage());
                                row.values.add(null);
                            }
                        }
                    }
                    dataset.data.add(row);
                }
                dataset.size = (long) dataset.data.size();
                result.addDataset(dataset);
            }
        } catch (SQLException ex) {
            logger.error("Error getting data: " + ex.getMessage());
            result.error("Error getting data: " + ex.getMessage());
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
    private String getSqlQuery(DataQuery query, boolean intervalOnly) {
        String sql;
        if (query.getGroup() != null && query.getGroup().indexOf(',') < 0) {
            // sql = "SELECT eui, last(status,ts) as status, last(alert,ts) as alert,
            // last(ts,ts) as tstamp FROM devicestatus WHERE eui IN (SELECT eui FROM devices
            // WHERE groups LIKE ?) GROUP BY eui ORDER BY eui";
            sql = """
                    SELECT a.eui, last(a.status, a.ts) as status, last(a.alert, a.ts) as alert,
                    b.name, last(a.ts, a.ts) as tstamp
                    FROM devicestatus as a
                    JOIN devices as b ON a.eui=b.eui
                    WHERE a.eui IN (SELECT eui FROM devices WHERE groups LIKE ?)
                    GROUP BY a.eui,b.eui ORDER BY a.eui
                    """;
        } else if (query.getGroup() != null && query.getGroup().indexOf(',') >= 0) {
            // pseudo group with comma separated EUIs list
            // first comma must be removed
            String[] euiList = query.getGroup().substring(1).split(",");
            StringBuilder inClause = new StringBuilder();
            String eui;
            for (int i = 0; i < euiList.length; i++) {
                // trim and remove possible spaces, single quotes, double quotes
                eui = euiList[i].trim().replace(" ", "").replace("'", "").replace("\"", "");
                if (eui.isEmpty()) {
                    continue;
                }
                inClause.append("'");
                inClause.append(eui);
                inClause.append("'");
                if (i < euiList.length - 1) {
                    inClause.append(",");
                }
            }
            // sql = "SELECT eui, last(status,ts) as status, last(alert,ts) as alert,
            // last(ts,ts) as tstamp FROM devicestatus WHERE eui IN (SELECT eui FROM devices
            // WHERE groups LIKE ?) GROUP BY eui ORDER BY eui";
            sql = """
                    SELECT a.eui, last(a.status, a.ts) as status, last(a.alert, a.ts) as alert,
                    b.name, last(a.ts, a.ts) as tstamp
                    FROM devicestatus as a
                    JOIN devices as b ON a.eui=b.eui
                    WHERE a.eui IN (
                    """
                    + inClause.toString()
                    + """
                    ) GROUP BY a.eui,b.eui ORDER BY a.eui
                    """;
        } else if (intervalOnly) {
            sql = """
                    SELECT eui, ts2 AS tstamp, '' AS name,ROUND(EXTRACT(EPOCH FROM (ts2 - ts1)) *1000)::INTEGER AS interval
                    FROM (
                    SELECT eui, tstamp, tstamp AS ts1, LAG(tstamp) OVER (ORDER BY tstamp DESC) AS ts2
                    FROM analyticdata
                    WHERE eui=? ORDER BY tstamp DESC LIMIT 2
                    ) subquery WHERE ts2 IS NOT NULL
                    """;
        } else {
            // sql = "SELECT eui, status, alert, ts as tstamp FROM devicestatus WHERE eui =
            // ? ORDER BY ts DESC LIMIT ?";
            sql = """
                    SELECT a.eui, a.status, a.alert, b.name, a.ts as tstamp FROM devicestatus as a
                    JOIN devices as b ON a.eui=b.eui
                    WHERE a.eui = ? ORDER BY a.ts DESC LIMIT ?
                    """;
        }
        return sql;
    }

    private ReportResult getGroupData(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            User user,
            int defaultLimit) {
        ReportResult result = new ReportResult();
        result.setQuery("default", query);
        result.contentType = "application/json";
        result.setId(-1L);
        result.setTitle("");
        result.setDescription("");
        result.setTimestamp(new Timestamp(System.currentTimeMillis()));

        Dataset dataset = null;

        String[] channelNames = { "status", "alert", "name" };
        String[] requestedChannelNames = {};
        if (query.getChannelName() == null ||
                query.getChannelName().isEmpty() ||
                query.getChannelName().equals("*")) {
            requestedChannelNames = channelNames;
        } else {
            requestedChannelNames = query.getChannelName().split(",");
        }

        DatasetHeader header = new DatasetHeader(query.getEui());
        for (int i = 0; i < requestedChannelNames.length; i++) {
            header.columns.add(requestedChannelNames[i]);
        }

        // get data
        String sql = getSqlQuery(query, false);
        String eui = "";
        String deviceName = "";
        String previousEui = "";
        ReportResult tmpResult = null;
        try (
                Connection conn = olapDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (query.getGroup() != null && query.getGroup().indexOf(',') < 0) {
                stmt.setString(1, "%," + query.getGroup() + ",%");
            }
            try (ResultSet rs = stmt.executeQuery()) {
                double value;
                while (rs.next()) {
                    eui = rs.getString("eui");
                    if (!eui.equals(previousEui)) {
                        result.addDatasetHeader(header);
                        if (dataset != null && dataset.data.size() > 0) {
                            result.datasets.add(dataset);
                        }
                        deviceName = rs.getString("name");
                        dataset = new Dataset(deviceName); //
                        dataset.eui = eui;
                        previousEui = eui;
                    }

                    DatasetRow row = new DatasetRow();
                    row.timestamp = rs.getTimestamp("tstamp").getTime();
                    for (int i = 0; i < requestedChannelNames.length; i++) {
                        try {
                            value = rs.getDouble(requestedChannelNames[i]);
                            if (rs.wasNull()) {
                                row.values.add(null);
                            } else {
                                row.values.add(value);
                            }
                        } catch (Exception ex) {
                            logger.warn(
                                    "Error getting value: " + ex.getMessage());
                            // probably NaN value
                            try {
                                row.values.add(rs.getString(requestedChannelNames[i]));
                            } catch (Exception e) {
                                logger.warn("Error getting value as string: " + e.getMessage());
                                row.values.add(null);
                            }
                        }
                    }
                    dataset.data.add(row);
                }
                result.addDatasetHeader(header);
                if (dataset != null && dataset.data.size() > 0) {
                    result.datasets.add(dataset);
                }
                // dataset.size = (long) dataset.data.size();
                // result.addDataset(dataset);
            }
        } catch (SQLException ex) {
            logger.error("Error getting data: " + ex.getMessage());
            result.error("Error getting data: " + ex.getMessage());
        }
        logger.info("result dataset size: " + result.datasets.size());
        return result;
    }

    private DeviceDto getDevice(
            AgroalDataSource oltpDs,
            String eui,
            User user) {
        if (eui == null || eui.isEmpty()) {
            return null;
        }
        DeviceDto device = null;

        String sql;
        if (user.organization != null && user.organization > 1) {
            sql = "SELECT eui,name,latitude,longitude,altitude,channels FROM devices WHERE eui = ? AND organization = ?";
        } else {
            sql = "SELECT eui,name,latitude,longitude,altitude,channels FROM devices WHERE eui = ? AND (userid = ? OR team LIKE ? OR administrators LIKE ?)";
        }
        try (
                Connection conn = oltpDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, eui);
            if (user.organization != null && user.organization <= 1) {
                stmt.setString(2, user.uid);
                stmt.setString(3, "%," + user.uid + ",%");
                stmt.setString(4, "%," + user.uid + ",%");
            } else {
                stmt.setLong(2, user.organization);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    device = new DeviceDto();
                    device.eui = rs.getString("eui");
                    device.name = rs.getString("name");
                    device.latitude = rs.getDouble("latitude");
                    device.longitude = rs.getDouble("longitude");
                    device.altitude = rs.getDouble("altitude");
                    device.channels = rs.getString("channels");
                }
            }
        } catch (SQLException ex) {
            logger.error("Error getting device: " + ex.getMessage());
        }
        return device;
    }

    @Override
    public String getReportHtml(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            Integer organization,
            Integer tenant,
            String path,
            User user,
            Boolean withHeader) {
        return super.getAsHtml(
                getReportResult(
                        olapDs,
                        oltpDs,
                        logsDs,
                        query,
                        organization,
                        tenant,
                        path,
                        user),
                0,
                withHeader);
    }

    @Override
    public String getReportHtml(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            User user,
            Boolean withHeader) {
        return super.getAsHtml(
                getReportResult(olapDs, oltpDs, logsDs, query, user),
                0,
                withHeader);
    }

    @Override
    public String getReportCsv(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            Integer organization,
            Integer tenant,
            String path,
            User user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
                "Unimplemented method 'getReportCsv'");
    }

    @Override
    public String getReportCsv(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            User user) {
        ReportResult result = getReportResult(
                olapDs,
                oltpDs,
                logsDs,
                query,
                user);
        return super.getAsCsv(result, 0, "\r\n", ",", true);
    }

    @Override
    public String getReportFormat(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            User user,
            String format) {
        return null;
    }

    @Override
    public String getReportFormat(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            Integer organization,
            Integer tenant,
            String path,
            User user,
            String format) {
        // TODO: Implement this method
        return null;
    }
}
