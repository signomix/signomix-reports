package com.signomix.reports.domain;

import java.util.List;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.db.ReportDefinition;
import com.signomix.common.tsdb.ReportDao;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReportManager {

    @Inject
    Logger logger;

    @DataSource("oltp")
    AgroalDataSource oltpDs;

    ReportDao managerDao;

    void onStart(@Observes StartupEvent ev) {
        managerDao = new ReportDao();
        managerDao.setDatasource(oltpDs);
    }

        public ReportDefinition getReportDefinition(Integer id, User user) {
            ReportDefinition reportDefinition;
            try {
                reportDefinition = managerDao.getReportDefinition(id);
            } catch (IotDatabaseException e) {
                throw new RuntimeException("Error retrieving report definition", e);
            }
            if (reportDefinition == null) {
                throw new RuntimeException("Report definition not found");
            }
            if (!reportDefinition.userLogin.equals(user.uid) && (reportDefinition.administrators == null || !reportDefinition.administrators.contains(user.uid))) {
                throw new RuntimeException("Unauthorized access to report definition");
            }
            return reportDefinition;
        }

        public void updateReportDefinition(ReportDefinition reportDefinition, User user) {
            ReportDefinition existingReportDefinition = getReportDefinition(reportDefinition.id, user);
            if (!existingReportDefinition.userLogin.equals(user.uid) && (existingReportDefinition.administrators == null || !existingReportDefinition.administrators.contains(user.uid))) {
                throw new RuntimeException("Unauthorized access to report definition");
            }
            try {
                managerDao.updateReportDefinition(reportDefinition.id, reportDefinition);
            } catch (IotDatabaseException e) {
                throw new RuntimeException("Error updating report definition", e);
            }
        }

        public int saveReportDefinition(ReportDefinition reportDefinition, User user) {
            if (reportDefinition.id != null) {
                throw new RuntimeException("Report definition ID must be null for new report definitions");
            }
            ReportDefinition newReportDefinition = new ReportDefinition();
            newReportDefinition.userLogin = user.uid;
            newReportDefinition.organization = user.organization.intValue();
            newReportDefinition.tenant = user.tenant;
            try {
                return managerDao.saveReportDefinition(reportDefinition);
            } catch (IotDatabaseException e) {
                throw new RuntimeException("Error saving report definition", e);
            }
        }

        public void deleteReportDefinition(Integer id, User user) {
            ReportDefinition existingReportDefinition = getReportDefinition(id, user);
            if (!existingReportDefinition.userLogin.equals(user.uid) && (existingReportDefinition.administrators == null || !existingReportDefinition.administrators.contains(user.uid))) {
                throw new RuntimeException("Unauthorized access to report definition");
            }
            try {
                managerDao.deleteReportDefinition(id);
            } catch (IotDatabaseException e) {
                throw new RuntimeException("Error deleting report definition", e);
            }
        }

        public List<ReportDefinition> getReportDefinitions(User user) {
            try {
                return managerDao.getReportDefinitions(user.uid);
            } catch (IotDatabaseException e) {
                throw new RuntimeException("Error retrieving report definitions", e);
            }
        }

}
