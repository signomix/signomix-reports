package com.signomix.reports.pre.kanarek;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.pre.StatusReport2;

import io.agroal.api.AgroalDataSource;

public class KanarekReport extends StatusReport2 {

    @Override
    public String getReportFormat(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, User user, String format) {

        //if (format == null || !format.equals("kanarek")) {
        //    return "";
        //}

        ReportResult result = super.getReportResult(olapDs, oltpDs, logsDs, query, user);
        KanarekFormatter formatter = new KanarekFormatter();
        return formatter.format(result);
    }

}
