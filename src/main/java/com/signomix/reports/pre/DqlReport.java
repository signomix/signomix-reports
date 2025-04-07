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

public class DqlReport extends Report implements ReportIface {

    private static final Logger logger = Logger.getLogger(DqlReport.class);

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
        /*
         * String reportName = DATASET_NAME;
         * ReportResult result = new ReportResult();
         * result.setQuery("default", query);
         * result.contentType = "application/json";
         * result.setId(-1L);
         * result.setTitle("");
         * result.setDescription("");
         * result.setTimestamp(new Timestamp(System.currentTimeMillis()));
         * result.setQuery(reportName, query);
         */

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
        // result.setQuery(reportName, query);

        /*
         * String devEui = getDevice(oltpDs, query.getEui(), user.uid);
         * if (devEui == null) {
         * result.error("No device found: " + query.getEui());
         * return result;
         * }
         */
        Dataset dataset = new Dataset(query.getEui());
        dataset.name = reportName;
        dataset.eui = query.getEui();
        dataset.size = 0L;

        // get channel names
        HashMap<String, String> channelColumnNames = new HashMap<>();
        String[] channelNames = {};
        String[] requestedChannelNames = {};
        HashSet<String> channelNamesSet = new HashSet<>();
        if (query.getChannelName() == null) {
            result.error("No channel name specified");
            return result;
        }
        if (query.getChannelName().equals("*")) {
            requestedChannelNames = channelNames;
        } else {
            requestedChannelNames = query.getChannelName().split(",");
        }
        for (String channel : requestedChannelNames) {
            channelNamesSet.add(channel);
        }
        HashSet<String> deviceChannelNamesSet = new HashSet<>();
        String sql = "SELECT channels FROM devicechannels WHERE eui = ?";
        try (Connection conn = oltpDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, query.getEui());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String channels = rs.getString(1);
                    if (channels != null) {
                        channelNames = channels.split(","); // channels names declared in the device

                        for (String channel : channelNames) {
                            deviceChannelNamesSet.add(channel);
                        }
                        for (int i = 0; i < channelNames.length; i++) {
                            if (channelNamesSet.contains(channelNames[i])) {
                                channelColumnNames.put(channelNames[i], "d" + (i + 1));
                            }
                        }
                    }
                    // device coordinates are added to the dataset if not already present
                    // (latitude, longitude, altitude)
                    // those channels columns won't be named as d1, d2, d3, etc. but as latitude,
                    // longitude, altitude
                    if (channelNamesSet.contains("latitude")) {
                        if (!deviceChannelNamesSet.contains("latitude")) {
                            channelColumnNames.put("latitude", "latitude");
                        }
                    }
                    if (channelNamesSet.contains("longitude")) {
                        if (!deviceChannelNamesSet.contains("longitude")) {
                            channelColumnNames.put("longitude", "longitude");
                        }
                    }
                    if (channelNamesSet.contains("altitude")) {
                        if (!deviceChannelNamesSet.contains("altitude")) {
                            channelColumnNames.put("altitude", "altitude");
                        }
                    }
                } else {
                    result.error("No channel definition found for device " + query.getEui());
                    return result;
                }
            }
        } catch (SQLException ex) {
            logger.error("Error getting channel names: " + ex.getMessage());
            result.error("Error getting channel names: " + ex.getMessage());
        }

        DatasetHeader header = new DatasetHeader(query.getEui());
        for (int i = 0; i < requestedChannelNames.length; i++) {
            header.columns.add(requestedChannelNames[i]);
        }
        result.addDatasetHeader(header);

        // get data
        sql = getSqlQuery(query, channelColumnNames, user);
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
            stmt.setInt(idx++, query.getLimit());
            
            try (ResultSet rs = stmt.executeQuery()) {
                double value;
                boolean noNulls;
                String columnName;
                while (rs.next()) {
                    DatasetRow row = new DatasetRow();
                    row.timestamp = rs.getTimestamp("tstamp").getTime();
                    noNulls = true;
                    for (int i = 0; i < requestedChannelNames.length; i++) {
                        try {
                            columnName = channelColumnNames.get(requestedChannelNames[i]);
                            // if the column name starts with "d", it is a data channel
                            // otherwise it is a device configuration parameter
                            if (columnName.startsWith("d")) {
                                value = rs.getDouble(columnName);
                                // value = rs.getDouble(channelColumnNames.get(requestedChannelNames[i]));
                                if (rs.wasNull()) {
                                    row.values.add(null);
                                    noNulls = false;
                                } else {
                                    row.values.add(value);
                                }
                            } else {
                                // TODO: get device configuration parameters
                                switch (columnName) {
                                    case "latitude":
                                        row.values.add(device.latitude);
                                        break;
                                    case "longitude":
                                        row.values.add(device.longitude);
                                        break;
                                    case "altitude":
                                        row.values.add(device.altitude);
                                        break;
                                    default:
                                        logger.warn("Unknown column name: " + columnName);
                                        row.values.add(0d);
                                }
                            }
                        } catch (Exception ex) {
                            logger.warn("Error getting value: " + ex.getMessage());
                            // probably NaN value
                            row.values.add(null);
                            noNulls = false;
                        }
                    }

                    // the query already filters out nulls
                    // but in case of NaN values, the row is skipped
                    if (query.isNotNull() && !noNulls) {
                        continue;
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
    private String getSqlQuery(DataQuery query, HashMap<String, String> channelColumnNames, User user) {

        String columnName;
        String columns = "tstamp,";
        for (String channel : channelColumnNames.keySet()) {
            columnName = channelColumnNames.get(channel);
            if (columnName.startsWith("d")) {
                columns += columnName + ",";
            }
        }
        columns = columns.substring(0, columns.length() - 1);

        String notNullCondition;

        if (query.isNotNull()) {
            notNullCondition = " AND NOT (";
            for (String channel : channelColumnNames.keySet()) {
                columnName = channelColumnNames.get(channel);
                if (columnName.startsWith("d")) {
                    notNullCondition += channelColumnNames.get(channel) + " IS NULL OR ";
                }
            }
            notNullCondition = notNullCondition.substring(0, notNullCondition.length() - 4);
            notNullCondition += ") ";
        } else {
            notNullCondition = "";
        }
        String sql = "SELECT " + columns + " FROM analyticdata WHERE eui = ? ";
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

        sql += notNullCondition;

        if (query.isSortingForced()) {
            sql += " ORDER BY tstamp DESC ";
        } else {
            sql += " ORDER BY tstamp " + query.getSortOrder() + " ";
        }

        sql += " LIMIT ? ";

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
        List<DeviceDto> devices = getGroupDevices(query.getGroup(), oltpDs, logsDs, user);
        if (devices.isEmpty()) {
            result.error("No devices found in group " + query.getGroup());
            return result;
        } else {
            devices.forEach(device -> {
                result.configs.put(device.eui,device.configuration);
                logger.info("Group device: " + device);
            });
            
        }
        ReportResult tmpResult;
        Dataset dataset;
        DataQuery tmpQuery;
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i) == null) {
                logger.debug("Skipping null device");
                continue;
            }
            try {
                tmpQuery = DataQuery.parse(query.getSource());
            } catch (DataQueryException e) {
                logger.warn("Error parsing query: " + e.getMessage());
                result.error("Error parsing query: " + e.getMessage());
                return result;
            }
            tmpResult = new ReportResult();
            tmpQuery.setEui(devices.get(i).eui);
            tmpQuery.setGroup(null);
            tmpResult = getDeviceData(olapDs, oltpDs, logsDs, tmpQuery, user, defaultLimit, devices.get(i));
            result.headers.add(tmpResult.headers.get(0));
            dataset = tmpResult.datasets.get(0);
            if (dataset != null && dataset.size > 0) {
                result.datasets.add(dataset);
            } else {
                logger.warn("No data for device: " + devices.get(i));
            }

        }
        logger.info("result dataset size: " + result.datasets.size());
        return result;
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

    private List<DeviceDto> getGroupDevices(String groupEui, AgroalDataSource oltpDs,
            AgroalDataSource logsDs, User user) {
        List<DeviceDto> devices = new ArrayList<>();
        String sql = "SELECT eui,name,latitude,longitude,altitude,configuration FROM devices WHERE groups LIKE ? AND (userid = ? OR team LIKE ? OR administrators LIKE ?)";
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
                    devices.add(device);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error getting group devices: " + ex.getMessage());
        }
        return devices;
    }

    /**
     * Deserializes device configuration as map of String key-value pairs from JSON string.
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
    public String getReportFormat(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs, DataQuery query, User user, String format) {
        return null;
    }

    @Override
    public String getReportFormat(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs, DataQuery query, Integer organization, Integer tenant, String path, User user, String format) {
        // TODO: Implement this method
        return null;
    }

}
