package com.signomix.reports.pre;

import com.signomix.common.db.DataQuery;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.domain.ReportIface;

public class Html implements ReportIface {

    @Override
    public ReportResult getReportResult(DataQuery query, Integer organization, Integer tenant, String path) {
        ReportResult result = new ReportResult(query);
        result.contentType="text/html";
        result.content="<h1>Test</h1><p>Test</p>";
        return result;
    }

    @Override
    public ReportResult getReportResult(DataQuery query) {
        ReportResult result = new ReportResult(query);
        result.contentType="text/html";
        result.content="<h1>Test</h1><p>Test</p>";
        return result;
    }
    
}
