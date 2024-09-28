package com.signomix.reports.pre;

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

public class DqlReport extends Report implements ReportIface {

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
            result = getDeviceData(olapDs, oltpDs, logsDs, query, user, defaultLimit);
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
            AgroalDataSource logsDs, DataQuery query, User user, int defaultLimit) {

        String reportName = DATASET_NAME;
        ReportResult result = new ReportResult();
        result.setQuery("default", query);
        result.contentType = "application/json";
        result.setId(-1L);
        result.setTitle("");
        result.setDescription("");
        result.setTimestamp(new Timestamp(System.currentTimeMillis()));
        // result.setQuery(reportName, query);

        String devEui = getDevice(oltpDs, query.getEui(), user.uid);
        if (devEui == null) {
            result.error("No device found: " + query.getEui());
            return result;
        }
        Dataset dataset = new Dataset(query.getEui());
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
        String sql = "SELECT channels FROM devicechannels WHERE eui = ?";
        try (Connection conn = oltpDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, query.getEui());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String channels = rs.getString(1);
                    if (channels != null) {
                        channelNames = channels.split(",");
                        for (int i = 0; i < channelNames.length; i++) {
                            if (channelNamesSet.contains(channelNames[i])) {
                                channelColumnNames.put(channelNames[i], "d" + (i + 1));
                            }
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
        boolean useDefaultLimit = query.getFromTs() != null;
        sql = getSqlQuery(query, useDefaultLimit, channelColumnNames);
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
            if (useDefaultLimit) {
                stmt.setInt(idx++, defaultLimit);
            } else {
                stmt.setInt(idx++, query.getLimit());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                double value;
                boolean noNulls;
                while (rs.next()) {
                    DatasetRow row = new DatasetRow();
                    row.timestamp = rs.getTimestamp("tstamp").getTime();
                    noNulls = true;
                    for (int i = 0; i < requestedChannelNames.length; i++) {
                        try {
                            value = rs.getDouble(channelColumnNames.get(requestedChannelNames[i]));
                            if (rs.wasNull()) {
                                row.values.add(null);
                                noNulls = false;
                            } else {
                                row.values.add(value);
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
        if (!useDefaultLimit && query.getSortOrder().equals("ASC")) {
            result = sortResult(result, DATASET_NAME);
        }
        return result;
    }

    private String getSqlQuery(DataQuery query, boolean useDefaultLimit, HashMap<String, String> channelColumnNames) {

        String columns = "tstamp,";
        for (String channel : channelColumnNames.keySet()) {
            columns += channelColumnNames.get(channel) + ",";
        }
        columns = columns.substring(0, columns.length() - 1);

        String notNullCondition;

        if (query.isNotNull()) {
            notNullCondition = " AND NOT (";
            for (String channel : channelColumnNames.keySet()) {
                notNullCondition += channelColumnNames.get(channel) + " IS NULL OR ";
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
        if (query.getProject() != null) {
            sql += " AND project = ? ";
        }

        sql += notNullCondition;

        if (useDefaultLimit) {
            sql += " ORDER BY tstamp " + query.getSortOrder() + " ";
        } else {
            sql += " ORDER BY tstamp DESC ";
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
        List<String> devices = getGroupDevices(query.getGroup(), oltpDs, logsDs, user);
        if (devices.isEmpty()) {
            result.error("No devices found in group " + query.getGroup());
            return result;
        } else {
            devices.forEach(device -> logger.info("Group device: " + device));
        }
        ReportResult tmpResult;
        Dataset dataset;
        DataQuery tmpQuery;
        for (int i = 0; i < devices.size(); i++) {
            try {
                tmpQuery = DataQuery.parse(query.getSource());
            } catch (DataQueryException e) {
                logger.warn("Error parsing query: " + e.getMessage());
                result.error("Error parsing query: " + e.getMessage());
                return result;
            }
            tmpResult = new ReportResult();
            tmpQuery.setEui(devices.get(i));
            tmpQuery.setGroup(null);
            tmpResult = getDeviceData(olapDs, oltpDs, logsDs, tmpQuery, user, defaultLimit);
                result.headers.add(tmpResult.headers.get(0));
            dataset = tmpResult.datasets.get(0);
            if(dataset != null && dataset.size > 0){
                result.datasets.add(dataset);
            }else{
                logger.warn("No data for device: " + devices.get(i));
            }
            
        }
        logger.info("result dataset size: " + result.datasets.size());
        return result;
    }

    private String getDevice(AgroalDataSource oltpDs, String eui, String userId) {
        String devEui = null;
        String sql = "SELECT eui FROM devices WHERE eui = ? AND (userid = ? OR team LIKE ? OR administrators LIKE ?)";
        try (Connection conn = oltpDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, eui);
            stmt.setString(2, userId);
            stmt.setString(3, "%," + userId + ",%");
            stmt.setString(4, "%," + userId + ",%");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    devEui = rs.getString(1);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error getting device: " + ex.getMessage());
        }
        return devEui;
    }

    private List<String> getGroupDevices(String groupEui, AgroalDataSource oltpDs,
            AgroalDataSource logsDs, User user) {
        List<String> devices = new ArrayList<>();
        String sql = "SELECT eui FROM devices WHERE groups LIKE ? AND (userid = ? OR team LIKE ? OR administrators LIKE ?)";
        try (Connection conn = oltpDs.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%," + groupEui + ",%");
            stmt.setString(2, user.uid);
            stmt.setString(3, "%," + user.uid + ",%");
            stmt.setString(4, "%," + user.uid + ",%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    devices.add(rs.getString(1));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error getting group devices: " + ex.getMessage());
        }
        return devices;
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
