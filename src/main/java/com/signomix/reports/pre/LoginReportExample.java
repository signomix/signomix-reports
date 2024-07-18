package com.signomix.reports.pre;

import java.sql.Timestamp;

import com.signomix.common.db.DataQuery;
import com.signomix.common.db.Dataset;
import com.signomix.common.db.DatasetHeader;
import com.signomix.common.db.DatasetRow;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.domain.ReportIface;

public class LoginReportExample implements ReportIface{

    @Override
    public ReportResult getReportResult(DataQuery query, Integer organization, Integer tenant, String path) {
                String reportName = "dataset0";
                ReportResult result = new ReportResult();
                result.setQuery("default", query);
                result.contentType = "application/json";
                result.setId(-1L);
                result.setTitle("Login report");
                result.setDescription("This is a dummy report");
                result.setTimestamp(new Timestamp(System.currentTimeMillis()));
                result.setQuery(reportName, query);

                DatasetHeader header = new DatasetHeader(reportName);
                header.columns.add("login");
                header.columns.add("result_code");
                result.addDatasetHeader(header);

                Dataset data = new Dataset(reportName);
                data.eui = "123456";
                data.size=4L;
                for (int i = 0; i < 4; i++) {
                    DatasetRow row = new DatasetRow();
                    row.timestamp = System.currentTimeMillis()+i*1000;
                    row.values.add("user"+i);
                    row.values.add("success");
                    data.data.add(row);
                }
                result.addDataset(data);

                return result;

    }

    @Override
    public ReportResult getReportResult(DataQuery query) {
        String reportName = "dataset0";
                ReportResult result = new ReportResult();
                result.setQuery("default", query);
                result.contentType = "application/json";
                result.setId(-1L);
                result.setTitle("Login report");
                result.setDescription("This is a dummy report");
                result.setTimestamp(new Timestamp(System.currentTimeMillis()));
                result.setQuery(reportName, query);

                DatasetHeader header = new DatasetHeader(reportName);
                header.columns.add("login");
                header.columns.add("result_code");
                result.addDatasetHeader(header);

                Dataset data = new Dataset(reportName);
                data.eui = "";
                data.size=4L;
                for (int i = 0; i < 4; i++) {
                    DatasetRow row = new DatasetRow();
                    row.timestamp = System.currentTimeMillis()+i*1000;
                    row.values.add("user"+i);
                    row.values.add("success");
                    data.data.add(row);
                }
                result.addDataset(data);

                return result;
    }

}
