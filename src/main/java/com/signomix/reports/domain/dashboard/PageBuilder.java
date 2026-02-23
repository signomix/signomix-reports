package com.signomix.reports.domain.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * PageBuilder generates HTML dashboard pages based on JSON dashboard definitions.
 * Supports responsive grid layout with 1 column for mobile and 10 columns for desktop.
 */
public class PageBuilder {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Generates HTML page from dashboard definition JSON
     * 
     * @param dashboardJson JSON string containing dashboard definition
     * @return HTML string with responsive grid layout
     * @throws JsonProcessingException if JSON parsing fails
     */
    public static String buildPage(String dashboardJson) throws JsonProcessingException {
        try {
            JsonNode rootNode = objectMapper.readTree(dashboardJson);
        
        String title = rootNode.path("title").asText("Dashboard");
        JsonNode widgetsNode = rootNode.path("widgets");
        JsonNode itemsNode = rootNode.path("items");
        
        StringBuilder html = new StringBuilder();
        
        // HTML header with Bootstrap 5
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>" + escapeHtml(title) + "</title>\n");
        html.append("    <!-- Bootstrap 5 CSS -->\n");
        html.append("    <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css\" rel=\"stylesheet\">\n");
        html.append("    <style>\n");
        html.append("        body { padding: 20px; }\n");
        html.append("        .dashboard-title { margin-bottom: 20px; }\n");
        html.append("        .widget-card { border: 1px solid #ddd; border-radius: 8px; padding: 15px; height: 100%; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("        .widget-title { font-weight: bold; margin-bottom: 10px; font-size: 1.1rem; }\n");
        html.append("        .widget-content { color: #666; }\n");
        html.append("        /* Custom 10-column grid for desktop */\n");
        html.append("        @media (min-width: 768px) {\n");
        html.append("            .col-10-1 { width: 10%; }\n");
        html.append("            .col-10-2 { width: 20%; }\n");
        html.append("            .col-10-3 { width: 30%; }\n");
        html.append("            .col-10-4 { width: 40%; }\n");
        html.append("            .col-10-5 { width: 50%; }\n");
        html.append("            .col-10-6 { width: 60%; }\n");
        html.append("            .col-10-7 { width: 70%; }\n");
        html.append("            .col-10-8 { width: 80%; }\n");
        html.append("            .col-10-9 { width: 90%; }\n");
        html.append("            .col-10-10 { width: 100%; }\n");
        html.append("        }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        // Dashboard title
        html.append("    <div class=\"dashboard-title\">\n");
        html.append("        <h1>" + escapeHtml(title) + "</h1>\n");
        html.append("    </div>\n");
        
        // Desktop grid (10 columns)
        html.append("    <div class=\"d-none d-md-block\">\n");
        html.append("        <div class=\"row g-3\">\n");
        
        buildDesktopGrid(html, widgetsNode, itemsNode);
        
        html.append("        </div>\n");
        html.append("    </div>\n");
        
        // Mobile grid (1 column)
        html.append("    <div class=\"d-md-none\">\n");
        html.append("        <div class=\"row g-2\">\n");
        
        buildMobileGrid(html, widgetsNode, itemsNode);
        
        html.append("        </div>\n");
        html.append("    </div>\n");
        
        // Bootstrap JS bundle
        html.append("    <script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js\"></script>\n");
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
        } catch (IOException e) {
            throw new JsonProcessingException("Failed to parse dashboard JSON", e) {};
        }
    }
    
    private static void buildDesktopGrid(StringBuilder html, JsonNode widgetsNode, JsonNode itemsNode) {
        // Create a grid representation
        List<GridItem> gridItems = new ArrayList<>();
        
        for (int i = 0; i < itemsNode.size() && i < widgetsNode.size(); i++) {
            JsonNode itemNode = itemsNode.path(i);
            JsonNode widgetNode = widgetsNode.path(i);
            
            JsonNode el10Node = itemNode.path("_el10");
            int x = el10Node.path("x").asInt(0);
            int y = el10Node.path("y").asInt(0);
            int w = el10Node.path("w").asInt(1);
            int h = el10Node.path("h").asInt(1);
            
            String widgetTitle = getWidgetTitle(widgetNode);
            
            gridItems.add(new GridItem(x, y, w, h, widgetTitle));
        }
        
        // Sort by row (y) then column (x)
        gridItems.sort(Comparator.comparingInt((GridItem item) -> item.y).thenComparingInt(item -> item.x));
        
        // Group by rows
        int currentRow = 0;
        while (!gridItems.isEmpty()) {
            List<GridItem> rowItems = new ArrayList<>();
            for (GridItem item : gridItems) {
                if (item.y == currentRow) {
                    rowItems.add(item);
                }
            }
            
            if (!rowItems.isEmpty()) {
                html.append("            <div class=\"row mb-3\">\n");
                
                // Create columns for this row
                for (GridItem item : rowItems) {
                    String colClass = "col-10-" + item.w;
                    html.append("                <div class=\"" + colClass + "\" style=\"grid-column: span " + item.w + ";\">\n");
                    html.append("                    <div class=\"widget-card h-100\">\n");
                    html.append("                        <div class=\"widget-title\">" + escapeHtml(item.title) + "</div>\n");
                    html.append("                        <div class=\"widget-content\">Widget content</div>\n");
                    html.append("                    </div>\n");
                    html.append("                </div>\n");
                }
                
                html.append("            </div>\n");
                gridItems.removeAll(rowItems);
            }
            
            currentRow++;
        }
    }
    
    private static void buildMobileGrid(StringBuilder html, JsonNode widgetsNode, JsonNode itemsNode) {
        List<MobileWidget> mobileWidgets = new ArrayList<>();
        
        for (int i = 0; i < itemsNode.size() && i < widgetsNode.size(); i++) {
            JsonNode widgetNode = widgetsNode.path(i);
            JsonNode itemNode = itemsNode.path(i);
            
            String mobileSize = widgetNode.path("mobile_size").asText("1");
            
            // Skip if mobile_size is 0 (hidden on mobile)
            if ("0".equals(mobileSize)) {
                continue;
            }
            
            int mobilePosition = widgetNode.path("mobile_position").asInt(-1);
            int height = Integer.parseInt(mobileSize);
            String widgetTitle = getWidgetTitle(widgetNode);
            
            mobileWidgets.add(new MobileWidget(mobilePosition, height, widgetTitle, i));
        }
        
        // Sort by mobile_position if specified, otherwise by original index
        mobileWidgets.sort(Comparator
            .comparingInt((MobileWidget w) -> w.mobilePosition >= 0 ? w.mobilePosition : Integer.MAX_VALUE)
            .thenComparingInt(w -> w.originalIndex));
        
        // Generate mobile widgets
        for (MobileWidget widget : mobileWidgets) {
            html.append("            <div class=\"col-12 mb-2\">\n");
            html.append("                <div class=\"widget-card\">\n");
            html.append("                    <div class=\"widget-title\">" + escapeHtml(widget.title) + "</div>\n");
            html.append("                    <div class=\"widget-content\">Widget content</div>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
        }
    }
    
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
    private static class GridItem {
        int x, y, w, h;
        String title;
        
        GridItem(int x, int y, int w, int h, String title) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.title = title;
        }
    }
    
    /**
     * Represents a widget for mobile layout
     */
    private static class MobileWidget {
        int mobilePosition;
        int height;
        String title;
        int originalIndex;
        
        MobileWidget(int mobilePosition, int height, String title, int originalIndex) {
            this.mobilePosition = mobilePosition;
            this.height = height;
            this.title = title;
            this.originalIndex = originalIndex;
        }
    }
}