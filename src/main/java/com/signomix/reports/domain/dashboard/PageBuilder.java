package com.signomix.reports.domain.dashboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * PageBuilder generates HTML dashboard pages based on JSON dashboard
 * definitions.
 * Supports responsive grid layout with 1 column for mobile and configurable
 * columns (10 or 12) for desktop.
 */
public class PageBuilder {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generates HTML page from dashboard definition JSON with default 10 columns
     * and header included
     * 
     * @param dashboardJson JSON string containing dashboard definition
     * @return HTML string with responsive grid layout
     * @throws JsonProcessingException if JSON parsing fails
     */
    public static String buildPage(String dashboardJson) throws JsonProcessingException {
        return buildPage(dashboardJson, 10, true);
    }

    /**
     * Generates HTML page from dashboard definition JSON
     * 
     * @param dashboardJson JSON string containing dashboard definition
     * @param columns       Number of columns in the grid (10 or 12). Default is 10.
     * @param withHeader    Whether to include HTML header and Bootstrap CSS.
     *                      Default is true.
     * @return HTML string with responsive grid layout
     * @throws JsonProcessingException if JSON parsing fails
     */
    public static String buildPage(String dashboardJson, int columns, boolean withHeader)
            throws JsonProcessingException {
        try {
            JsonNode rootNode = objectMapper.readTree(dashboardJson);

            String title = rootNode.path("title").asText("Dashboard");
            JsonNode widgetsNode = rootNode.path("widgets");
            JsonNode itemsNode = rootNode.path("items");

            StringBuilder html = new StringBuilder();
            if (withHeader) {
                // HTML header with Bootstrap 5
                html.append("<!DOCTYPE html>\n");
                html.append("<html lang=\"en\">\n");
                html.append("<head>\n");
                html.append("    <meta charset=\"UTF-8\">\n");
                html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
                html.append("    <title>" + escapeHtml(title) + "</title>\n");
                html.append("    <!-- Bootstrap 5 CSS -->\n");
                html.append(
                        "    <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css\" rel=\"stylesheet\">\n");
                html.append("    <style>\n");
                html.append("        body { padding: 20px; }\n");
                html.append("        .dashboard-title { margin-bottom: 20px; }\n");
                html.append(
                        "        .widget-card { border: 1px solid #ddd; border-radius: 8px; padding: 15px; height: 100%; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
                html.append("        .widget-title { font-weight: bold; margin-bottom: 10px; font-size: 1.1rem; }\n");
                html.append("        .widget-content { color: #666; }\n");
                html.append("        /* Custom 10-column grid for desktop */\n");
                html.append("        @media (min-width: 768px) {\n");
                for (int i = 1; i <= columns; i++) {
                    int percentage = (i * 100) / columns;
                    html.append("            .col-" + columns + "-" + i + " { width: " + percentage + "%; }\n");
                }
                html.append("        }\n");
                html.append("    </style>\n");
                html.append("</head>\n");
                html.append("<body>\n");
            }

            // Dashboard title
            html.append("    <div class=\"dashboard-title\">\n");
            html.append("        <h1>" + escapeHtml(title) + "</h1>\n");
            html.append("    </div>\n");

            // Desktop grid (configurable columns)
            html.append("    <div class=\"d-none d-md-block\">\n");
            html.append("        <div class=\"row g-3\">\n");

            buildDesktopGrid(html, widgetsNode, itemsNode, columns);

            html.append("        </div>\n");
            html.append("    </div>\n");

            // Mobile grid (1 column)
            html.append("    <div class=\"d-md-none\">\n");
            html.append("        <div class=\"row g-2\">\n");

            buildMobileGrid(html, widgetsNode, itemsNode);

            html.append("        </div>\n");
            html.append("    </div>\n");

            if (withHeader) {
                // Bootstrap JS bundle
                html.append(
                        "    <script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js\"></script>\n");
                html.append("</body>\n");
                html.append("</html>");
            }
            return html.toString();
        } catch (IOException e) {
            throw new JsonProcessingException("Failed to parse dashboard JSON", e) {
            };
        }
    }

    private static void buildDesktopGrid(StringBuilder html, JsonNode widgetsNode, JsonNode itemsNode, int columns) {
        // Create a grid representation
        // List<GridItem> gridItems = new ArrayList<>();
        List<Widget> widgets = new ArrayList<>();

        for (int i = 0; i < itemsNode.size() && i < widgetsNode.size(); i++) {
            JsonNode itemNode = itemsNode.path(i);
            JsonNode widgetNode = widgetsNode.path(i);

            JsonNode elNode = itemNode.path("_el" + columns);
            int x = elNode.path("x").asInt(0);
            int y = elNode.path("y").asInt(0);
            int w = elNode.path("w").asInt(1);
            int h = elNode.path("h").asInt(1);

            String widgetTitle = getWidgetTitle(widgetNode);
            String widgetType = getWidgetType(widgetNode);

            // gridItems.add(new GridItem(x, y, w, h, widgetTitle, widgetType));
            widgets.add(new Widget(widgetNode, itemNode, i));
        }

        // Sort by row (y) then column (x)
        // gridItems.sort(Comparator.comparingInt((GridItem item) ->
        // item.y).thenComparingInt(item -> item.x));
        widgets.sort(Comparator.comparingInt((Widget item) -> item.y).thenComparingInt(item -> item.x));

        // Group by rows
        int currentRow = 0;
        // while (!gridItems.isEmpty()) {
        while (!widgets.isEmpty()) {
            List<Widget> rowItems = new ArrayList<>();
            for (Widget item : widgets) {
                if (item.y == currentRow) {
                    rowItems.add(item);
                }
            }

            if (!rowItems.isEmpty()) {
                html.append("            <div class=\"row mb-3\">\n");

                // Create columns for this row
                for (Widget item : rowItems) {
                    String colClass = "col-" + columns + "-" + item.w;
                    html.append("                <div class=\"" + colClass + "\" style=\"grid-column: span " + item.w
                            + ";\">\n");

                    // If widget height is greater than 1, create a nested grid
                    if (item.h > 1) {
                        html.append("                    <div class=\"widget-card h-100\">\n");
                        html.append("                        <div class=\"row h-100 g-0\">\n");

                        // First row of the nested grid - contains the widget content
                        html.append("                            <div class=\"col-12 mb-2\">\n");
                        html.append(getWidgetContent(item));
                        html.append("                            </div>\n");

                        // Additional rows for the remaining height
                        for (int row = 1; row < item.h; row++) {
                            html.append("                            <div class=\"col-12 mb-2\">\n");
                            html.append("                                <div class=\"widget-content\">&nbsp;</div>\n");
                            html.append("                            </div>\n");
                        }

                        html.append("                        </div>\n");
                        html.append("                    </div>\n");
                    } else {
                        // Standard single-row widget
                        html.append("                    <div class=\"widget-card h-100\">\n");
                        html.append(getWidgetContent(item));
                        html.append("                    </div>\n");
                    }

                    html.append("                </div>\n");
                }

                html.append("            </div>\n");
                widgets.removeAll(rowItems);
            }

            currentRow++;
        }
    }

    private static void buildMobileGrid(StringBuilder html, JsonNode widgetsNode, JsonNode itemsNode) {
        // List<MobileWidget> mobileWidgets = new ArrayList<>();
        List<Widget> widgets = new ArrayList<>();

        for (int i = 0; i < itemsNode.size() && i < widgetsNode.size(); i++) {
            JsonNode widgetNode = widgetsNode.path(i);
            JsonNode itemNode = itemsNode.path(i);

            /*
             * String mobileSize = widgetNode.path("mobile_size").asText("1");
             * 
             * // Skip if mobile_size is 0 (hidden on mobile)
             * if ("0".equals(mobileSize)) {
             * continue;
             * }
             * 
             * int mobilePosition = widgetNode.path("mobile_position").asInt(-1);
             * int height = Integer.parseInt(mobileSize);
             * String widgetTitle = getWidgetTitle(widgetNode);
             * String widgetType = getWidgetType(widgetNode);
             * 
             * mobileWidgets.add(new MobileWidget(mobilePosition, height, widgetTitle, i,
             * widgetType));
             */
            String mSize = widgetNode.path("mobile_size").asText("1");
            int mobileSize = 0;
            try {
                mobileSize = Integer.parseInt(mSize);
            } catch (NumberFormatException e) {
            }
            if (mobileSize == 0) {
                continue; // Skip if mobile_size is 0 (hidden on mobile)
            }

            widgets.add(new Widget(widgetNode, itemNode, i));
        }

        // Sort by mobile_position if specified, otherwise by original index
        // mobileWidgets.sort(Comparator
        // .comparingInt((MobileWidget w) -> w.mobilePosition >= 0 ? w.mobilePosition :
        // Integer.MAX_VALUE)
        // .thenComparingInt(w -> w.originalIndex));

        widgets.sort(Comparator
                .comparingInt((Widget w) -> w.mobilePosition >= 0 ? w.mobilePosition : Integer.MAX_VALUE)
                .thenComparingInt(w -> w.originalIndex));

        // Generate mobile widgets
        // for (MobileWidget widget : mobileWidgets) {
        for (Widget widget : widgets) {
            html.append("            <div class=\"col-12 mb-2\">\n");
            html.append("                <div class=\"widget-card\">\n");
            html.append(getWidgetContent(widget));
            html.append("                </div>\n");
            html.append("            </div>\n");
        }
    }

    /*
     * private static String getWidgetContent(GridItem item) {
     * StringBuilder content = new StringBuilder();
     * if (item.title != null && !item.title.isEmpty()) {
     * content.append("<div class=\"widget-title\">" + escapeHtml(item.title) +
     * "</div>\n");
     * }
     * content.append("<div class=\"widget-content\">");
     * content.append(WidgetBuilder.buildWidget(WidgetType.valueOf(item.type.
     * toLowerCase())));
     * content.append("</div>\n");
     * return content.toString();
     * }
     */

    private static String getWidgetContent(Widget item) {
        StringBuilder content = new StringBuilder();
        if (item.title != null && !item.title.isEmpty()) {
            content.append("<div class=\"widget-title\">" + escapeHtml(item.title) + "</div>\n");
        }
        content.append("<div class=\"widget-content\">");
        WidgetType widgetType;
        try {
            widgetType = WidgetType.valueOf(item.type.toLowerCase());
        } catch (IllegalArgumentException e) {
            widgetType = null; // Unknown type
        }
        if (widgetType != null) {
            content.append(WidgetBuilder.buildWidget(widgetType));
        } else {
            content.append("<div class=\"text-muted\">Unknown widget type: " + escapeHtml(item.type) + "</div>");
        }
        content.append("</div>\n");
        return content.toString();
    }

    /*
     * private static String getWidgetContent(MobileWidget item) {
     * StringBuilder content = new StringBuilder();
     * if (item.title != null && !item.title.isEmpty()) {
     * content.append("<div class=\"widget-title\">" + escapeHtml(item.title) +
     * "</div>\n");
     * }
     * content.append("<div class=\"widget-content\">");
     * content.append(WidgetBuilder.buildWidget(WidgetType.valueOf(item.type.
     * toLowerCase())));
     * content.append("</div>\n");
     * return content.toString();
     * }
     */

    private static String getWidgetTitle(JsonNode widgetNode) {
        // Try different fields for title
        String title = widgetNode.path("title").asText();
        if (title == null || title.isEmpty()) {
            title = widgetNode.path("name").asText();
        }
        if (title == null || title.isEmpty()) {
            title = widgetNode.path("channel").asText();
        }
        if (title == null || title.isEmpty()) {
            title = "Widget";
        }
        return title;
    }

    private static String getWidgetType(JsonNode widgetNode) {
        String type = widgetNode.path("type").asText();
        if (type == null || type.isEmpty()) {
            type = "unknown";
        }
        return type;
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

    /**
     * Represents a grid item for desktop layout
     */
    /*
     * private static class GridItem {
     * int x, y, w, h;
     * String title;
     * String type;
     * 
     * GridItem(int x, int y, int w, int h, String title, String type) {
     * this.x = x;
     * this.y = y;
     * this.w = w;
     * this.h = h;
     * this.title = title;
     * this.type = type;
     * }
     * }
     */

    /**
     * Represents a widget for mobile layout
     */
    /*
     * private static class MobileWidget {
     * int mobilePosition;
     * int height;
     * String title;
     * String type;
     * int originalIndex;
     * 
     * MobileWidget(int mobilePosition, int height, String title, int originalIndex,
     * String type) {
     * this.mobilePosition = mobilePosition;
     * this.height = height;
     * this.title = title;
     * this.originalIndex = originalIndex;
     * this.type = type;
     * }
     * }
     */

    private static class Widget {
        int x, y, w, h;
        int mobilePosition, mobileSize, height;
        String title;
        String type;
        int originalIndex;

        Widget(JsonNode widgetsNode, JsonNode itemsNode, int originalIndex) {
            this.originalIndex = originalIndex;

            x = itemsNode.path("_el10").path("x").asInt(0);
            y = itemsNode.path("_el10").path("y").asInt(0);
            w = itemsNode.path("_el10").path("w").asInt(1);
            h = itemsNode.path("_el10").path("h").asInt(1);

            title = getWidgetTitle(widgetsNode);
            type = getWidgetType(widgetsNode);
            String mSize = widgetsNode.path("mobile_size").asText("1");

            mobilePosition = widgetsNode.path("mobile_position").asInt(-1);
            try {
                height = Integer.parseInt(mSize);
            } catch (NumberFormatException e) {
                height = 1; // Default height if parsing fails
            }

        }

    }
}