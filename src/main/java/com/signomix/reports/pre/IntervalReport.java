package com.signomix.reports.pre;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.Dataset;
import com.signomix.common.db.DatasetHeader;
import com.signomix.common.db.DatasetRow;
import com.signomix.common.db.Report;
import com.signomix.common.db.ReportIface;
import com.signomix.common.db.ReportResult;
import io.agroal.api.AgroalDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.jboss.logging.Logger;

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
        // header.columns.add("delta");
        header.columns.add(channelName);
        result.addDatasetHeader(header);

        // get data
        /*
         * String sql1 = getSqlQuery(168);
         * String sql0 = getSqlQuery(169);
         */
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
        String sql = getSqlQuery(query, channelColumnNames);
        ArrayList<DatasetRow> rows0 = new ArrayList<>(); // 1 row more to calculate delta
        logger.info("SQL query: " + sql);
        String channelColumnName = channelColumnNames.get(channelName);
        try (Connection conn = olapDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DatasetRow row = new DatasetRow();
                    row.timestamp = rs.getTimestamp("bucket").getTime();
                    if(query.isIntervalDeltas()) {
                        row.values.add(rs.getDouble("delta"));
                    } else {
                        row.values.add(rs.getDouble(channelColumnName));
                    }
                    rows0.add(row);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error getting data: " + ex.getMessage());
            result.error("Error getting data: " + ex.getMessage());
        }
        //if (!query.isIntervalDeltas()) {
            for (int i = 0; i < rows0.size(); i++) {
                DatasetRow row = new DatasetRow();
                row.timestamp = rows0.get(i).timestamp;
                row.values.add((double) rows0.get(i).values.get(0));
                dataset.data.add(row);
            }
        //} else {
            //// calculate delta
        //    double delta = 0;
        //    for (int i = 0; i < rows0.size() - 1; i++) {
        //         if (i < rows0.size() - 1) {
        //             delta = (double) rows0.get(i).values.get(0) - (double) rows0.get(i + 1).values.get(0);
        //         } else {
        //             delta = 0;
        //         }
        //         DatasetRow row = new DatasetRow();
        //         // TODO: implement query.isIntervalTimestampAtEnd() logic
        //         if (query.isIntervalTimestampAtEnd()) {
        //             //
        //         } else {
        //             //
        //         }
        //         row.timestamp = rows0.get(i).timestamp;
        //         row.values.add(delta);
        //         dataset.data.add(row);
        //     }
        // }


        // logger.info("Multiplier channel name: " + query.getMultiplierChannelName());
        /*
         * if (query.getMultiplierChannelName() != null &&
         * !query.getMultiplierChannelName().isEmpty()) {
         * String sqlMultiplier;
         * Timestamp firstMultiplierValueTimestamp = null;
         * try {
         * firstMultiplierValueTimestamp = findFirstMultiplierValueTimestamp(olapDs,
         * query,
         * multiplierChannelColumnNames.get(query.getMultiplierChannelName()));
         * sqlMultiplier = getSqlQuery4Multiplier(query, multiplierChannelColumnNames);
         * } catch (Exception e) {
         * e.printStackTrace();
         * result.error("Error getting multiplier data: " + e.getMessage());
         * return result;
         * }
         * //logger.info("SQL query for multiplier: " + sqlMultiplier);
         * ArrayList<DatasetRow> rows1 = new ArrayList<>();
         * String multiplierChannelColumnName =
         * multiplierChannelColumnNames.get(query.getMultiplierChannelName());
         * try (Connection conn = olapDs.getConnection();
         * PreparedStatement stmt = conn.prepareStatement(sqlMultiplier)) {
         * stmt.setString(1, multiplayerDevice.eui);
         * stmt.setTimestamp(2, firstMultiplierValueTimestamp);
         * stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis())); // quick
         * fix: use current time as toTs
         * try (ResultSet rs = stmt.executeQuery()) {
         * while (rs.next()) {
         * DatasetRow row = new DatasetRow();
         * row.timestamp = rs.getTimestamp("ts").getTime();
         * row.values.add(rs.getDouble(multiplierChannelColumnName));
         * rows1.add(row);
         * }
         * }
         * } catch (SQLException ex) {
         * logger.error("Error getting multiplier data: " + ex.getMessage());
         * result.error("Error getting multiplier data: " + ex.getMessage());
         * }
         * // calculate multiplied values
         * if (rows1.size() > 0) {
         * for (int i = 0; i < dataset.data.size(); i++) {
         * DatasetRow row = dataset.data.get(i);
         * if (i < rows1.size()) {
         * double multiplierValue = (double) rows1.get(i).values.get(0);
         * double originalValue = (double) row.values.get(0);
         * row.values.add(originalValue * multiplierValue); // add multiplied value
         * } else {
         * row.values.add(0.0); // no multiplier value, add 0
         * }
         * }
         * }
         * }
         */

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
        HashSet<String> deviceChannelNamesSet = new HashSet<>();
        String sql = "SELECT channels FROM devicechannels WHERE eui = ?";
        logger.info("SQL query to get multiplier channel names: " + sql);
        logger.info("Multiplier device EUI: " + query.getMultiplierDeviceEui());
        try (Connection conn = oltpDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, query.getMultiplierDeviceEui());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String channels = rs.getString(1);
                    if (channels != null) {
                        channelNames = channels.split(","); // channels names declared in the device
                        for (int i = 0; i < channelNames.length; i++) {
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

    private Timestamp findFirstMultiplierValueTimestamp(
            AgroalDataSource olapDs, DataQuery query, String multiplierChannelColumnName) {
        Timestamp firstTimestamp = null;
        String periodCondition;
        String sql = "SELECT last(tstamp,tstamp) as tstamp FROM analyticdata WHERE eui = ? AND ";
        Timestamp fromTs = query.getFromTs();
        String interval = query.getInterval();
        if (!(fromTs != null || (interval != null && query.getLimit() > 0))) {
            // error, no fromTs or interval specified
        }
        if (fromTs != null) {
            periodCondition = "tstamp <= ?";
        } else {
            periodCondition = "tstamp <= now () - ?*INTERVAL '1 " + query.getIntervalName() + "'";
        }
        sql += periodCondition;
        logger.info("SQL query to find first multiplier value timestamp: " + sql);
        try (Connection conn = olapDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, query.getMultiplierDeviceEui());
            if (fromTs != null) {
                stmt.setTimestamp(2, fromTs);
            } else {
                stmt.setInt(2, query.getLimit());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    firstTimestamp = rs.getTimestamp("tstamp");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            logger.error("Error getting first multiplier value timestamp: " + ex.getMessage());
        }
        return firstTimestamp;
    }

    private String getSqlQuery(DataQuery query,
            HashMap<String, String> channelColumnNames) {
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
        String eui = query.getEui();
        String[] channelNames = query.getChannelName().split(",");
        String channelColumnName = channelColumnNames.get(channelNames[0]); // only first channel is supported
        boolean isMultiplier = (query.getMultiplierChannelName() != null
                && !query.getMultiplierChannelName().isEmpty());

        String period;
        int numberOfSamples = query.getLimit();
        // if (query.isIntervalDeltas()) {
        //     numberOfSamples++;
        // }
        if (query.getFromTs() != null && query.getToTs() != null) {
            // deltas are not supported in this case
            // TODO: to support deltas in this case, we need to subtract one interval from
            // query.getFromTs()
            period = " AND tstamp >= '" + query.getFromTs() + "' AND tstamp <= '" + query.getToTs() + "' ";
        } else {
            period = " AND tstamp >= now () - INTERVAL '" + numberOfSamples + " " + query.getIntervalName() + "' "
                    + "AND tstamp <= now() ";
        }

        String sql;
        if (isMultiplier || query.isGapfill() || query.isIntervalDeltas()) {
            if(isMultiplier){
                /*
SELECT a.bucket, a.delta * b.v2 AS result
FROM (
    SELECT bucket, value - LAG(value) OVER (ORDER BY bucket) AS delta
    FROM (
        SELECT 
            time_bucket_gapfill('1 day', tstamp) AS bucket, locf(last(d1, tstamp)) AS value
        FROM analyticdata
        WHERE tstamp >= '2025-05-20' AND tstamp <= '2025-06-10' AND eui='ORGTEST1'
        GROUP BY bucket
    ) sub_a
) a
JOIN (
    SELECT bucket, locf(last(d1, tstamp)) AS v2
    FROM analyticdata
    WHERE tstamp >= '2025-05-20' AND tstamp <= '2025-06-10' AND eui='MP'
    GROUP BY bucket
) b
ON a.bucket = b.bucket
ORDER BY a.bucket;
                 */
            }else{
            sql = "SELECT bucket,value as "+ channelColumnName +",value - LAG(value) OVER (ORDER BY bucket) AS delta " + //
                    "FROM (" +
                    "    SELECT time_bucket_gapfill('" + interval + "', tstamp) AS bucket, locf(last(" + channelColumnName + ", tstamp)) AS value" + //
                    "    FROM analyticdata" + //
                    "    WHERE eui='ORGTEST1'" + //
                    period +
                    "    GROUP BY bucket" + //
                    ") sub ORDER BY bucket";
            }
        } else {
            sql = "SELECT time_bucket('" + interval + "', tstamp) AS bucket," + //
                    "  last(" + channelColumnName + ", tstamp) as " + channelColumnName + " " + //
                    "FROM analyticdata " + //
                    "WHERE eui='" + eui + "' AND " + channelColumnName + " IS NOT NULL " + //
                    // "WHERE eui='" + eui + "' " + //
                    period + //
                    "GROUP BY eui, bucket " + //
                    "ORDER BY bucket DESC;";
        }
        return sql;
    }

    private String getSqlQuery4Multiplier(DataQuery query,
            HashMap<String, String> multiplierChannelColumnNames) throws Exception {
        /*
         * SELECT
         * time_bucket_gapfill('1 day', tstamp) AS ts,
         * locf(last(d1,tstamp)) as d1
         * FROM analyticdata
         * WHERE eui='ORGTEST1'
         * AND tstamp>now () - 10*INTERVAL '1 day'
         * AND tstamp < now()
         * GROUP BY eui,ts
         * ORDER BY ts DESC LIMIT 100
         */
        String interval;
        if (query.isInterval()) {
            interval = query.getInterval();
        } else {
            interval = "1 hour";
        }
        String multiplierChannelColumnName = null;
        String multiplierChannelName = query.getMultiplierChannelName();
        logger.info("Multiplier channel name (1): " + multiplierChannelName);
        logger.info("Multiplier channel column names size: " + multiplierChannelColumnNames.size());
        if (multiplierChannelName != null) {
            multiplierChannelColumnName = multiplierChannelColumnNames.get(multiplierChannelName);
        }
        if (multiplierChannelColumnName == null) {
            throw new Exception("No multiplier channel name specified");
        }

        String sql = "SELECT time_bucket_gapfill('" + interval + "', tstamp) AS ts," + //
                "  locf(last(" + multiplierChannelColumnName + ", tstamp)) as " + multiplierChannelColumnName + " " + //
                "FROM analyticdata " + //
                "WHERE eui=? " + //
                "AND tstamp>=? " + // use fromTs to get only data after the first multiplier value timestamp
                "AND tstamp<=? " + // use toTs to get only data before the last multiplier value timestamp
                "GROUP BY eui, ts " + //
                "ORDER BY ts DESC;";

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
        logger.info("SQL query: " + sql);
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
            logger.info("Reading result set for device: " + eui);
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
        } catch (

        SQLException ex) {
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
