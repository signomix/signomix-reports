package com.signomix.reports.domain;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.DataQueryException;
import com.signomix.common.db.ReportDaoIface;
import com.signomix.common.db.ReportIface;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.pre.DummyReport;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

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

    @ConfigProperty(name = "signomix.report.limit")
    Integer reportResultLimit;

    ReportDaoIface reportDao;

    void onStart(@Observes StartupEvent ev) {
        reportDao = new com.signomix.common.tsdb.ReportDao();
        reportDao.setDatasource(olapDs);
    }

    public ReportResult generateReport(String query, Integer organization, Integer tenant, String path, User user) {
        ReportResult result = new ReportResult();
        result.status = 501;
        result.errorMessage = "Not implemented";
        return result;
        
        /* DataQuery dataQuery;
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
        return result; */
    }

    public ReportResult generateReport(String query, User user) {
        DataQuery dataQuery;
        try {
            dataQuery = DataQuery.parse(query);
        } catch (DataQueryException e) {
            e.printStackTrace();
            return new ReportResult().error(400,"DataQuery error: " + e.getMessage());
        }
        return generateReport(dataQuery, user);
        /* 
        String className = null;
        try {
            className = dataQuery.getClassName();
            if(className == null) {
                return new ReportResult().error(400,"Class not defined in query");
            }else if(className.indexOf(".")<0) {
                className = "com.signomix.reports.pre." + className;
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
        return result; */
    }

    public ReportResult generateReport(DataQuery dataQuery, User user) {

        String className = null;
        try {
            className = dataQuery.getClassName();
            if(className == null) {
                return new ReportResult().error(400,"Class not defined in query");
            }else if(className.indexOf(".")<0) {
                className = "com.signomix.reports.pre." + className;
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

    public String generateFormatedReport(DataQuery dataQuery, User user) {

        String className = null;
        try {
            className = dataQuery.getClassName();
            if(className == null) {
                return "Error 400: Class not defined in query";
            }else if(className.indexOf(".")<0) {
                className = "com.signomix.reports.pre." + className;
            }
            boolean isAvailable = reportDao.isAvailable(className, user.number, user.organization.intValue(),
                    user.tenant, user.path);
            if (!isAvailable) {
                return "Error 401: Access denied for " + className;
            }
        } catch (Exception e) {
            return "Error 400:" + e.getMessage();
        }
        ReportIface report = (ReportIface) getReportInstance(dataQuery, user);
        if (null == report) {
            return "Error 400: Report not found: " + className;
        }
        if(dataQuery.getFormat().equals("html")) {
            return report.getReportHtml(olapDs,oltpDs,logsDs,dataQuery, user);
        }else if(dataQuery.getFormat().equals("csv")) {
            return report.getReportCsv(oltpDs, olapDs, logsDs, dataQuery, user);
        }else {
            return "Error 400: Format not supported: " + dataQuery.getFormat();
        }
    }

    private Object getReportInstance() {
        return new DummyReport();
    }

    private ReportIface getReportInstance(DataQuery query, User user) {
        String className = query.getClassName();
        if(className == null) {
            return null;
        }else if(className.indexOf(".")<0) {
            className = "com.signomix.reports.pre." + className;
        }
        ReportIface report = null;
        try {
            report = (ReportIface) Class.forName(className).getDeclaredConstructor().newInstance();
            HashMap<String, Object> options = new HashMap<>();
            options.put("result.limit", reportResultLimit);
            report.setOptions(options);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            logger.warn("Error creating report instance", e);
        }
        return report;
    }

}
