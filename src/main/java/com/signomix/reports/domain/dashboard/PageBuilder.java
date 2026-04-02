package com.signomix.reports.domain.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signomix.common.User;
import com.signomix.common.db.DashboardDao;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.gui.Dashboard;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * PageBuilder generates HTML dashboard pages based on JSON dashboard
 * definitions.
 * Supports responsive grid layout with 1 column for mobile and configurable
 * columns (10 or 12) for desktop.
 */
@ApplicationScoped
public class PageBuilder {

    @Inject
    Logger logger;

    @Inject
    WidgetBuilder widgetBuilder;

    @DataSource("oltp")
    AgroalDataSource oltpDs;

    DashboardDao dashboardDao;

    @ConfigProperty(name = "signomix.organization.default")
    Long defaultOrganizationId;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String COPYRIGHT =
        "<span style=\"font-size: 1.0rem;\" >created with&nbsp;</span><a style=\"font-size: 1.0rem;\" href=\"https://signomix.com\" target=\"_blank\">Signomix IoT platform</a>";

    void onStart(@Observes StartupEvent ev) {
        dashboardDao = new DashboardDao();
        dashboardDao.setDatasource(oltpDs);
    }

    /**
     * Generates HTML page from dashboard definition JSON with default 10 columns
     * and header included
     *
     * @param dashboardJson JSON string containing dashboard definition
     * @return HTML string with responsive grid layout
     * @throws JsonProcessingException if JSON parsing fails
     */
    public String buildPage(
        User user,
        String dashboardJson,
        boolean header,
        boolean title
    ) throws JsonProcessingException {
        return buildPage(user, dashboardJson, 10, header, title, "UTC");
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
    public String buildPage(
        User user,
        String dashboardJson,
        int columns,
        boolean withHeader,
        boolean withTitle,
        String timeZone
    ) throws JsonProcessingException {
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
                html.append(
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                );
                html.append("    <title>" + escapeHtml(title) + "</title>\n");
                html.append("    <!-- Bootstrap 5 CSS -->\n");
                html.append(
                    "    <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css\" rel=\"stylesheet\">\n"
                );
                html.append(
                    "    <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/bootstrap-icons@1.13.1/font/bootstrap-icons.min.css\">\n"
                );
                html.append("    <style>\n");
                html.append("     body { padding: 20px; }\n");
                html.append("     .dashboard-title { margin-bottom: 20px; }\n");
                html.append(
                    "     .widget-card { border: 1px solid #ddd; border-radius: 8px; padding: 10px; height: 100%; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n"
                );
                html.append(
                    "    .widget-title {  margin-bottom: 10px; font-size: 1.0rem; }\n"
                );
                html.append("    .dashboard-footer { font-size: .5rem; }\n");
                html.append(
                    "    .widget-content { font-weight: bold; font-size: 1.6rem; color: #000000; }\n"
                );
                html.append(
                    "    .widget-info { font-size: 1rem; color: #000000; margin-bottom: 5px; }\n"
                );
                html.append(
                    "    .widget-no-data { font-size: 1.0rem; color: #868585; }\n"
                );
                html.append(
                    "    .widget-error { font-size: 1.0rem; color: #ff0000; }\n"
                );
                html.append("     /* Custom 10-column grid for desktop */\n");
                html.append("     @media (min-width: 768px) {\n");
                for (int i = 1; i <= columns; i++) {
                    int percentage = (i * 100) / columns;
                    html.append(
                        "            .col-" +
                            columns +
                            "-" +
                            i +
                            " { width: " +
                            percentage +
                            "%; }\n"
                    );
                }
                html.append("     }\n");
                html.append("    </style>\n");
                html.append("</head>\n");
                html.append("<body>\n");
            }

            // Dashboard title
            if (withTitle) {
                html.append("    <div class=\"dashboard-title\">\n");
                html.append("        <h1>" + escapeHtml(title) + "</h1>\n");
                html.append("    </div>\n");
            }

            // Desktop grid (configurable columns)
            html.append("    <div class=\"d-none d-md-block\">\n");
            html.append("        <div class=\"row no-gutters g-3\">\n");

            buildDesktopGrid(
                user,
                html,
                widgetsNode,
                itemsNode,
                columns,
                timeZone
            );

            html.append("        </div>\n");
            html.append("    </div>\n");

            // Mobile grid (1 column)
            html.append("    <div class=\"d-md-none\">\n");
            html.append("        <div class=\"row no-gutters g-2\">\n");

            buildMobileGrid(user, html, widgetsNode, itemsNode, timeZone);

            html.append("        </div>\n");
            html.append("    </div>\n");

            // Copyright footer
            html.append("    <div class=\"dashboard-footer\">\n");
            html.append("        <h1>" + COPYRIGHT + "</h1>\n");
            html.append("    </div>\n");

            if (withHeader) {
                // Bootstrap JS bundle
                html.append(
                    "    <script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js\"></script>\n"
                );
                html.append("</body>\n");
                html.append("</html>");
            }
            return html.toString();
        } catch (IOException e) {
            throw new JsonProcessingException(
                "Failed to parse dashboard JSON",
                e
            ) {};
        }
    }

    public String buildPageById(
        User user,
        String id,
        boolean withHeader,
        boolean withTitle,
        String timeZone
    ) throws JsonProcessingException {
        try {
            // Load dashboard definition from file based on ID
            String dashboardJson = loadDashboardDefinitionById(user, id);
            if (dashboardJson == null) {
                throw new JsonProcessingException(
                    "Dashboard definition not found for ID: " + id
                ) {};
            }
            return buildPage(
                user,
                dashboardJson,
                10,
                withHeader,
                withTitle,
                timeZone
            );
        } catch (IOException e) {
            throw new JsonProcessingException(
                "Failed to load dashboard definition for ID: " + id,
                e
            ) {};
        }
    }

    private void buildDesktopGrid(
        User user,
        StringBuilder html,
        JsonNode widgetsNode,
        JsonNode itemsNode,
        int columns,
        String timeZone
    ) {
        // Create a grid representation
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
        widgets.sort(
            Comparator.comparingInt((Widget item) -> item.y).thenComparingInt(
                item -> item.x
            )
        );

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
                html.append(
                    "            <div class=\"row no-gutters mb-3\">\n"
                );

                // Create columns for this row
                for (Widget item : rowItems) {
                    String colClass = "col-" + columns + "-" + item.w;
                    html.append(
                        "                <div class=\"" +
                            colClass +
                            "\" style=\"grid-column: span " +
                            item.w +
                            ";\">\n"
                    );

                    // If widget height is greater than 1, create a nested grid
                    if (item.h > 1) {
                        html.append(
                            "                    <div class=\"widget-card h-100\">\n"
                        );
                        html.append(
                            "                        <div class=\"row h-100 g-0\">\n"
                        );

                        // First row of the nested grid - contains the widget content
                        html.append(
                            "                            <div class=\"col-12 mb-2\">\n"
                        );
                        html.append(getWidgetContent(user, item, timeZone));
                        html.append("                            </div>\n");

                        // Additional rows for the remaining height
                        for (int row = 1; row < item.h; row++) {
                            html.append(
                                "                            <div class=\"col-12 mb-2\">\n"
                            );
                            html.append(
                                "                                <div class=\"widget-content\">&nbsp;</div>\n"
                            );
                            html.append("                            </div>\n");
                        }

                        html.append("                        </div>\n");
                        html.append("                    </div>\n");
                    } else {
                        // Standard single-row widget
                        html.append(
                            "                    <div class=\"widget-card h-100\">\n"
                        );
                        html.append(getWidgetContent(user, item, timeZone));
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

    private void buildMobileGrid(
        User user,
        StringBuilder html,
        JsonNode widgetsNode,
        JsonNode itemsNode,
        String timeZone
    ) {
        List<Widget> widgets = new ArrayList<>();

        for (int i = 0; i < itemsNode.size() && i < widgetsNode.size(); i++) {
            JsonNode widgetNode = widgetsNode.path(i);
            JsonNode itemNode = itemsNode.path(i);

            String mSize = widgetNode.path("mobile_size").asText("1");
            int mobileSize = 0;
            try {
                mobileSize = Integer.parseInt(mSize);
            } catch (NumberFormatException e) {}
            if (mobileSize == 0) {
                continue; // Skip if mobile_size is 0 (hidden on mobile)
            }

            widgets.add(new Widget(widgetNode, itemNode, i));
        }

        widgets.sort(
            Comparator.comparingInt((Widget w) ->
                w.mobilePosition >= 0 ? w.mobilePosition : Integer.MAX_VALUE
            ).thenComparingInt(w -> w.originalIndex)
        );

        // Generate mobile widgets
        for (Widget widget : widgets) {
            html.append("            <div class=\"col-12 mb-2\">\n");
            html.append("                <div class=\"widget-card\">\n");
            html.append(getWidgetContent(user, widget, timeZone));
            html.append("                </div>\n");
            html.append("            </div>\n");
        }
    }

    private String getWidgetContent(User user, Widget item, String timeZone) {
        return widgetBuilder.buildWidget(user, item, timeZone);
    }

    private String getWidgetTitle(JsonNode widgetNode) {
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

    private String getWidgetType(JsonNode widgetNode) {
        String type = widgetNode.path("type").asText();
        if (type == null || type.isEmpty()) {
            type = "unknown";
        }
        return type;
    }

    private String escapeHtml(String text) {
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

    private String loadDashboardDefinitionById(User user, String id)
        throws IOException {
        Dashboard dashboard = null;
        try {
            dashboard = dashboardDao.getDashboard(id);
        } catch (IotDatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (dashboard == null) {
            return null;
        }
        // Check if user has access to this dashboard:
        // - user is the owner of the dashboard (dashboard.getUserID() == user.uid)
        // - or user organization is not default one and user belongs to the same organization as the dashboard
        if (
            !(dashboard.getUserID().equals(user.uid)) ||
            (dashboard.getOrganizationId() != defaultOrganizationId &&
                dashboard.getOrganizationId() == user.organization.longValue())
        ) {
            return null; // User does not have access
        }
        // convert dashboard definition to JSON string
        String dashboardJson = objectMapper.writeValueAsString(dashboard);
        return dashboardJson;
    }
}
