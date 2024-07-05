package com.signomix.reports.domain;

import com.signomix.common.db.DataQuery;

public interface ReportIface {

    public ReportResult getReportResult(DataQuery query, Integer organization, Integer tenant, String path);
    public ReportResult getReportResult(DataQuery query);
}
