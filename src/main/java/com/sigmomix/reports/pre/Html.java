package com.sigmomix.reports.pre;

import com.sigmomix.reports.domain.ReportIface;
import com.sigmomix.reports.domain.ReportResult;
import com.signomix.common.db.DataQuery;

public class Html implements ReportIface {

    @Override
    public ReportResult getReportResult(DataQuery query, Integer organization, Integer tenant, String path,
            String language) {
        ReportResult result = new ReportResult(query, language);
        result.contentType="text/html";
        result.content="<h1>Test</h1><p>Test</p>";
        return result;
    }

    @Override
    public ReportResult getReportResult(DataQuery query, String language) {
        ReportResult result = new ReportResult(query, language);
        result.contentType="text/html";
        result.content="<h1>Test</h1><p>Test</p>";
        return result;
    }
    
}
