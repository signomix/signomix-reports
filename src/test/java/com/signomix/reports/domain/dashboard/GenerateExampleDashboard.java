package com.signomix.reports.domain.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GenerateExampleDashboard {
    
    public static void main(String[] args) {
        try {
            System.out.println("Generating dashboard from doc/dashboard_defiinition_example1.json...");
            
            // Read the example dashboard definition
            String dashboardJson = new String(Files.readAllBytes(
                Paths.get("doc/dashboard_defiinition_example1.json")));
            
            // Generate HTML using PageBuilder
            PageBuilder pageBuilder = new PageBuilder();
            String html = pageBuilder.buildPage(null,dashboardJson);
            
            // Save to output file
            Files.write(Paths.get("doc/dashboard_defiinition_example1.html"), html.getBytes());
            
            System.out.println("✅ SUCCESS: Dashboard generated and saved to doc/dashboard_defiinition_example1.html");
            System.out.println("📊 Generated HTML size: " + html.length() + " characters");
            System.out.println("📄 File location: doc/dashboard_defiinition_example1.html");
            
        } catch (JsonProcessingException e) {
            System.err.println("❌ ERROR: Failed to parse dashboard JSON");
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}