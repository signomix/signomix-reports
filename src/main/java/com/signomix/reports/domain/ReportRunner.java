package com.signomix.reports.domain;

import java.lang.reflect.InvocationTargetException;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.DataQueryException;
import com.signomix.common.db.ReportDaoIface;
import com.signomix.common.db.ReportResult;
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

    @Inject
    @DataSource("oltp")
    AgroalDataSource oltpDs;

    @Inject
    @DataSource("qdb")
    AgroalDataSource logsDs;

    ReportDaoIface reportDao;

    void onStart(@Observes StartupEvent ev) {
        reportDao = new com.signomix.common.tsdb.ReportDao();
        reportDao.setDatasource(olapDs);
    }

    public ReportResult generateReport(String query, Integer organization, Integer tenant, String path, User user) {
        DataQuery dataQuery;
        try {
            dataQuery = DataQuery.parse(query);
        } catch (DataQueryException e) {
            return new ReportResult().error(400,e.getMessage());
        }
        ReportIface report = (ReportIface) getReportInstance();
        if (null == report) {
            return new ReportResult().error(404,"Class not found: " + dataQuery.getClassName());
        }
        ReportResult result = report.getReportResult(olapDs,oltpDs,logsDs,dataQuery, organization, tenant, path, user);
        return result;
    }

    public ReportResult generateReport(String query, User user) {
        DataQuery dataQuery;
        String className = null;
        try {
            dataQuery = DataQuery.parse(query);
        } catch (DataQueryException e) {
            e.printStackTrace();
            return new ReportResult().error(400,"DataQuery error: " + e.getMessage());
        }
        try {
            className = dataQuery.getClassName();
            if (className == null) {
                return new ReportResult().error(400,"Class not defined in query");
            }
            boolean isAvailable = reportDao.isAvailable(className, user.number, user.organization.intValue(),
                    user.tenant, user.path);
            if (!isAvailable) {
                return new ReportResult().error(401,"Access denied for " + className);
            }
        } catch (Exception e) {
            return new ReportResult().error(400,"Error " + e.getMessage());
        }
        ReportIface report = (ReportIface) getReportInstance(dataQuery, user);
        if (null == report) {
            return new ReportResult().error(404,"Report not found: " + className);
        }
        ReportResult result = report.getReportResult(olapDs,oltpDs,logsDs,dataQuery, user);
        return result;
    }

    public ReportResult generateReport(DataQuery dataQuery, User user) {

        String className = null;
        try {
            className = dataQuery.getClassName();
            if (className == null) {
                return new ReportResult().error(400,"Class not defined in query");
            }
            boolean isAvailable = reportDao.isAvailable(className, user.number, user.organization.intValue(),
                    user.tenant, user.path);
            if (!isAvailable) {
                return new ReportResult().error(401,"Access denied for " + className);
            }
        } catch (Exception e) {
            return new ReportResult().error(400,"Error " + e.getMessage());
        }
        ReportIface report = (ReportIface) getReportInstance(dataQuery, user);
        if (null == report) {
            return new ReportResult().error(404,"Report not found: " + className);
        }
        ReportResult result = report.getReportResult(olapDs,oltpDs,logsDs,dataQuery, user);
        return result;
    }

    private Object getReportInstance() {
        return new DummyReport();
    }

    private ReportIface getReportInstance(DataQuery query, User user) {
        String className = query.getClassName();
/*         try {
            boolean isAvailable = reportDao.isAvailable(className, user.number, user.organization.intValue(),
                    user.tenant, user.path);
            if (!isAvailable) {
                return null;
            }
        } catch (Exception e) {
            return null;
        } */
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
