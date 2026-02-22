package com.signomix.reports.port.in;

import java.util.List;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.ReportDefinition;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.domain.ReportManager;
import com.signomix.reports.domain.ReportRunner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReportPort {

    @Inject
    ReportRunner reportRunner;

    @Inject
    ReportManager reportManager;
    
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

    public ReportResult getTwinsReportResult(String query, Integer organization, Integer tenant, String path, String language, User user) {
        try{
            DataQuery dataQuery = DataQuery.parse(query);
            return reportRunner.generateTwinsReport(dataQuery, organization, tenant, path, user);
        }catch(Exception e){
            ReportResult result = new ReportResult();
            result.status = 500;
            result.errorMessage = e.getMessage();
            return result;
        }
    }

    public void updateReportDefinition(ReportDefinition reportDefinition, User user) {
        reportManager.updateReportDefinition(reportDefinition, user);
    }

    public void deleteReportDefinition(Integer id, User user) {
        reportManager.deleteReportDefinition(id, user);
    }

    public ReportDefinition getReportDefinition(Integer id, User user) {
        return reportManager.getReportDefinition(id, user);
    }

    public List<ReportDefinition> getReportDefinitionsForUser(User user) {
        return reportManager.getReportDefinitions(user);
    }

    public int createReportDefinition(ReportDefinition reportDefinition, User user) {
        return reportManager.saveReportDefinition(reportDefinition, user);
    }




}
