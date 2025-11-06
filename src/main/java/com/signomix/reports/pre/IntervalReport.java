package com.signomix.reports.pre;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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

    private static final Logger logger = Logger.getLogger(IntervalReport.class);
    private static final int DEFAULT_ORGANIZATION = 1;

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
            DeviceDto multiplierDevice = null;
            if (query.getMultiplierDeviceEui() != null) {
                multiplierDevice = getDevice(oltpDs, query.getMultiplierDeviceEui(), user.uid, user.organization);
            }
            DeviceDto device = getDevice(oltpDs, query.getEui(), user.uid, user.organization);
            if (device == null) {
                result = new ReportResult();
                result.contentType = "application/json";
                result.error(404, "Device not found");
                return result;
            }
            result = getDeviceData(olapDs, oltpDs, logsDs, query, user, defaultLimit, device, multiplierDevice);
        } else {
            result = new ReportResult();
            result.contentType = "application/json";
            result.error("No data source specified");
        }
        return result;
    }

    private ReportResult getDeviceData(AgroalDataSource olapDs, AgroalDataSource oltpDs,
            AgroalDataSource logsDs, DataQuery query, User user, int defaultLimit, DeviceDto device,
            DeviceDto multiplayerDevice) {

        String channelName = query.getChannels().get(0); // only first channel is supported
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
        header.columns.add(channelName);
        result.addDatasetHeader(header);

        // get data
        String zone = query.getZone();
        HashMap<String, String> channelColumnNames;
        try {
            channelColumnNames = getChannelColumnNames(query, oltpDs);
        } catch (Exception e) {
            logger.error("Error getting channel names: " + e.getMessage());
            result.error("Error getting channel names: " + e.getMessage());
            return result;
        }
        HashMap<String, String> multiplierChannelColumnNames = new HashMap<>();
        try {
            multiplierChannelColumnNames = getMultiplierChannelColumnNames(query, oltpDs);
        } catch (Exception e) {
            logger.warn("Error getting multiplier channel names: " + e.getMessage());
        }
        String sql = getSqlQuery(query, channelColumnNames, multiplierChannelColumnNames);
        ArrayList<DatasetRow> rows0 = new ArrayList<>(); // 1 row more to calculate delta
        //logger.info("Time zone: " + zone);
        String channelColumnName = channelColumnNames.get(channelName);
        try (Connection conn = olapDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DatasetRow row = new DatasetRow();
                    row.timestamp = rs.getTimestamp("bucket").getTime();
                    if (query.isIntervalDeltas()) {
                        row.values.add(rs.getDouble("delta"));
                    } else {
                        row.values.add(rs.getDouble(channelColumnName));
                    }
                    rows0.add(row);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            logger.error("Error getting data: " + ex.getMessage());
            result.error("Error getting data: " + ex.getMessage());
        }
        // if (!query.isIntervalDeltas()) {
        for (int i = 0; i < rows0.size(); i++) {
            DatasetRow row = new DatasetRow();
            row.timestamp = rows0.get(i).timestamp;
            row.values.add((double) rows0.get(i).values.get(0));
            dataset.data.add(row);
        }
        dataset.size = (long) dataset.data.size();
        result.addDataset(dataset);
        return result;
    }

    /**
     * Get channel names for the device.
     *
     * @param query  DataQuery object containing the device EUI and channel names
     * @param oltpDs AgroalDataSource for OLTP database connection
     * @return HashMap with channel names as keys and column names as values
     * @throws Exception if no channel name is specified or if no channel definition is found for the device
     */
    private HashMap<String, String> getChannelColumnNames(DataQuery query, AgroalDataSource oltpDs) throws Exception {
        HashMap<String, String> channelColumnNames = new HashMap<>();
        String[] channelNames = {};
        String[] requestedChannelNames = {};
        HashSet<String> channelNamesSet = new HashSet<>();
        if (query.getChannelName() == null) {
            throw new Exception("No channel name specified");
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
                            if(i>=MAX_CHANNELS){
                                break;
                            }
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
                    throw new Exception("No channel definition found for device " + query.getEui());
                }
            }
        } catch (SQLException ex) {
            logger.error("Error getting channel names: " + ex.getMessage());
            throw new Exception("Error getting channel names: " + ex.getMessage());
        }
        return channelColumnNames;
    }

    private HashMap<String, String> getMultiplierChannelColumnNames(DataQuery query, AgroalDataSource oltpDs)
            throws Exception {
        HashMap<String, String> channelColumnNames = new HashMap<>();
        if (query.getMultiplierDeviceEui() == null) {
            logger.warn("No multiplier device EUI specified, no channels will be returned");
            return channelColumnNames; // no multiplier device, no channels
        }
        String[] channelNames = {};
        String[] requestedChannelNames = {};
        HashSet<String> channelNamesSet = new HashSet<>();
        if (query.getChannelName() == null) {
            throw new Exception("No channel name specified");
        }
        if (query.getChannelName().equals("*")) {
            requestedChannelNames = channelNames;
        } else {
            requestedChannelNames = query.getChannelName().split(",");
        }
        for (String channel : requestedChannelNames) {
            channelNamesSet.add(channel);
        }
        String sql = "SELECT channels FROM devicechannels WHERE eui = ?";
        try (Connection conn = oltpDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, query.getMultiplierDeviceEui());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String channels = rs.getString(1);
                    if (channels != null) {
                        channelNames = channels.split(","); // channels names declared in the device
                        for (int i = 0; i < channelNames.length; i++) {
                            if(i>=MAX_CHANNELS){
                                break;
                            }
                            channelColumnNames.put(channelNames[i], "d" + (i + 1));
                        }
                    }
                } else {
                    throw new Exception("No channel definition found for device " + query.getMultiplierDeviceEui());
                }
            }
        } catch (SQLException ex) {
            logger.error("Error getting channel names: " + ex.getMessage());
            throw new Exception("Error getting channel names: " + ex.getMessage());
        }
        return channelColumnNames;
    }

    private String getSqlQuery(DataQuery query,
            HashMap<String, String> channelColumnNames, HashMap<String, String> multiplierChannelColumnNames) {
        // There are two types of queries:
        // 1. Interval query with deltas (query.isIntervalDeltas() == true)
        // 2. Interval query without deltas (query.isIntervalDeltas() == false)
        // Each case can use gapfill or not, depending on the query.isGapfill() value.
        String interval;
        if (query.isInterval()) {
            interval = query.getInterval();
        } else {
            interval = "1 hour";
        }
        String zone = "UTC"; // TODO: use device timezone if available
        if (query.getZone() != null && !query.getZone().isEmpty()) {
            zone = query.getZone();
        }
        String eui = query.getEui();
        String[] channelNames = query.getChannelName().split(",");
        String channelColumnName = channelColumnNames.get(channelNames[0]); // only first channel is supported
        boolean isMultiplier = (query.getMultiplierChannelName() != null
                && !query.getMultiplierChannelName().isEmpty());

        String period;
        String multiplierPeriod; // is 40 days before feirst date of period
        String multiplierChannelColumnName = multiplierChannelColumnNames.get(query.getMultiplierChannelName());
        String multiplierEui = query.getMultiplierDeviceEui();
        int numberOfSamples = query.getLimit();

        if (query.getFromTs() != null && query.getToTs() != null) {
            period = " AND tstamp >= '" + query.getFromTs() + "' AND tstamp <= '" + query.getToTs() + "' ";
            multiplierPeriod = " AND tstamp >= '" + query.getFromTs() + "'::timestamp - INTERVAL '40 day' " +
                    " AND tstamp <= '" + query.getToTs() + "' ";
        } else {
            period = " AND tstamp >= now () - INTERVAL '" + numberOfSamples + " " + query.getIntervalName() + "' "
                    + "AND tstamp <= now() ";
            multiplierPeriod = " AND tstamp >= now () - INTERVAL '" + (numberOfSamples + 40) + " "
                    + query.getIntervalName() + "' "
                    + "AND tstamp <= now() ";
        }

        String sql="";
        if (isMultiplier || query.isGapfill() || query.isIntervalDeltas()) {
            if (isMultiplier) {

                sql += "SELECT a.bucket, a.delta * b.v2 AS " + channelColumnName + ", a.delta * b.v2 AS delta" + //
                        " FROM (" +
                        "    SELECT bucket, value - LAG(value) OVER (ORDER BY bucket) AS delta " + //
                        "    FROM (" +
                        "        SELECT " +
                        "            time_bucket_gapfill('" + interval + "', tstamp, '"+zone+"') AS bucket, locf(last("
                        + channelColumnName + ", tstamp AT TIME ZONE '"+zone+"')) AS value" + //
                        "        FROM analyticdata" + //
                        "        WHERE eui='" + eui + "' " +
                        period +
                        "        GROUP BY bucket" + //
                        "    ) sub_a" + //
                        ") a " + //
                        " JOIN (" +
                        "    SELECT time_bucket_gapfill('" + interval + "', tstamp, '"+zone+"') AS bucket, locf(last("
                        + multiplierChannelColumnName + ", tstamp AT TIME ZONE '"+zone+"')) AS v2" + //
                        "    FROM analyticdata" + //
                        "    WHERE eui='" + multiplierEui + "'" + //
                        multiplierPeriod +
                        "    GROUP BY bucket" + //
                        ") b " + //
                        "ON a.bucket = b.bucket " + //
                        "ORDER BY a.bucket DESC;";
            } else {
                sql += "SELECT bucket,value as " + channelColumnName
                        + ",value - LAG(value) OVER (ORDER BY bucket) AS delta " + //
                        "FROM (" +
                        "    SELECT time_bucket_gapfill('" + interval + "', tstamp, '"+zone+"') AS bucket, locf(last("
                        + channelColumnName + ", tstamp AT TIME ZONE '"+zone+"')) AS value" + //
                        "    FROM analyticdata" + //
                        "    WHERE eui='" + eui + "'" + //
                        period +
                        "    GROUP BY bucket" + //
                        ") sub ORDER BY bucket DESC;";
            }
        } else {
            sql += "SELECT time_bucket('" + interval + "', tstamp, '"+zone+"') AS bucket," + //
                    "  last(" + channelColumnName + ", tstamp AT TIME ZONE '"+zone+"') as " + channelColumnName + " " + //
                    "FROM analyticdata " + //
                    "WHERE eui='" + eui + "' AND " + channelColumnName + " IS NOT NULL " + //
                    period + //
                    "GROUP BY eui, bucket " + //
                    "ORDER BY bucket DESC;";
        }
        //logger.info("SQL query: " + sql);
        return sql;
    }

    private DeviceDto getDevice(AgroalDataSource oltpDs, String eui, String userId, Long organization) {
        DeviceDto device = null;
        String sql = null;
        if (organization == DEFAULT_ORGANIZATION) {
            sql = "SELECT eui,name,latitude,longitude,altitude,channels FROM devices WHERE eui = ? AND (userid = ? OR team LIKE ? OR administrators LIKE ?)";
        } else {
            sql = "SELECT eui,name,latitude,longitude,altitude,channels FROM devices WHERE eui = ? AND organization = ?";
        }
        // logger.info("SQL query: " + sql);
        try (Connection conn = oltpDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, eui);
            if (organization == DEFAULT_ORGANIZATION) {
                stmt.setString(2, userId);
                stmt.setString(3, "%," + userId + ",%");
                stmt.setString(4, "%," + userId + ",%");
            } else {
                stmt.setLong(2, organization);
            }
            // logger.info("Reading result set for device: " + eui);
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
    public String getReportHtml(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, Integer organization, Integer tenant, String path, User user, Boolean withHeader) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReportHtml'");
    }

    @Override
    public String getReportHtml(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, User user, Boolean withHeader) {
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
        ReportResult result = getReportResult(olapDs, oltpDs, logsDs, query, user);
        return super.getAsCsv(result, 0, "\r\n",
                ",", true);
    }

    @Override
    public String getReportFormat(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, User user, String format) {
        // TODO: Implement this method
        throw new UnsupportedOperationException("Unimplemented method 'getReportCsv'");
    }

    @Override
    public String getReportFormat(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, Integer organization, Integer tenant, String path, User user, String format) {
        // TODO: Implement this method
        throw new UnsupportedOperationException("Unimplemented method 'getReportCsv'");
    }

}
