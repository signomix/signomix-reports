package com.signomix.reports.domain.dashboard;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.domain.AlertLevelService;
import com.signomix.reports.port.in.ReportPort;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.jboss.logging.Logger;

public abstract class AbstractWidgetBuilder {

    protected static final int DEFAULT_ROUNDING = 1;
    protected static final String ERR_QUERY_MESSAGE =
        "<div class=\"widget-error\">error parsing query</div>\n";
    protected static final String ERR_DATA_MESSAGE =
        "<div class=\"widget-no-data\">no data</div>\n";
    protected static final String DASHBOARD_SELECTION_FUNCTION_NAME =
        "selectDashboardCallback";
    protected static final String SEND_COMMAND_FUNCTION_NAME =
        "sendCommandCallback";
    protected static final String CANCEL_FUNCTION_NAME = "cancelCallback";

    @Inject
    protected Logger logger;

    @Inject
    protected ReportPort reportPort;

    @Inject
    protected AlertLevelService alertLevelService;

    public abstract String buildContent(
        User user,
        Widget widget,
        String timeZone
    );

    public static String escapeHtml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("\"", "&#39;");
    }

    protected String getValueFormatted(double value, int rounding) {
        if (rounding >= 0) {
            BigDecimal bd = new BigDecimal(Double.toString(value));
            bd = bd.setScale(rounding, RoundingMode.HALF_UP);
            value = bd.doubleValue();
            return String.format("%." + rounding + "f", value);
        } else {
            return String.format("%." + DEFAULT_ROUNDING + "f", value);
        }
    }

    protected Measurement getSingleValue(User user, DataQuery query) {
        ReportResult result = reportPort.getReportResult(query, user);
        if (result.status != 200) {
            logger.error("Error executing query: " + result.errorMessage);
            return null;
        }
        String channel = query.getChannels().get(0);
        int index = result.headers.get(0).columns.indexOf(channel);
        if (index == -1) return null;
        if (
            result.datasets.size() == 0 ||
            result.datasets.get(0).data.size() == 0
        ) return null;
        Measurement measurement = null;
        try {
            double value = (Double) result.datasets
                .get(0)
                .data.get(0)
                .values.get(index);
            long ts = (Long) result.datasets.get(0).data.get(0).timestamp;
            measurement = new Measurement(ts, value);
        } catch (Exception e) {}
        return measurement;
    }

    protected String getTimestampFormatted(long timestamp, String timeZone) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss"
        );
        sdf.setTimeZone(java.util.TimeZone.getTimeZone(timeZone));
        return sdf.format(new java.util.Date(timestamp));
    }

    protected String getIconText(
        double value,
        Long timestamp,
        String alertRule,
        String[] icons
    ) {
        String[] iconNames = {
            "bi-emoji-smile",
            "bi-emoji-neutral",
            "bi-emoji-frown",
            "bi-emoji-expressionless",
        };
        for (int i = 0; i < icons.length; i++) {
            if (!icons[i].trim().isEmpty()) iconNames[i] = icons[i].trim();
        }
        int alertLevel = alertLevelService.getAlertLevel(
            alertRule,
            value,
            timestamp
        );
        int index =
            alertLevel >= 0 && alertLevel < iconNames.length ? alertLevel : 3;
        return "<i class=\"h3 bi " + iconNames[index] + "\"></i>";
    }
}
