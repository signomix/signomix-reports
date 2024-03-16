package com.sigmomix.reports.domain;

import com.signomix.common.db.DataQuery;

public interface ReportIface {

    public ReportResult getReportResult(DataQuery query, Integer organization, Integer tenant, String path, String language);

}
