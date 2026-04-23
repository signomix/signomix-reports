package com.signomix.reports.domain.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;

public class PageBuilderHeightValidationTest {
    
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
            String html = pageBuilder.buildPage(null, dashboardJson, true, true, "UTC");
            
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
            
            // Check for nested grid structure
            boolean hasNestedGrid = html.contains("row h-100 g-0");
            int nestedGridCount = countOccurrences(html, "row h-100 g-0");
            
            // Widget 1 should NOT have a nested grid
            int widget1Start = html.indexOf("Widget 1 (h=1)");
            int widget1CardStart = html.lastIndexOf("<div class=\"widget-card h-100\"", widget1Start);
            int widget1CardEnd = html.indexOf("</div>\n                    </div>\n                </div>\n            </div>\n", widget1CardStart);
            String widget1Section = html.substring(widget1CardStart, widget1CardEnd);
            boolean widget1HasNoNestedGrid = !widget1Section.contains("row h-100 g-0");
            
            // Widget 2 should have a nested grid with 2 rows
            int widget2Start = html.indexOf("Widget 2 (h=2)");
            int widget2CardStart = html.lastIndexOf("<div class=\"widget-card h-100\"", widget2Start);
            int widget2CardEnd = html.indexOf("</div>\n                    </div>\n                </div>\n            </div>\n", widget2CardStart);
            String widget2Section = html.substring(widget2CardStart, widget2CardEnd);
            boolean widget2HasNestedGrid = widget2Section.contains("row h-100 g-0");
            int widget2RowCount = countOccurrences(widget2Section, "col-12 mb-2");
            
            // Widget 3 should have a nested grid with 3 rows
            int widget3Start = html.indexOf("Widget 3 (h=3)");
            int widget3CardStart = html.lastIndexOf("<div class=\"widget-card h-100\"", widget3Start);
            int widget3CardEnd = html.indexOf("</div>\n                    </div>\n                </div>\n            </div>\n", widget3CardStart);
            String widget3Section = html.substring(widget3CardStart, widget3CardEnd);
            boolean widget3HasNestedGrid = widget3Section.contains("row h-100 g-0");
            int widget3RowCount = countOccurrences(widget3Section, "col-12 mb-2");
            
            System.out.println("HTML validation:");
            System.out.println("  Title: " + hasTitle);
            System.out.println("  Widget 1: " + hasWidget1);
            System.out.println("  Widget 2: " + hasWidget2);
            System.out.println("  Widget 3: " + hasWidget3);
            System.out.println("  Widget 4: " + hasWidget4);
            System.out.println("  Has nested grids: " + hasNestedGrid);
            System.out.println("  Nested grid count: " + nestedGridCount + " (expected: 2 for h>1 widgets)");
            System.out.println("  Widget 1 has no nested grid: " + widget1HasNoNestedGrid);
            System.out.println("  Widget 2 has nested grid: " + widget2HasNestedGrid);
            System.out.println("  Widget 2 row count: " + widget2RowCount + " (expected: 2)");
            System.out.println("  Widget 3 has nested grid: " + widget3HasNestedGrid);
            System.out.println("  Widget 3 row count: " + widget3RowCount + " (expected: 3)");
            
            if (!hasTitle || !hasWidget1 || !hasWidget2 || !hasWidget3 || !hasWidget4) {
                System.err.println("ERROR: HTML validation failed");
                System.exit(1);
            }
            
            if (nestedGridCount != 2) {
                System.err.println("ERROR: Should have exactly 2 nested grids (for widgets with h>1)");
                System.exit(1);
            }
            
            if (!widget1HasNoNestedGrid) {
                System.err.println("ERROR: Widget 1 should NOT have a nested grid (h=1)");
                System.exit(1);
            }
            
            if (!widget2HasNestedGrid) {
                System.err.println("ERROR: Widget 2 should have a nested grid (h=2)");
                System.exit(1);
            }
            
            if (widget2RowCount != 2) {
                System.err.println("ERROR: Widget 2 should have exactly 2 rows in nested grid, found " + widget2RowCount);
                System.exit(1);
            }
            
            if (!widget3HasNestedGrid) {
                System.err.println("ERROR: Widget 3 should have a nested grid (h=3)");
                System.exit(1);
            }
            
            if (widget3RowCount != 3) {
                System.err.println("ERROR: Widget 3 should have exactly 3 rows in nested grid, found " + widget3RowCount);
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
