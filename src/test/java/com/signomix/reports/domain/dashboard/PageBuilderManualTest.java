package com.signomix.reports.domain.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PageBuilderManualTest {
    
    public static void main(String[] args) {
        try {
            // Test 1: Load from file
            System.out.println("Testing PageBuilder with example dashboard...");
            String dashboardJson = new String(Files.readAllBytes(
                Paths.get("doc/dashboard_defiinition_example1.json")));
            
            String html = PageBuilder.buildPage(dashboardJson);
            
            // Basic validation
            if (html == null || html.isEmpty()) {
                System.err.println("ERROR: Generated HTML is empty");
                System.exit(1);
            }
            
            // Check for expected content
            boolean hasDoctype = html.contains("<!DOCTYPE html>");
            boolean hasTitle = html.contains("<title>IOTEMULATOR</title>");
            boolean hasBootstrap = html.contains("bootstrap");
            boolean hasWidgetCard = html.contains("widget-card");
            boolean hasDesktopGrid = html.contains("d-none d-md-block");
            boolean hasMobileGrid = html.contains("d-md-none");
            boolean hasTemperature = html.contains("temperature");
            boolean hasHumidity = html.contains("humidity");
            
            System.out.println("HTML validation:");
            System.out.println("  DOCTYPE: " + hasDoctype);
            System.out.println("  Title: " + hasTitle);
            System.out.println("  Bootstrap: " + hasBootstrap);
            System.out.println("  Widget card class: " + hasWidgetCard);
            System.out.println("  Desktop grid: " + hasDesktopGrid);
            System.out.println("  Mobile grid: " + hasMobileGrid);
            System.out.println("  Temperature widget: " + hasTemperature);
            System.out.println("  Humidity widget: " + hasHumidity);
            
            if (!hasDoctype || !hasTitle || !hasBootstrap || !hasWidgetCard || 
                !hasDesktopGrid || !hasMobileGrid || !hasTemperature || !hasHumidity) {
                System.err.println("ERROR: HTML validation failed");
                System.exit(1);
            }
            
            // Save to file
            Files.write(Paths.get("target/test-dashboard.html"), html.getBytes());
            System.out.println("\nSUCCESS: Test HTML saved to target/test-dashboard.html");
            System.out.println("Generated HTML length: " + html.length() + " characters");
            
            // Test 2: Simple dashboard
            System.out.println("\nTesting with simple dashboard...");
            String simpleJson = "{"
                + "\"title\":\"Test Dashboard\","
                + "\"widgets\":["
                + "    {\"title\":\"Widget 1\", \"mobile_size\":\"1\"},"
                + "    {\"title\":\"Widget 2\", \"mobile_size\":\"1\"}"
                + "],"
                + "\"items\":["
                + "    {\"_el10\":{\"x\":0,\"y\":0,\"w\":2,\"h\":1}},"
                + "    {\"_el10\":{\"x\":3,\"y\":0,\"w\":2,\"h\":1}}"
                + "]"
                + "}";
            
            String simpleHtml = PageBuilder.buildPage(simpleJson);
            
            boolean simpleHasTitle = simpleHtml.contains("Test Dashboard");
            boolean simpleHasWidget1 = simpleHtml.contains("Widget 1");
            boolean simpleHasWidget2 = simpleHtml.contains("Widget 2");
            
            System.out.println("Simple dashboard validation:");
            System.out.println("  Title: " + simpleHasTitle);
            System.out.println("  Widget 1: " + simpleHasWidget1);
            System.out.println("  Widget 2: " + simpleHasWidget2);
            
            if (!simpleHasTitle || !simpleHasWidget1 || !simpleHasWidget2) {
                System.err.println("ERROR: Simple dashboard validation failed");
                System.exit(1);
            }
            
            System.out.println("\nSUCCESS: All tests passed!");
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}