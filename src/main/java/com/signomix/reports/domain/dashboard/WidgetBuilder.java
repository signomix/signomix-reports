package com.signomix.reports.domain.dashboard;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.DataQueryException;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.domain.AlertLevelService;
import com.signomix.reports.port.in.ReportPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WidgetBuilder {

    @Inject
    Logger logger;

    @Inject
    ReportPort reportPort;

    @Inject
    AlertLevelService alertLevelService;

    public String buildWidget(User user, Widget widget, String timeZone) {
        StringBuilder content = new StringBuilder();
        if (widget.title != null && !widget.title.isEmpty()) {
            content
                .append("<div class=\"widget-title\">")
                .append(escapeHtml(widget.title))
                .append("</div>\n");
        }
        switch (widget.type) {
            case "text":
                content.append(buildTextWidget(user, widget.description));
                break;
            case "symbol":
                content.append(buildSymbolWidget(user, widget, timeZone));
                break;
            case "led":
                content.append(buildLedWidget(user, widget, timeZone));
                break;
            default:
                content.append(
                    "unsupported widget type: " + escapeHtml(widget.type)
                );
                break;
        }
        return content.toString();
    }

    public String buildTextWidget(User user, String text) {
        logger.debug("Building text widget with description: " + text);
        StringBuilder content = new StringBuilder();
        content.append("<div class=\"widget-text\">");
        //content.append(escapeHtml(text));
        // sanitize text to allow only basic HTML tags like <b>, <i>, <u>, <br>, <p>
        // and escape the rest
        text = text.replaceAll("(?i)<(?!/?(b|i|u|br|p)\\b)[^>]*>", "");
        content.append(text);
        content.append("</div>");
        return content.toString();
    }

    public String buildSymbolWidget(User user, Widget widget, String timeZone) {
        String errorString = null;
        logger.debug("Building symbol widget with query: " + widget.query);
        DataQuery query = null;
        try {
            query = DataQuery.parse(widget.query);
        } catch (DataQueryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (query == null) {
            return (
                "<div class=\"widget-error\">" +
                "error parsing query" +
                "</div>\n"
            );
        }
        String unitName = widget.unitName != null ? widget.unitName : "";
        logger.debug("Unit name: " + unitName);
        //Double value = getSingleValue(user, query);
        //Long timesamp = getTimestamp(user, query);
        Measurement measurement = getSingleValue(user, query);
        if (measurement == null || measurement.value() == null) {
            return "<div class=\"widget-no-data\">" + "no data" + "</div>\n";
        }
        int rounding = 2;
        double value =
            Math.round(measurement.value() * Math.pow(10, rounding)) /
            Math.pow(10, rounding);
        // format value as text
        String valueText = String.format("%." + rounding + "f", value);
        valueText = valueText + " " + unitName;
        String tstamp = getTimestampFormatted(
            measurement.timestamp(),
            timeZone
        );
        return (
            "<div class=\"widget-content\">" +
            valueText +
            "</div>\n" +
            "<div class=\"widget-info\">" +
            tstamp +
            "</div>\n"
        );
    }

    public String buildLedWidget(User user, Widget widget, String timeZone) {
        String errorString = null;
        logger.debug("Building LED widget with query: " + widget.query);
        DataQuery query = null;
        try {
            query = DataQuery.parse(widget.query);
        } catch (DataQueryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (query == null) {
            return (
                "<div class=\"widget-error\">" +
                "error parsing query" +
                "</div>\n"
            );
        }
        String iconDef = widget.icon != null ? widget.icon : "";
        String[] icons = iconDef.split(",");
        String alertRule = widget.rule != null ? widget.rule : "";

        Measurement measurement = getSingleValue(user, query);
        if (measurement == null || measurement.value() == null) {
            return "<div class=\"widget-no-data\">" + "no data" + "</div>\n";
        }
        int rounding = 2;
        double value =
            Math.round(measurement.value() * Math.pow(10, rounding)) /
            Math.pow(10, rounding);

        String iconName = "";
        // format value as text
        String valueText = getIconText(
            value,
            measurement.timestamp(),
            alertRule,
            icons
        );
        String tstamp = getTimestampFormatted(
            measurement.timestamp(),
            timeZone
        );
        return (
            "<div class=\"widget-content\">" +
            valueText +
            "</div>\n" +
            "<div class=\"widget-info\">" +
            tstamp +
            "</div>\n"
        );
    }

    private String getIconText(
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
        // iconNames[0] = "bi-emoji-smile-fill";
        // iconNames[1] = "bi-emoji-neutral-fill";
        // iconNames[2] = "bi-emoji-frown-fill";
        // iconNames[3] = "bi-emoji-expressionless-fill";
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
        //TODO: get color based on alert level
        return "<i class=\"h3 bi " + iconNames[index] + "\"></i>";
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private Measurement getSingleValue(User user, DataQuery query) {
        ReportResult result = reportPort.getReportResult(query, user);
        if (result.status != 200) {
            logger.error("Error executing query: " + result.errorMessage);
            return null;
        }
        String channel = query.getChannels().get(0);
        int index = result.headers.get(0).columns.indexOf(channel);
        if (index == -1) {
            logger.error("Channel not found in result: " + channel);
            return null;
        }
        if (
            result.datasets.size() == 0 ||
            result.datasets.get(0).data.size() == 0
        ) {
            logger.warn("Query returned no data for channel: " + channel);
            return null;
        }
        Measurement measurement = null;
        double value;
        long ts;
        try {
            value = (Double) result.datasets
                .get(0)
                .data.get(0)
                .values.get(index);
            ts = (Long) result.datasets.get(0).data.get(0).timestamp;
            measurement = new Measurement(ts, value);
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }

        return measurement;
    }

    private String getTimestampFormatted(long timestamp, String timeZone) {
        
        String formatted;
        // format timestamp as date and time
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss"
        );
        sdf.setTimeZone(java.util.TimeZone.getTimeZone(timeZone));
        formatted = sdf.format(new java.util.Date(timestamp));
        logger.debug("Formatting timestamp: " + timestamp + " with time zone: " + timeZone+ " formatted: " + formatted);
        return formatted;
    }
}
