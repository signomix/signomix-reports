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

public class GroupReport extends Report implements ReportIface {

    private static final Logger logger = Logger.getLogger(GroupReport.class);

    private static final String DATASET_NAME = "dataset0";
    private static final String QUERY_NAME = "default";

    private static final int DEFAULT_ORGANIZATION = 1;

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
            result = new ReportResult();
            result.contentType = "application/json";
            result.error(404, "Report not applicable for device. It is for groups only.");
            return result;
        } else if (query.getGroup() != null) {
            result = getGroupData(olapDs, oltpDs, logsDs, query, user, defaultLimit);
        } else {
            result = new ReportResult();
            result.contentType = "application/json";
            result.error("No data source specified");
        }
        return result;
    }

    private ReportResult getGroupData(AgroalDataSource olapDs, AgroalDataSource oltpDs,
            AgroalDataSource logsDs, DataQuery query, User user, int defaultLimit) {

        List<String> channelNames = getGroupChannels(oltpDs, query.getGroup(), user);
        if (channelNames.isEmpty()) {
            logger.warn("No channels found for group: " + query.getGroup());
            ReportResult result = new ReportResult();
            result.contentType = "application/json";
            result.error("No channels found for group: " + query.getGroup());
            return result;
        }
        String sql = "SELECT last(a.eui,a.tstamp) AS eui,last(a.tstamp,a.tstamp) AS tstamp,"
        + "d.name as cfg_name,d.latitude as cfg_latitude,d.longitude as cfg_longitude,d.altitude as cfg_altitude,";
        for (int i = 0; i < channelNames.size(); i++) {
            sql += "last(a.d" + (i + 1) + ",a.tstamp) FILTER (WHERE a.d" + (i + 1) + " IS NOT NULL) AS d" + (i + 1);
            if (i < channelNames.size() - 1) {
                sql += ",";
            }
        }
        sql += " FROM analyticdata a";
        sql += " JOIN devices d ON d.eui = a.eui";
        sql += " WHERE a.eui IN (SELECT eui FROM devices WHERE groups LIKE ?)";
        sql += " GROUP BY a.eui, d.name, d.latitude, d.longitude, d.altitude";

        ReportResult result = new ReportResult();
        result.setQuery("default", query);
        result.contentType = "application/json";
        result.setId(-1L);
        result.setTitle("");
        result.setDescription("");
        result.setTimestamp(new Timestamp(System.currentTimeMillis()));

        try (Connection conn = olapDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%," + query.getGroup() + ",%");
            try (ResultSet rs = stmt.executeQuery()) {
                Dataset dataset = new Dataset(query.getGroup());
                dataset.name = DATASET_NAME;
                dataset.eui = query.getGroup();
                dataset.size = 0L;

                DatasetHeader header = new DatasetHeader(query.getGroup());
                for (String channel : channelNames) {
                    header.columns.add(channel);
                }
                // result.addDatasetHeader(header);
                HashMap<String, Object> config;
                String eui;
                Double value;
                while (rs.next()) {
                    result.addDatasetHeader(header);
                    eui = rs.getString("eui");
                    dataset = new Dataset(eui);
                    dataset.eui = eui;
                    try {
                        dataset.name = rs.getString("cfg_name");
                    } catch (SQLException e) {
                        dataset.name = DATASET_NAME;
                    }
                    DatasetRow row = new DatasetRow();
                    row.timestamp = rs.getTimestamp("tstamp").getTime();
                    // latitude, longitude, altitude are rs columns number 4, 5, 6
                    for (int i = 0; i < channelNames.size(); i++) {
                        //row.values.add(rs.getDouble("d" + (i + 1)));
                        value = rs.getDouble(i + 7);
                        if (rs.wasNull()) {
                            row.values.add(null);
                        } else {
                            row.values.add(value);  
                        }
                    }
                    dataset.data.add(row);
                    dataset.size = 1L;
                    result.addDataset(dataset);
                    config = new HashMap<>();
                    config.put("name", dataset.name);
                    try {
                        config.put("latitude", rs.getDouble("cfg_latitude"));
                        config.put("longitude", rs.getDouble("cfg_longitude"));
                        config.put("altitude", rs.getDouble("cfg_altitude"));
                    } catch (SQLException e) {
                    }
                    result.configs.put(dataset.eui, config);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error getting group data: " + ex.getMessage());
            result.error("Error getting group data: " + ex.getMessage());
            logger.error("SQL: " + sql);
        }

        return result;
    }

    private List<String> getGroupChannels(AgroalDataSource oltpDs, String groupEui, User user) {
        List<String> channels = new ArrayList<>();
        String sql;
        if (user.organization == DEFAULT_ORGANIZATION) {
            sql = "SELECT channels FROM groups WHERE eui=? AND (userid = ? OR team LIKE ? OR administrators LIKE ?)";
        } else {
            sql = "SELECT channels FROM groups WHERE eui=? AND organization = ?";
        }
        try (Connection conn = oltpDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (user.organization == DEFAULT_ORGANIZATION) {
                stmt.setString(1, groupEui);
                stmt.setString(2, user.uid);
                stmt.setString(3, "%," + user.uid + ",%");
                stmt.setString(4, "%," + user.uid + ",%");
            } else {
                stmt.setString(1, groupEui);
                stmt.setLong(2, user.organization);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String channelsStr = rs.getString("channels");
                    if (channelsStr != null) {
                        String[] channelArray = channelsStr.split(",");
                        for (String channel : channelArray) {
                            if (channel != null && !channel.trim().isEmpty()) {
                                channels.add(channel);
                            }
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            logger.error("Error getting group channels: " + ex.getMessage());
        }
        return channels;
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
            logger.debug("Error deserializing device configuration: " + e.getMessage());
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
