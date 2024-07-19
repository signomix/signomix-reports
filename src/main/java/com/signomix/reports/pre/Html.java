package com.signomix.reports.pre;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.domain.Report;
import com.signomix.reports.domain.ReportIface;

import io.agroal.api.AgroalDataSource;

public class Html extends Report implements ReportIface {

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
        ReportResult result = new ReportResult(query);
        result.contentType = "text/html";
        result.content = "<h1>Test</h1><p>Test</p>";
        return result;
    }

    @Override
    public ReportResult getReportResult(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            User user) {
        ReportResult result = new ReportResult(query);
        result.contentType = "text/html";
        result.content = "<h1>Test</h1><p>Test</p>";
        return result;
    }

}
