package com.signomix.reports.pre;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
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
            DeviceDto device = getDevice(oltpDs, query.getEui(), user.uid);
            if (device == null) {
                result = new ReportResult();
                result.contentType = "application/json";
                result.error(404, "No device found: " + query.getEui());
                return result;
            }
            result = getDeviceData(olapDs, oltpDs, logsDs, query, user, defaultLimit, device);
        } else if (query.getGroup() != null) {
            result = getGroupData(olapDs, oltpDs, logsDs, query, user, defaultLimit);
        } else {
            result = new ReportResult();
            result.contentType = "application/json";
            result.error("No data source specified");
        }
        return result;
    }

    private ReportResult getDeviceData(AgroalDataSource olapDs, AgroalDataSource oltpDs,
            AgroalDataSource logsDs, DataQuery query, User user, int defaultLimit, DeviceDto device) {

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
        String[] channelNames = { "status", "alert" };
        String[] requestedChannelNames = {};
        if (query.getChannelName() == null || query.getChannelName().isEmpty() || query.getChannelName().equals("*")) {
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
        String sql = getSqlQuery(query);
        logger.debug("SQL query: " + sql);
        try (Connection conn = olapDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, query.getEui());
            stmt.setInt(2, query.getLimit() > 0 ? query.getLimit() : defaultLimit);
            try (ResultSet rs = stmt.executeQuery()) {
                double value;
                while (rs.next()) {
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
                            logger.warn("Error getting value: " + ex.getMessage());
                            // probably NaN value
                            row.values.add(null);
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
    private String getSqlQuery(DataQuery query) {

        String sql;
        if (query.getGroup() != null) {
            sql = "SELECT eui, last(status,ts) as status, last(alert,ts) as alert, last(ts,ts) as tstamp FROM devicestatus WHERE eui IN (SELECT eui FROM devices WHERE groups LIKE ?) GROUP BY eui ORDER BY eui";
        } else {
            sql = "SELECT eui, status, alert, ts as tstamp FROM devicestatus WHERE eui = ? ORDER BY ts DESC LIMIT ?";
        }
        return sql;
    }

    private ReportResult getGroupData(AgroalDataSource olapDs, AgroalDataSource oltpDs,
            AgroalDataSource logsDs, DataQuery query, User user, int defaultLimit) {
        ReportResult result = new ReportResult();
        result.setQuery("default", query);
        result.contentType = "application/json";
        result.setId(-1L);
        result.setTitle("");
        result.setDescription("");
        result.setTimestamp(new Timestamp(System.currentTimeMillis()));

        Dataset dataset = null;

        String[] channelNames = { "status", "alert" };
        String[] requestedChannelNames = {};
        if (query.getChannelName() == null || query.getChannelName().isEmpty() || query.getChannelName().equals("*")) {
            requestedChannelNames = channelNames;
        } else {
            requestedChannelNames = query.getChannelName().split(",");
        }

        DatasetHeader header = new DatasetHeader(query.getEui());
        for (int i = 0; i < requestedChannelNames.length; i++) {
            header.columns.add(requestedChannelNames[i]);
        }

        // get data
        String sql = getSqlQuery(query);
        String eui = "";
        String deviceName = "";
        String previousEui = "";
        ReportResult tmpResult = null;
        try (Connection conn = olapDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%,"+query.getGroup()+",%");
            try (ResultSet rs = stmt.executeQuery()) {
                double value;
                while (rs.next()) {
                    eui = rs.getString("eui");
                    if (!eui.equals(previousEui)) {
                        result.addDatasetHeader(header);
                        if (dataset != null && dataset.data.size() > 0) {
                            result.datasets.add(dataset);
                        }
                        deviceName = eui;
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
                            logger.warn("Error getting value: " + ex.getMessage());
                            // probably NaN value
                            row.values.add(null);
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

        /*
         * for (int i = 0; i < devices.size(); i++) {
         * if (devices.get(i) == null) {
         * logger.debug("Skipping null device");
         * continue;
         * }
         * try {
         * tmpQuery = DataQuery.parse(query.getSource());
         * } catch (DataQueryException e) {
         * logger.warn("Error parsing query: " + e.getMessage());
         * result.error("Error parsing query: " + e.getMessage());
         * return result;
         * }
         * tmpResult = new ReportResult();
         * tmpQuery.setEui(devices.get(i).eui);
         * tmpQuery.setGroup(null);
         * tmpResult = getDeviceData(olapDs, oltpDs, logsDs, tmpQuery, user,
         * defaultLimit, devices.get(i));
         * result.headers.add(tmpResult.headers.get(0));
         * dataset = tmpResult.datasets.get(0);
         * if (dataset != null && dataset.size > 0) {
         * result.datasets.add(dataset);
         * } else {
         * logger.warn("No data for device: " + devices.get(i));
         * }
         * 
         * }
         */
        logger.info("result dataset size: " + result.datasets.size());
        return result;
    }

    private DeviceDto getDevice(AgroalDataSource oltpDs, String eui, String userId) {
        DeviceDto device = null;
        String sql = "SELECT eui,name,latitude,longitude,altitude,channels FROM devices WHERE eui = ? AND (userid = ? OR team LIKE ? OR administrators LIKE ?)";
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
                    device.channels = rs.getString("channels");
                }
            }
        } catch (SQLException ex) {
            logger.error("Error getting device: " + ex.getMessage());
        }
        return device;
    }

    private List<DeviceDto> getGroupDevices(String groupEui, AgroalDataSource oltpDs,
            AgroalDataSource logsDs, User user) {
        List<DeviceDto> devices = new ArrayList<>();
        String sql = "SELECT eui,name,latitude,longitude,altitude,configuration,channels FROM devices WHERE groups LIKE ? AND (userid = ? OR team LIKE ? OR administrators LIKE ?)";
        try (Connection conn = oltpDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%," + groupEui + ",%");
            stmt.setString(2, user.uid);
            stmt.setString(3, "%," + user.uid + ",%");
            stmt.setString(4, "%," + user.uid + ",%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DeviceDto device = new DeviceDto();
                    device.eui = rs.getString("eui");
                    device.name = rs.getString("name");
                    device.latitude = rs.getDouble("latitude");
                    device.longitude = rs.getDouble("longitude");
                    device.altitude = rs.getDouble("altitude");
                    device.configuration = deserializeConfiguration(rs.getString("configuration"));
                    device.configuration.put("eui", device.eui);
                    device.configuration.put("name", device.name);
                    device.configuration.put("latitude", String.valueOf(device.latitude));
                    device.configuration.put("longitude", String.valueOf(device.longitude));
                    device.configuration.put("altitude", String.valueOf(device.altitude));
                    device.channels = rs.getString("channels");
                    devices.add(device);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error getting group devices: " + ex.getMessage());
        }
        return devices;
    }

    /**
     * Deserializes device configuration as map of String key-value pairs from JSON
     * string.
     */
    @SuppressWarnings("unchecked")
    private HashMap<String, Object> deserializeConfiguration(String configuration) {
        HashMap<String, Object> config = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            config = mapper.readValue(configuration, HashMap.class);
        } catch (Exception e) {
            logger.error("Error deserializing device configuration: " + e.getMessage());
        }
        return config;
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
        // TODO Auto-generated method stub
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
    public String getReportFormat(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, User user, String format) {
        return null;
    }

    @Override
    public String getReportFormat(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, Integer organization, Integer tenant, String path, User user, String format) {
        // TODO: Implement this method
        return null;
    }

}
