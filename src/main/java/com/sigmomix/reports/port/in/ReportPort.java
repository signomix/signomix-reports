package com.sigmomix.reports.port.in;

import com.sigmomix.reports.domain.ReportResult;
import com.sigmomix.reports.domain.ReportRunner;
import com.signomix.common.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReportPort {

    @Inject
    ReportRunner reportRunner;

    public ReportResult getReportResult(String query, Integer organization, Integer tenant, String path, String language) {
        return reportRunner.generateReport(query, organization, tenant, path, language);
    }

    public ReportResult getReportResult(String query, String language, User user) {
        return reportRunner.generateReport(query, language, user);
    }

}
