package com.signomix.reports.pre;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

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
import redis.clients.jedis.Jedis;

public class TwinsReport extends Report implements ReportIface {

    private static final Logger logger = Logger.getLogger(TwinsReport.class);

    private static final String DATASET_NAME = "dataset0";
    private static final String QUERY_NAME = "default";

    private static final int DEFAULT_ORGANIZATION = 1;

    private static Jedis jedis;

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
        return null;

    }

    public ReportResult getReportResult(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            Integer organization,
            Integer tenant,
            String path,
            User user) {

        List<String> channelNames = query.getChannels();

        ReportResult result = new ReportResult();
        result.setQuery("default", query);
        result.contentType = "application/json";
        result.setId(-1L);
        result.setTitle("");
        result.setDescription("");
        result.setTimestamp(new Timestamp(System.currentTimeMillis()));

        try {

            logger.info("Generating twins report for organization " + organization + " with query: " + query);

            Dataset dataset = new Dataset(query.getEui());
            dataset.name = DATASET_NAME;
            dataset.eui = query.getEui();
            dataset.size = 0L;

            
            HashMap<String, Object> config;
            String eui;
            Double value;
            long timestamp=System.currentTimeMillis();
            Jedis jedis = new Jedis("redis", 6379);
            // get redis keys starts from prefix
            String keyPrefix = organization + ":" + query.getGroup(); // eg: "158:DKHSROOM*"
            Set<String> keys = jedis.keys(keyPrefix);
            for (String key : keys) {
                HashMap<String, String> dataMap = new HashMap<>(jedis.hgetAll(key));
                //log dataMap
                logger.info("Twins data for key " + key + ": " + dataMap.toString());
                DatasetHeader header = new DatasetHeader(dataMap.get("eui"));
                for (String channel : channelNames) {
                    header.columns.add(channel);
                }
                header.name=dataMap.get("eui");
                result.addDatasetHeader(header);
                eui = dataMap.get("eui");
                dataset = new Dataset(eui);
                dataset.eui = eui;
                dataset.name = dataMap.get("name");
                DatasetRow row = new DatasetRow();
                try{
                    row.timestamp = Long.parseLong(dataMap.get("timestamp"));
                } catch (NumberFormatException nfe){
                    logger.warn("Invalid timestamp format for device: " + eui);
                    row.timestamp = timestamp;
                }
                for (int i = 0; i < channelNames.size(); i++) {
                    try{
                        value = Double.parseDouble(dataMap.get(channelNames.get(i)));
                    } catch (NumberFormatException nfe){
                        value=null;
                    } catch (Exception e){
                        logger.warn("Error parsing value for channel: " + channelNames.get(i) + ", Error: " + e.getMessage());
                        value=null;
                    }
                    logger.info("Channel: " + channelNames.get(i) + ", Value: " + value);
                    row.values.add(value);
                }
                dataset.data.add(row);
                dataset.size = 1L;
                result.addDataset(dataset);
                config = new HashMap<>();
                config.put("name", dataMap.get("name"));
                result.configs.put(dataset.eui, config);
            }
            logger.info("sort order: " + query.getSortOrder());
            if (query.getSortOrder() != null && query.getSortOrder().equalsIgnoreCase("asc")) {
                // sort datasets by name ascending
                result.datasets.sort((d1, d2) -> d2.name.compareTo(d1.name));
                // sort dataset headers by name
                result.headers.sort((h1, h2) -> h2.name.compareTo(h1.name));
            } else {
                // sort datasets by name descending
                result.datasets.sort((d1, d2) -> d1.name.compareTo(d2.name));
                // sort dataset headers by name
                result.headers.sort((h1, h2) -> h1.name.compareTo(h2.name));
            }
            result.status = 200;

        } catch (Exception ex) {
            logger.error("Error getting group data: " + ex.getMessage());
            result.error(500, "Error getting group data: " + ex.getMessage());
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
                        for (int i = 0; i < channelArray.length; i++) {
                            if (i >= MAX_CHANNELS) {
                                break;
                            }
                            String channel = channelArray[i];
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
