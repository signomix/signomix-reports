package com.signomix.reports.domain.dashboard;

import com.signomix.common.User;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TextWidgetBuilder extends AbstractWidgetBuilder {
    @Override
    public String buildContent(User user, Widget widget, String timeZone) {
        StringBuilder content = new StringBuilder();
        content.append("<div class=\"widget-text\">");
        String text = widget.description.replaceAll("(?i)<(?!/?(b|i|u|br|p)\\b)[^>]*>", "");
        content.append(text).append("</div>");
        return content.toString();
    }
}
