package com.signomix.reports.domain.dashboard;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.DataQueryException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LedWidgetBuilder extends AbstractWidgetBuilder {
    @Override
    public String buildContent(User user, Widget widget, String timeZone) {
        DataQuery query = null;
        try { query = DataQuery.parse(widget.query); } catch (DataQueryException e) {}
        if (query == null) return ERR_QUERY_MESSAGE;
        
        String[] icons = (widget.icon != null ? widget.icon : "").split(",");
        String alertRule = widget.rule != null ? widget.rule : "";
        Measurement measurement = getSingleValue(user, query);
        if (measurement == null || measurement.value() == null) return ERR_DATA_MESSAGE;
        
        double value = Math.round(measurement.value() * Math.pow(10, 2)) / Math.pow(10, 2);
        String valueText = getIconText(value, measurement.timestamp(), alertRule, icons);
        String tstamp = getTimestampFormatted(measurement.timestamp(), timeZone);
        return "<div class=\"widget-content\">" + valueText + "</div>\n<div class=\"widget-info\">" + tstamp + "</div>\n";
    }
}
