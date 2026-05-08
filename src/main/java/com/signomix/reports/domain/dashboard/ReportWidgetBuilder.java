package com.signomix.reports.domain.dashboard;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.DataQueryException;
import com.signomix.common.db.DatasetRow;
import com.signomix.common.db.ReportResult;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReportWidgetBuilder extends AbstractWidgetBuilder {
    @Override
    public String buildContent(User user, Widget widget, String timeZone) {
        DataQuery query = null;
        try { query = DataQuery.parse(widget.query); } catch (DataQueryException e) {}
        if (query == null) return ERR_QUERY_MESSAGE;
        
        ReportResult result = reportPort.getReportResult(query, user);
        if (result.status != 200) return null;
        
        StringBuilder content = new StringBuilder("<div class=\"overflow-scroll\"><table class=\"table table.sm\">\n<thead>\n<tr>\n");
        content.append("<th scope=\"col\"><i class=\"bi bi-clock\"></i></th>\n");
        for (int i = 0; i < result.headers.get(0).columns.size(); i++) {
            content.append("<th scope=\"col\" class=\"text-end\">").append(result.headers.get(0).columns.get(i)).append("</th>\n");
        }
        content.append("</tr>\n</thead>\n<tbody>\n");
        
        for (int i = 0; i < result.datasets.get(0).data.size(); i++) {
            DatasetRow datasetRow = result.datasets.get(0).data.get(i);
            content.append("<tr>\n<td>").append(getTimestampFormatted(datasetRow.timestamp, timeZone)).append("</td>\n");
            for (int j = 0; j < datasetRow.values.size(); j++) {
                String valueText = getValueFormatted((double) datasetRow.values.get(j), widget.rounding);
                content.append("<td class=\"text-end\">").append(valueText).append("</td>\n");
            }
            content.append("</tr>\n");
        }
        content.append("</tbody>\n</table>\n</div>\n");
        return content.toString();
    }
}
