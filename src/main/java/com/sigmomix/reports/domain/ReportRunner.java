package com.sigmomix.reports.domain;

import java.lang.reflect.InvocationTargetException;

import com.signomix.common.db.DataQuery;
import com.signomix.common.db.DataQueryException;
import com.signomix.common.db.ReportDaoIface;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReportRunner {

    @Inject
    @DataSource("olap")
    AgroalDataSource olapDs;

    ReportDaoIface reportDao;

    void onStart(@Observes StartupEvent ev) {
        reportDao = new com.signomix.common.tsdb.ReportDao();
        reportDao.setDatasource(olapDs);
    }

    public ReportResult generateReport(String query, Integer organization, Integer tenant, String path, String language) {
        DataQuery dataQuery;
        try {
            dataQuery = DataQuery.parse(query);
        } catch (DataQueryException e) {
            return new ReportResult().error(e.getMessage());
        }
        ReportIface report = (ReportIface)getReportInstance();
        if(null==report){
            return new ReportResult().error("Class not found: "+dataQuery.getClassName());
        }
        ReportResult result = report.getReportResult(dataQuery, organization, tenant, path, language);
        return result;
    }

    public ReportResult generateReport(String query, String className, String language) {
        DataQuery dataQuery;
        try {
            dataQuery = DataQuery.parse(query);
        } catch (DataQueryException e) {
            return new ReportResult().error(e.getMessage());
        }
        ReportIface report = (ReportIface)getReportInstance(className);
        if(null==report){
            return new ReportResult().error("Class not found: "+dataQuery.getClassName());
        }
        ReportResult result = report.getReportResult(dataQuery, language);
        return result;
    }

    private Object getReportInstance(){
        return new DummyReport();
    }

    private ReportIface getReportInstance(String className){
        ReportIface report=null;
        try {
            report = (ReportIface)Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return report;
    }

}
