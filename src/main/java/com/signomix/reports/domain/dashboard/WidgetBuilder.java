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

    public String buildWidget(User user, Widget widget, String timeZone) {
        StringBuilder content = new StringBuilder();
        if (widget.title != null && !widget.title.isEmpty()) {
            content
                .append("<div class=\"widget-title\">")
                .append(
                    AbstractWidgetBuilder.escapeHtml(
                        (widget.type.equalsIgnoreCase("buttonplus")
                            ? ""
                            : widget.title)
                    )
                )
                .append("</div>\n");
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
            default:
                content
                    .append("unsupported widget type: ")
                    .append(AbstractWidgetBuilder.escapeHtml(widget.type));
                break;
        }
        return content.toString();
    }
}
