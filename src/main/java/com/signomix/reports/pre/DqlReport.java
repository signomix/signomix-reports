package com.signomix.reports.pre;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;

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
        try{
            defaultLimit=(Integer)options.get("result.limit");
        }catch(Exception e){
            logger.error("Error getting default limit: "+e.getMessage());
        }
        String reportName = DATASET_NAME;
        ReportResult result = new ReportResult();
        result.setQuery("default", query);
        result.contentType = "application/json";
        result.setId(-1L);
        result.setTitle("User " + user.uid + " login history report");
        result.setDescription("");
        result.setTimestamp(new Timestamp(System.currentTimeMillis()));
        result.setQuery(reportName, query);

        if (query.getEui() != null) {
            result = getDeviceData(result, olapDs, oltpDs, logsDs, query, user, defaultLimit);
        } else if (query.getGroup() != null) {
            result = getGroupData(result, olapDs, oltpDs, logsDs, query, user, defaultLimit);
        } else {
            result.error("No data source specified");
        }
        return result;
    }

    private ReportResult getDeviceData(ReportResult result, AgroalDataSource olapDs, AgroalDataSource oltpDs,
            AgroalDataSource logsDs, DataQuery query, User user, int defaultLimit) {
        Dataset dataset = new Dataset(DATASET_NAME);
        dataset.eui = query.getEui();
        dataset.size = 0L;

        // get channel names
        HashMap<String, String> channelColumnNames = new HashMap<>();
        String[] channelNames = {};
        String[] requestedChannelNames = {};
        if (query.getChannelName() == null) {
            result.error("No channel name specified");
            return result;
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
                            channelColumnNames.put(channelNames[i], "d" + (i + 1));
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            logger.error("Error getting channel names: " + ex.getMessage());
            result.error("Error getting channel names: " + ex.getMessage());
        }
        if (query.getChannelName().equals("*")) {
            requestedChannelNames = channelNames;
        } else {
            requestedChannelNames = query.getChannelName().split(",");
        }
        DatasetHeader header = new DatasetHeader(DATASET_NAME);
        for (int i = 0; i < requestedChannelNames.length; i++) {
            header.columns.add(requestedChannelNames[i]);
        }
        result.addDatasetHeader(header);

        // get data
        boolean useDefaultLimit = true;
        sql = "SELECT * FROM analyticdata WHERE eui = ? ";
        if (query.getFromTs() != null) {
            sql += " AND tstamp >= ? ";
            if (query.getToTs() != null) {
                sql += " AND tstamp <= ? ";
            }
        } else {
            // limit
            useDefaultLimit = false;
        }
        if (query.getProject() != null) {
            sql += " AND project = ? ";
        }

        if (useDefaultLimit) {
            sql += " ORDER BY tstamp " + query.getSortOrder() + " ";
        } else {
            sql += " ORDER BY tstamp DESC ";
        }

        sql += " LIMIT ? ";

        logger.debug("SQL query: " + sql);
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
                while (rs.next()) {
                    DatasetRow row = new DatasetRow();
                    row.timestamp = rs.getTimestamp("tstamp").getTime();
                    for (int i = 0; i < requestedChannelNames.length; i++) {
                        row.values.add(rs.getDouble(channelColumnNames.get(requestedChannelNames[i])));
                    }
                    dataset.data.add(row);
                }
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

    private ReportResult getGroupData(ReportResult result, AgroalDataSource olapDs, AgroalDataSource oltpDs,
            AgroalDataSource logsDs, DataQuery query, User user, int defaultLimit) {
        Dataset data = new Dataset(DATASET_NAME);
        result.error("Group data not implemented");
        return result;
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
