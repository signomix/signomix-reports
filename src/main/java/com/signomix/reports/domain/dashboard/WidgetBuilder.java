package com.signomix.reports.domain.dashboard;

import com.signomix.common.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WidgetBuilder {

    @Inject
    TextWidgetBuilder textWidgetBuilder;

    @Inject
    SymbolWidgetBuilder symbolWidgetBuilder;

    @Inject
    LedWidgetBuilder ledWidgetBuilder;

    @Inject
    ReportWidgetBuilder reportWidgetBuilder;

    @Inject
    LinkWidgetBuilder linkWidgetBuilder;

    private static final String[] NO_TITLE_WIDGET_TYPES = {
        "buttonplus",
        "link",
    };

    public void onStart() {
        // this is needed to make sure that the builders are initialized
    }

    public String buildWidget(User user, Widget widget, String timeZone) {
        StringBuilder content = new StringBuilder();
        if (widget.title != null && !widget.title.isEmpty()) {
            if (hasTitle(widget.type.toLowerCase())) {
                content
                    .append("<div class=\"widget-title\">")
                    .append(AbstractWidgetBuilder.escapeHtml(widget.title))
                    .append("</div>\n");
            }
        }
        switch (widget.type) {
            case "text":
                content.append(
                    textWidgetBuilder.buildContent(user, widget, timeZone)
                );
                break;
            case "symbol":
                content.append(
                    symbolWidgetBuilder.buildContent(user, widget, timeZone)
                );
                break;
            case "led":
                content.append(
                    ledWidgetBuilder.buildContent(user, widget, timeZone)
                );
                break;
            case "report":
                content.append(
                    reportWidgetBuilder.buildContent(user, widget, timeZone)
                );
                break;
            case "buttonplus":
                content.append(
                    new ButtonPlusWidgetBuilder().buildContent(
                        user,
                        widget,
                        timeZone
                    )
                );
                break;
            case "link":
                content.append(
                    new LinkWidgetBuilder().buildContent(user, widget, timeZone)
                );
                break;
            default:
                content
                    .append("unsupported widget type: ")
                    .append(AbstractWidgetBuilder.escapeHtml(widget.type));
                break;
        }
        return content.toString();
    }

    private boolean hasTitle(String widgetType) {
        // some widget types doesn't use title
        for (String type : NO_TITLE_WIDGET_TYPES) {
            if (type.equalsIgnoreCase(widgetType)) {
                return false;
            }
        }
        return true;
    }
}
