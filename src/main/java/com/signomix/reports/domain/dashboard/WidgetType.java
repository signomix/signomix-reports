package com.signomix.reports.domain.dashboard;

public enum WidgetType {
    chart,
    text,
    symbol,
    image,
    led;

    public static WidgetType fromString(String type) {
        for (WidgetType widgetType : WidgetType.values()) {
            if (widgetType.name().equalsIgnoreCase(type)) {
                return widgetType;
            }
        }
        return null;
    }
}
