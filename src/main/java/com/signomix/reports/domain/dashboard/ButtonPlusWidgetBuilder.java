package com.signomix.reports.domain.dashboard;

import com.signomix.common.User;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ButtonPlusWidgetBuilder extends AbstractWidgetBuilder {

    DialogValueBuilder dialogValueBuilder = new DialogValueBuilder();

    @Override
    public String buildContent(User user, Widget widget, String timeZone) {
        StringBuilder content = new StringBuilder();
        String dialogElement = "dialog-value-" + widget.originalIndex;
        String okCallbackName = "okCallback_" + widget.originalIndex;
        String cancelCallbackName = "cancelCallback_" + widget.originalIndex;

        String dialogContent = dialogValueBuilder.buildDialog(
            dialogElement,
            widget.title,
            widget.description,
            new String[] { "OK", "Cancel" },
            "body",
            widget.configuration,
            okCallbackName,
            cancelCallbackName
        );
        content.append(dialogContent);

        // Add callback functions scripts (using img onerror hack to ensure they are evaluated when injected via innerHTML)
        content
            .append("<img src=\"x\" onerror=\"")
            .append("window.")
            .append(okCallbackName)
            .append(
                " = function(param) { alert('NOT IMPLEMENTED - OK clicked, parameter: ' + param); }; "
            )
            .append("window.")
            .append(cancelCallbackName)
            .append(
                " = function(param) { alert('NOT IMPLEMENTED - Cancel clicked, parameter: ' + param); }; "
            )
            .append(
                "this.parentNode.removeChild(this);\" style=\"display:none;\" />\n"
            );

        content.append("<div class=\"widget-content\">");
        content.append("<div class=\"container p-0\">");
        String title =
            widget.title != null && !widget.title.isEmpty()
                ? widget.title
                : "Command button";
        content
            .append(
                "<a href=\"#\" onclick=\"event.preventDefault(); document.getElementById('"
            )
            .append(dialogElement)
            .append(
                "').showModal();\" class=\"btn btn-danger w-100\" role=\"button\" aria-disabled=\"true\">"
            )
            .append(title)
            .append("</a>");
        content.append("</div>");
        content.append("</div>");
        return content.toString();
    }
}
