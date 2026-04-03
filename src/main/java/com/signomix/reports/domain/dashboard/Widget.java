package com.signomix.reports.domain.dashboard;

import com.fasterxml.jackson.databind.JsonNode;

class Widget {

    int x, y, w, h;
    int mobilePosition, mobileSize, height;
    String title;
    String type;
    int originalIndex;
    String query;
    String unitName;
    String description;
    String icon;
    String rule;
    String dev_id;
    String channel;

    Widget(JsonNode widgetsNode, JsonNode itemsNode, int originalIndex) {
        this.originalIndex = originalIndex;

        x = itemsNode.path("_el10").path("x").asInt(0);
        y = itemsNode.path("_el10").path("y").asInt(0);
        w = itemsNode.path("_el10").path("w").asInt(1);
        h = itemsNode.path("_el10").path("h").asInt(1);

        title = getWidgetTitle(widgetsNode);
        type = getWidgetType(widgetsNode);
        query = widgetsNode.path("query").asText();
        unitName = widgetsNode.path("unitName").asText();
        description = widgetsNode.path("description").asText();
        icon = widgetsNode.path("icon").asText();
        rule = widgetsNode.path("range").asText();
        dev_id = widgetsNode.path("dev_id").asText();
        channel = widgetsNode.path("channel").asText();
        String mSize = widgetsNode.path("mobile_size").asText("1");

        mobilePosition = widgetsNode.path("mobile_position").asInt(-1);
        try {
            height = Integer.parseInt(mSize);
        } catch (NumberFormatException e) {
            height = 1; // Default height if parsing fails
        }
        if (query == null || query.isEmpty()) {
            query =
                "report DqlReport eui " +
                dev_id +
                " channel " +
                channel +
                " last 1";
        }
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
}
