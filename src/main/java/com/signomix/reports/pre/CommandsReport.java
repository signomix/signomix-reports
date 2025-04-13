package com.signomix.reports.pre;

import java.sql.Timestamp;
import java.util.List;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.Dataset;
import com.signomix.common.db.DatasetHeader;
import com.signomix.common.db.DatasetRow;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.IotDatabaseIface;
import com.signomix.common.db.Report;
import com.signomix.common.db.ReportIface;
import com.signomix.common.db.ReportResult;
import com.signomix.common.iot.CommandDto;
import com.signomix.common.tsdb.IotDatabaseDao;

import io.agroal.api.AgroalDataSource;

public class CommandsReport extends Report implements ReportIface {

    private static final Logger logger = Logger.getLogger(CommandsReport.class);

    private static final String DATASET_NAME = "dataset0";
    private static final String QUERY_NAME = "default";

    IotDatabaseIface iotDao;

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
        result.setTitle("Command history report");
        result.setDescription("");
        result.setTimestamp(new Timestamp(System.currentTimeMillis()));
        result.setQuery(reportName, query);

        DatasetHeader header = new DatasetHeader(reportName);
        result.addDatasetHeader(header);

        Dataset data = new Dataset(reportName);
        data.eui = query.getEui();
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
        return getData(oltpDs, user, query);
    }

    private ReportResult getData(AgroalDataSource oltpDs,
            User user, DataQuery query) {

        String reportName = DATASET_NAME;
        ReportResult result = new ReportResult();
        result.setQuery("default", null);
        result.contentType = "application/json";
        result.setId(-1L);
        result.setTitle("");
        result.setDescription("");
        result.setTimestamp(new Timestamp(System.currentTimeMillis()));
        Dataset dataset = new Dataset();
        dataset.name = reportName;
        dataset.eui = query.getEui();
        dataset.size = 0L;

        // get channel names
        String[] channelNames = { "category", "type", "origin", "payload", "port", "sentat" };
        String[] requestedChannelNames = {};
        if(query.getChannels() == null || query.getChannels().isEmpty()) {
            requestedChannelNames = channelNames;
        } else {
            requestedChannelNames = query.getChannels().toArray(new String[0]);
        }
        DatasetHeader header = new DatasetHeader(query.getEui());
        for (int i = 0; i < requestedChannelNames.length; i++) {
            header.columns.add(requestedChannelNames[i]);
        }
        result.addDatasetHeader(header);

        // get data
        List<CommandDto> commands = null;
        try {
            iotDao = new IotDatabaseDao();
            iotDao.setDatasource(oltpDs);
            commands = iotDao.getDeviceCommands(query.getEui(), query.isNotNull());
        } catch (IotDatabaseException e) {
            e.printStackTrace();
            logger.error("Error getting commands for device: " + query.getEui() + " - " + e.getMessage());
            return result;
        }
        logger.info("Commands found for device: " + query.getEui()+  " - " + commands.size());
        for (CommandDto command : commands) {
            DatasetRow row = new DatasetRow();
            row.timestamp = command.createdAt;
            for (int i = 0; i < requestedChannelNames.length; i++) {
                switch (requestedChannelNames[i]) {
                    case "category":
                        row.values.add(command.category);
                        break;
                    case "type":
                        row.values.add(command.type);
                        break;
                    case "origin":
                        row.values.add(command.origin);
                        break;
                    case "payload":
                        row.values.add(command.payload);
                        break;
                    case "port":
                        row.values.add(command.port);
                        break;
                    case "sentat":
                        row.values.add(command.sentAt);
                        break;
                }
            }
            dataset.data.add(row);
        }
        result.addDataset(dataset);
        return result;
    }

    @Override
    public String getReportHtml(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, Integer organization, Integer tenant, String path, User user, Boolean withHeader) {
        throw new UnsupportedOperationException("Unimplemented method 'getReportCsv'");
    }

    @Override
    public String getReportHtml(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, User user, Boolean withHeader) {
        throw new UnsupportedOperationException("Unimplemented method 'getReportCsv'");
    }

    @Override
    public String getReportCsv(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, Integer organization, Integer tenant, String path, User user) {
        throw new UnsupportedOperationException("Unimplemented method 'getReportCsv'");
    }

    @Override
    public String getReportCsv(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, User user) {
        throw new UnsupportedOperationException("Unimplemented method 'getReportCsv'");
    }

    @Override
    public String getReportFormat(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, User user, String format) {
        throw new UnsupportedOperationException("Unimplemented method 'getReportCsv'");
    }

    @Override
    public String getReportFormat(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, Integer organization, Integer tenant, String path, User user, String format) {
        throw new UnsupportedOperationException("Unimplemented method 'getReportCsv'");
    }

}
