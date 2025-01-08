package com.signomix.reports.port.in;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.domain.ReportRunner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReportPort {

    @Inject
    ReportRunner reportRunner;

    public ReportResult getReportResult(String query, Integer organization, Integer tenant, String path, String language, User user) {
        return reportRunner.generateReport(query, organization, tenant, path, user);
    }

    public ReportResult getReportResult(String query, User user) {
        return reportRunner.generateReport(query, user);
    }

    public ReportResult getReportResult(DataQuery query, User user) {
        return reportRunner.generateReport(query, user);
    }

    public String getReportResultFormatted(DataQuery query, User user, Boolean withHeader) {
        return reportRunner.generateFormatedReport(query, user, withHeader);
    }

}
