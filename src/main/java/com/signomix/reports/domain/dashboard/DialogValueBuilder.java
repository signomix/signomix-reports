package com.signomix.reports.domain.dashboard;

import com.fasterxml.jackson.databind.JsonNode;

public class DialogValueBuilder {

    public String buildDialog(
        String dialogId,
        String title,
        String message,
        String[] labels,
        String color,
        JsonNode configuration,
        String okCallback,
        String cancelCallback
    ) {
        StringBuilder sb = new StringBuilder();

        String selectedColor = (color != null && !color.isEmpty())
            ? color
            : "primary";
        String okLabel = (labels != null && labels.length > 0)
            ? labels[0]
            : "OK";
        String cancelLabel = (labels != null && labels.length > 1)
            ? labels[1]
            : "Cancel";

        sb
            .append("<dialog id=\"")
            .append(dialogId)
            .append(
                "\" style=\"border: none; border-radius: 5px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); max-width: 400px; padding: 5px; z-index: 1000;\">\n"
            );
        sb
            .append("  <div class=\"text-center alert alert-")
            .append(selectedColor)
            .append(" px-2 py-1 m-0\" style=\"padding: 0px; margin: 0px;\">\n");
        sb
            .append("    <h5 class=\"card-title mt-1 mb-2\">")
            .append(title != null ? title : "")
            .append("</h5>\n");

        if (message != null && !message.isEmpty()) {
            sb.append("    <p>").append(message).append("</p>\n");
        }

        sb.append("    <div class=\"mb-1\">\n");
        sb.append("      <form>\n");
        sb.append("        <div class=\"mb-3 w-100\">\n");

        String label = "Set value";
        if (configuration != null && configuration.has("label")) {
            label = configuration.get("label").asText();
        }
        sb
            .append("          <label for=\"valueInput_")
            .append(dialogId)
            .append("\" class=\"form-label\">")
            .append(label)
            .append("</label>\n");

        if (
            configuration != null &&
            "number".equals(configuration.path("type").asText())
        ) {
            JsonNode numConfig = configuration.path("number");
            String min = numConfig.path("minimum").asText("");
            String max = numConfig.path("maximum").asText("");
            String step = numConfig.path("step").asText("");
            sb
                .append(
                    "          <input type=\"number\" class=\"form-control\" id=\"valueInput_"
                )
                .append(dialogId)
                .append("\" min=\"")
                .append(min)
                .append("\" max=\"")
                .append(max)
                .append("\" step=\"")
                .append(step)
                .append("\" />\n");
        } else if (
            configuration != null &&
            "option".equals(configuration.path("type").asText())
        ) {
            sb.append("          <div class=\"form-check\">\n");
            JsonNode options = configuration.path("option");
            if (options.isArray()) {
                for (JsonNode option : options) {
                    String optName = option.path("name").asText("");
                    String optValue = option.path("value").asText("");
                    sb
                        .append(
                            "            <label><input class=\"form-check-input me-1 ms-1\" type=\"radio\" name=\"amount_"
                        )
                        .append(dialogId)
                        .append("\" value=\"")
                        .append(optValue)
                        .append("\" />")
                        .append(optName)
                        .append("</label>\n");
                }
            }
            sb.append("          </div>\n");
        }

        String hint = "";
        if (configuration != null && configuration.has("hint")) {
            hint = configuration.get("hint").asText();
        }
        sb
            .append("          <div id=\"valueHelp_")
            .append(dialogId)
            .append("\" class=\"form-text\">")
            .append(hint)
            .append("</div>\n");
        sb.append("        </div>\n");
        sb.append("      </form>\n");
        sb.append("    </div>\n");

        String btnColor = "primary";
        if (configuration != null && configuration.has("btncolor")) {
            btnColor = configuration.get("btncolor").asText();
        }

        sb.append("    <div class=\"mb-1\">\n");
        sb
            .append("      <button class=\"btn btn-outline-")
            .append(btnColor)
            .append("\" onclick=\"document.getElementById('")
            .append(dialogId)
            .append("').close(); ")
            .append(cancelCallback)
            .append("(false);\">")
            .append(cancelLabel)
            .append("</button>\n");
        sb
            .append("      <button class=\"btn btn-outline-")
            .append(btnColor)
            .append("\" onclick=\"document.getElementById('")
            .append(dialogId)
            .append("').close(); ")
            .append(okCallback)
            .append("(true);\">")
            .append(okLabel)
            .append("</button>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n");
        sb.append("</dialog>\n");

        return sb.toString();
    }
}
