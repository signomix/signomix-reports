package com.signomix.reports.domain;

import java.lang.reflect.InvocationTargetException;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.DataQueryException;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.ReportDaoIface;
import com.signomix.reports.pre.DummyReport;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReportRunner {

    @Inject
    Logger logger;

    @Inject
    @DataSource("olap")
    AgroalDataSource olapDs;

    ReportDaoIface reportDao;

    void onStart(@Observes StartupEvent ev) {
        reportDao = new com.signomix.common.tsdb.ReportDao();
        reportDao.setDatasource(olapDs);
    }

    public ReportResult generateReport(String query, Integer organization, Integer tenant, String path) {
        DataQuery dataQuery;
        try {
            dataQuery = DataQuery.parse(query);
        } catch (DataQueryException e) {
            return new ReportResult().error(e.getMessage());
        }
        ReportIface report = (ReportIface) getReportInstance();
        if (null == report) {
            return new ReportResult().error("Class not found: " + dataQuery.getClassName());
        }
        ReportResult result = report.getReportResult(dataQuery, organization, tenant, path);
        return result;
    }

    public ReportResult generateReport(String query, User user) {
        DataQuery dataQuery;
        String className = null;
        try {
            dataQuery = DataQuery.parse(query);
        } catch (DataQueryException e) {
            e.printStackTrace();
            return new ReportResult().error("DataQuery error: " + e.getMessage());
        }
        try {
            className = dataQuery.getClassName();
            if (className == null) {
                return new ReportResult().error("Class not defined in query");
            }
            boolean isAvailable = reportDao.isAvailable(className, user.number, user.organization.intValue(),
                    user.tenant, user.path);
            if (!isAvailable) {
                return new ReportResult().error("Access denied for " + className);
            }
        } catch (Exception e) {
            return new ReportResult().error("Error " + e.getMessage());
        }
        ReportIface report = (ReportIface) getReportInstance(dataQuery, user);
        if (null == report) {
            return new ReportResult().error("Report not found: " + className);
        }
        ReportResult result = report.getReportResult(dataQuery);
        return result;
    }

    public ReportResult generateReport(DataQuery dataQuery, User user) {

        String className = null;
        try {
            className = dataQuery.getClassName();
            if (className == null) {
                return new ReportResult().error("Class not defined in query");
            }
            boolean isAvailable = reportDao.isAvailable(className, user.number, user.organization.intValue(),
                    user.tenant, user.path);
            if (!isAvailable) {
                return new ReportResult().error("Access denied for " + className);
            }
        } catch (Exception e) {
            return new ReportResult().error("Error " + e.getMessage());
        }
        ReportIface report = (ReportIface) getReportInstance(dataQuery, user);
        if (null == report) {
            return new ReportResult().error("Report not found: " + className);
        }
        ReportResult result = report.getReportResult(dataQuery);
        return result;
    }

    private Object getReportInstance() {
        return new DummyReport();
    }

    private ReportIface getReportInstance(DataQuery query, User user) {
        String className = query.getClassName();
        try {
            boolean isAvailable = reportDao.isAvailable(className, user.number, user.organization.intValue(),
                    user.tenant, user.path);
            if (!isAvailable) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        ReportIface report = null;
        try {
            report = (ReportIface) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return report;
    }

}
