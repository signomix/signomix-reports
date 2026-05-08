package com.signomix.reports.domain.dashboard;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.DataQueryException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SymbolWidgetBuilder extends AbstractWidgetBuilder {
    @Override
    public String buildContent(User user, Widget widget, String timeZone) {
        DataQuery query = null;
        try { query = DataQuery.parse(widget.query); } catch (DataQueryException e) {}
        if (query == null) return ERR_QUERY_MESSAGE;
        
        String unitName = widget.unitName != null ? widget.unitName : "";
        Measurement measurement = getSingleValue(user, query);
        if (measurement == null || measurement.value() == null) return ERR_DATA_MESSAGE;
        
        String valueText = getValueFormatted(measurement.value(), widget.rounding) + " " + unitName;
        String tstamp = getTimestampFormatted(measurement.timestamp(), timeZone);
        return "<div class=\"widget-content\">" + valueText + "</div>\n<div class=\"widget-info\">" + tstamp + "</div>\n";
    }
}
