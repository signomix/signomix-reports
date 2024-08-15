package com.signomix.reports.pre;

import java.sql.Timestamp;
import java.util.HashMap;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.Dataset;
import com.signomix.common.db.DatasetHeader;
import com.signomix.common.db.DatasetRow;
import com.signomix.common.db.Report;
import com.signomix.common.db.ReportIface;
import com.signomix.common.db.ReportResult;

import io.agroal.api.AgroalDataSource;

public class DummyReport extends Report implements ReportIface {

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
        String reportName = "dataset0";
        ReportResult result = new ReportResult();
        result.setQuery("default", query);
        result.contentType = "application/json";
        result.setId(-1L);
        result.setTitle("Dummy report");
        result.setDescription("This is a dummy report");
        result.setTimestamp(new Timestamp(System.currentTimeMillis()));
        result.setQuery(reportName, query);

        DatasetHeader header = new DatasetHeader(reportName);
        header.columns.add("temperature");
        header.columns.add("humidity");
        result.addDatasetHeader(header);

        Dataset data = new Dataset(reportName);
        data.eui = "123456";
        data.size = 1000L;
        for (int i = 0; i < 10; i++) {
            DatasetRow row = new DatasetRow();
            row.timestamp = System.currentTimeMillis() + i * 1000;
            row.values.add(20.0 + Math.random() * 10);
            row.values.add(40.0 + Math.random() * 20);
            data.data.add(row);
        }
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

        String reportName = "dataset0";
        ReportResult result = new ReportResult();
        result.setQuery("default", query);
        result.contentType = "application/json";
        result.setId(-1L);
        result.setTitle("Dummy report");
        result.setDescription("This is a dummy report");
        result.setTimestamp(new Timestamp(System.currentTimeMillis()));
        result.setQuery(reportName, query);

        DatasetHeader header = new DatasetHeader(reportName);
        header.columns.add("temperature");
        header.columns.add("humidity");
        result.addDatasetHeader(header);

        Dataset data = new Dataset(reportName);
        data.eui = "123456";
        data.size = 1000L;
        for (int i = 0; i < 10; i++) {
            DatasetRow row = new DatasetRow();
            row.timestamp = System.currentTimeMillis() + i * 1000;
            row.values.add(20.0 + Math.random() * 10);
            row.values.add(40.0 + Math.random() * 20);
            data.data.add(row);
        }
        result.addDataset(data);

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
