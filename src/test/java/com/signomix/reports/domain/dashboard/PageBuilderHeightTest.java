package com.signomix.reports.domain.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;

public class PageBuilderHeightTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("Testing PageBuilder with widgets of different heights...");
            
            // Test case: Dashboard with widgets of different heights
            String dashboardJson = "{"
                + "\"title\":\"Height Test Dashboard\","
                + "\"widgets\":["
                + "    {\"title\":\"Widget 1 (h=1)\", \"mobile_size\":\"1\"},"
                + "    {\"title\":\"Widget 2 (h=2)\", \"mobile_size\":\"1\"},"
                + "    {\"title\":\"Widget 3 (h=3)\", \"mobile_size\":\"1\"},"
                + "    {\"title\":\"Widget 4 (h=1)\", \"mobile_size\":\"1\"}"
                + "],"
                + "\"items\":["
                + "    {\"_el10\":{\"x\":0,\"y\":0,\"w\":3,\"h\":1}},"
                + "    {\"_el10\":{\"x\":3,\"y\":0,\"w\":3,\"h\":2}},"
                + "    {\"_el10\":{\"x\":6,\"y\":0,\"w\":4,\"h\":3}},"
                + "    {\"_el10\":{\"x\":0,\"y\":1,\"w\":3,\"h\":1}}"
                + "]"
                + "}";
            
            String html = PageBuilder.buildPage(dashboardJson);
            
            // Basic validation
            if (html == null || html.isEmpty()) {
                System.err.println("ERROR: Generated HTML is empty");
                System.exit(1);
            }
            
            // Check for expected content
            boolean hasTitle = html.contains("Height Test Dashboard");
            boolean hasWidget1 = html.contains("Widget 1 (h=1)");
            boolean hasWidget2 = html.contains("Widget 2 (h=2)");
            boolean hasWidget3 = html.contains("Widget 3 (h=3)");
            boolean hasWidget4 = html.contains("Widget 4 (h=1)");
            
            // Check for nested grid structure for widgets with h > 1
            // Widget 2 should have a nested grid with 2 rows
            int widget2NestedGridCount = countOccurrences(html, "Widget 2 (h=2)");
            int widget2RowCount = countOccurrences(html, "Widget 2 (h=2)") > 0 ? 
                countOccurrences(html.substring(html.indexOf("Widget 2 (h=2)")), "col-12 mb-2") : 0;
            
            // Widget 3 should have a nested grid with 3 rows
            int widget3NestedGridCount = countOccurrences(html, "Widget 3 (h=3)");
            int widget3RowCount = countOccurrences(html, "Widget 3 (h=3)") > 0 ? 
                countOccurrences(html.substring(html.indexOf("Widget 3 (h=3)")), "col-12 mb-2") : 0;
            
            System.out.println("HTML validation:");
            System.out.println("  Title: " + hasTitle);
            System.out.println("  Widget 1: " + hasWidget1);
            System.out.println("  Widget 2: " + hasWidget2);
            System.out.println("  Widget 3: " + hasWidget3);
            System.out.println("  Widget 4: " + hasWidget4);
            System.out.println("  Widget 2 row count: " + widget2RowCount + " (expected: 2)");
            System.out.println("  Widget 3 row count: " + widget3RowCount + " (expected: 3)");
            
            if (!hasTitle || !hasWidget1 || !hasWidget2 || !hasWidget3 || !hasWidget4) {
                System.err.println("ERROR: HTML validation failed");
                System.exit(1);
            }
            
            if (widget2RowCount < 2) {
                System.err.println("ERROR: Widget 2 should have at least 2 rows in nested grid");
                System.exit(1);
            }
            
            if (widget3RowCount < 3) {
                System.err.println("ERROR: Widget 3 should have at least 3 rows in nested grid");
                System.exit(1);
            }
            
            System.out.println("\nSUCCESS: All height tests passed!");
            System.out.println("Generated HTML length: " + html.length() + " characters");
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}
