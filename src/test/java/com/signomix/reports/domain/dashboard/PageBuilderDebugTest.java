package com.signomix.reports.domain.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PageBuilderDebugTest {
    
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
            
            PageBuilder pageBuilder = new PageBuilder();
            String html = pageBuilder.buildPage(null, dashboardJson);
            
            // Save to file for inspection
            Files.write(Paths.get("target/height-test.html"), html.getBytes());
            System.out.println("HTML saved to target/height-test.html");
            
            // Check for nested grid structure
            boolean hasNestedGrid = html.contains("row h-100 g-0");
            int nestedGridCount = countOccurrences(html, "row h-100 g-0");
            
            System.out.println("\nAnalysis:");
            System.out.println("  Has nested grid: " + hasNestedGrid);
            System.out.println("  Nested grid count: " + nestedGridCount);
            System.out.println("  Widget 2 (h=2) present: " + html.contains("Widget 2 (h=2)"));
            System.out.println("  Widget 3 (h=3) present: " + html.contains("Widget 3 (h=3)"));
            
            // Find widget 2 section
            int widget2Index = html.indexOf("Widget 2 (h=2)");
            if (widget2Index != -1) {
                int start = Math.max(0, widget2Index - 500);
                int end = Math.min(html.length(), widget2Index + 500);
                System.out.println("\nContext around Widget 2:");
                System.out.println(html.substring(start, end));
            }
            
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
