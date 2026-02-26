package com.signomix.reports.domain.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Comprehensive test demonstrating the height feature for PageBuilder.
 * This test creates a dashboard with widgets of various heights and validates
 * that the HTML output correctly implements nested grids for widgets with h > 1.
 */
public class PageBuilderComprehensiveTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== Comprehensive PageBuilder Height Test ===\n");
            
            // Create a dashboard with widgets of different heights
            String dashboardJson = createTestDashboard();
            
            String html = PageBuilder.buildPage(dashboardJson);
            
            // Save HTML for manual inspection
            Files.write(Paths.get("target/comprehensive-test.html"), html.getBytes());
            System.out.println("HTML saved to target/comprehensive-test.html\n");
            
            // Validate the HTML structure
            validateHtml(html);
            
            System.out.println("\n=== All Tests Passed! ===");
            System.out.println("The PageBuilder now correctly supports widgets with height > 1");
            System.out.println("by creating nested grids with the appropriate number of rows.");
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static String createTestDashboard() {
        return "{"
            + "\"title\":\"Comprehensive Height Test\","
            + "\"widgets\":["
            + "    {\"title\":\"Header Widget (h=1)\", \"mobile_size\":\"1\"},"
            + "    {\"title\":\"Medium Widget (h=2)\", \"mobile_size\":\"1\"},"
            + "    {\"title\":\"Large Widget (h=3)\", \"mobile_size\":\"1\"},"
            + "    {\"title\":\"Tall Widget (h=4)\", \"mobile_size\":\"1\"},"
            + "    {\"title\":\"Footer Widget (h=1)\", \"mobile_size\":\"1\"}"
            + "],"
            + "\"items\":["
            + "    {\"_el10\":{\"x\":0,\"y\":0,\"w\":12,\"h\":1}},"  // Full width header
            + "    {\"_el10\":{\"x\":0,\"y\":1,\"w\":4,\"h\":2}},"  // Medium widget
            + "    {\"_el10\":{\"x\":4,\"y\":1,\"w\":4,\"h\":3}},"  // Large widget
            + "    {\"_el10\":{\"x\":8,\"y\":1,\"w\":4,\"h\":4}},"  // Tall widget
            + "    {\"_el10\":{\"x\":0,\"y\":2,\"w\":12,\"h\":1}}"  // Full width footer
            + "]"
            + "}";
    }
    
    private static void validateHtml(String html) {
        System.out.println("Validating HTML structure...\n");
        
        // Check basic structure
        check(html.contains("Comprehensive Height Test"), "Dashboard title");
        check(html.contains("Header Widget (h=1)"), "Header widget");
        check(html.contains("Medium Widget (h=2)"), "Medium widget");
        check(html.contains("Large Widget (h=3)"), "Large widget");
        check(html.contains("Tall Widget (h=4)"), "Tall widget");
        check(html.contains("Footer Widget (h=1)"), "Footer widget");
        
        // Check for nested grids (should be 3: h=2, h=3, h=4)
        int nestedGridCount = countOccurrences(html, "row h-100 g-0");
        check(nestedGridCount == 3, "Nested grid count (expected 3 for h>1 widgets)");
        
        // Validate that widgets with h>1 have nested grids
        check(html.contains("Medium Widget (h=2)") && findNestedGridBefore(html, "Medium Widget (h=2)"), 
               "Medium Widget (h=2) has nested grid");
        check(html.contains("Large Widget (h=3)") && findNestedGridBefore(html, "Large Widget (h=3)"), 
               "Large Widget (h=3) has nested grid");
        check(html.contains("Tall Widget (h=4)") && findNestedGridBefore(html, "Tall Widget (h=4)"), 
               "Tall Widget (h=4) has nested grid");
        
        // Validate that single-row widgets don't have nested grids
        check(!findNestedGridBefore(html, "Header Widget (h=1)"), 
               "Header Widget (h=1) has NO nested grid");
        check(!findNestedGridBefore(html, "Footer Widget (h=1)"), 
               "Footer Widget (h=1) has NO nested grid");
        
        System.out.println("  ✓ Header Widget (h=1) - no nested grid");
        System.out.println("  ✓ Medium Widget (h=2) - has nested grid");
        System.out.println("  ✓ Large Widget (h=3) - has nested grid");
        System.out.println("  ✓ Tall Widget (h=4) - has nested grid");
        System.out.println("  ✓ Footer Widget (h=1) - no nested grid");
        
        System.out.println("\n✓ All validations passed!");
    }
    
    private static boolean findNestedGridBefore(String html, String widgetTitle) {
        int widgetIndex = html.indexOf(widgetTitle);
        if (widgetIndex == -1) return false;
        
        // Look backwards from the widget title to find if there's a nested grid
        int searchStart = Math.max(0, widgetIndex - 500);
        String beforeWidget = html.substring(searchStart, widgetIndex);
        
        return beforeWidget.contains("row h-100 g-0");
    }
    
    private static void check(boolean condition, String description) {
        if (!condition) {
            System.err.println("ERROR: " + description + " failed");
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
