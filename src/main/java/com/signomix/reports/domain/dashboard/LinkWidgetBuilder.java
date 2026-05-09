package com.signomix.reports.domain.dashboard;

import com.signomix.common.User;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LinkWidgetBuilder extends AbstractWidgetBuilder {

    @Override
    public String buildContent(User user, Widget widget, String timeZone) {
        StringBuilder content = new StringBuilder();
        content.append("<div class=\"widget-led\">");
        String url =
            widget.dashboardId != null ? widget.dashboardId.trim() : "";
        content
            .append(
                "<a href=\"#\" onclick=\"event.preventDefault(); " +
                    DASHBOARD_SELECTION_FUNCTION_NAME +
                    "('" +
                    url +
                    "');\" class=\"btn btn-primary w-100\" role=\"button\" aria-disabled=\"true\">"
            )
            .append(
                widget.title != null && !widget.title.isEmpty()
                    ? widget.title
                    : url
            )
            .append("</a>");
        content.append("</div>");
        return content.toString();
    }
}
