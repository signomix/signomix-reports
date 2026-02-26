package com.signomix.reports.domain.dashboard;

public class WidgetBuilder {

    public static String buildWidget(WidgetType type) {
        switch (type) {
            case text:
                return buildTextWidget("Sample text");
            default:
                break;
        }
        return "";
    }

    public static String buildTextWidget(String text) {
        return text;
    }

}
