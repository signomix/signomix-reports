package com.sigmomix.reports.domain;

import java.sql.Timestamp;

import com.signomix.common.db.DataQuery;

public class DummyReport implements ReportIface{

    @Override
    public ReportResult getReportResult(DataQuery query, Integer organization, Integer tenant, String path,
            String language) {
                String reportName = "dataset1";
                ReportResult result = new ReportResult();
                result.query = query;
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
                data.size=1000L;
                for (int i = 0; i < 10; i++) {
                    DatasetRow row = new DatasetRow();
                    row.timestamp = System.currentTimeMillis()+i*1000;
                    row.values.add(20.0 + Math.random() * 10);
                    row.values.add(40.0 + Math.random() * 20);
                    data.data.add(row);
                }
                result.addDataset(data);

                return result;

    }

    @Override
    public ReportResult getReportResult(DataQuery query, String language) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReportResult'");
    }

}
