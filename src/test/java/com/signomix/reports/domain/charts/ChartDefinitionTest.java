package com.signomix.reports.domain.charts;

import com.signomix.common.db.ReportResult;
import com.signomix.common.db.Dataset;
import com.signomix.common.db.DatasetRow;
import com.signomix.common.db.DatasetHeader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

class ChartDefinitionTest {

    @Test
    void testSetReportData() {
        // Create a ChartDefinition instance
        ChartDefinition chartDef = new ChartDefinition();
        
        // Create a mock ReportResult with sample data similar to report-device.json
        ReportResult reportResult = new ReportResult();
        
        // Create dataset
        Dataset dataset = new Dataset();
        dataset.eui = "123456";
        dataset.name = "dataset0";
        dataset.size = 3L;
        
        // Create dataset rows with timestamp and values
        List<DatasetRow> dataRows = new ArrayList<>();
        
        DatasetRow row1 = new DatasetRow();
        row1.timestamp = 1721252572735L;
        row1.values = new ArrayList<>();
        row1.values.add(26.348);
        row1.values.add(57.098);
        dataRows.add(row1);
        
        DatasetRow row2 = new DatasetRow();
        row2.timestamp = 1721252573735L;
        row2.values = new ArrayList<>();
        row2.values.add(24.926);
        row2.values.add(57.437);
        dataRows.add(row2);
        
        DatasetRow row3 = new DatasetRow();
        row3.timestamp = 1721252574735L;
        row3.values = new ArrayList<>();
        row3.values.add(22.820);
        row3.values.add(54.840);
        dataRows.add(row3);
        
        dataset.data = new ArrayList<>(dataRows);
        
        // Create dataset header
        DatasetHeader header = new DatasetHeader();
        header.name = "dataset0";
        header.columns = new ArrayList<>();
        header.columns.add("temperature");
        header.columns.add("humidity");
        
        // Add dataset and header to report result
        reportResult.datasets = new ArrayList<>();
        reportResult.datasets.add(dataset);
        
        reportResult.headers = new ArrayList<>();
        reportResult.headers.add(header);
        
        // Call the method to test
        chartDef.setReportData(reportResult);
        
        // Verify the results
        assertNotNull(chartDef.data);
        assertEquals(2, chartDef.data.size()); // Should have 2 series (temperature and humidity)
        
        assertNotNull(chartDef.values);
        assertEquals(2, chartDef.values.size()); // Should have 2 series
        
        assertNotNull(chartDef.measurementNames);
        assertEquals(2, chartDef.measurementNames.size());
        assertEquals("temperature", chartDef.measurementNames.get(0));
        assertEquals("humidity", chartDef.measurementNames.get(1));
        
        // Verify data points for first series (temperature)
        assertEquals(3, chartDef.data.get(0).size());
        assertEquals(1721252572735L, chartDef.data.get(0).get(0).timestamp);
        assertEquals(26.348, chartDef.data.get(0).get(0).value);
        
        assertEquals(1721252573735L, chartDef.data.get(0).get(1).timestamp);
        assertEquals(24.926, chartDef.data.get(0).get(1).value);
        
        assertEquals(1721252574735L, chartDef.data.get(0).get(2).timestamp);
        assertEquals(22.820, chartDef.data.get(0).get(2).value);
        
        // Verify data points for second series (humidity)
        assertEquals(3, chartDef.data.get(1).size());
        assertEquals(1721252572735L, chartDef.data.get(1).get(0).timestamp);
        assertEquals(57.098, chartDef.data.get(1).get(0).value);
        
        assertEquals(1721252573735L, chartDef.data.get(1).get(1).timestamp);
        assertEquals(57.437, chartDef.data.get(1).get(1).value);
        
        assertEquals(1721252574735L, chartDef.data.get(1).get(2).timestamp);
        assertEquals(54.840, chartDef.data.get(1).get(2).value);
        
        // Verify values structure
        assertEquals(3, chartDef.values.get(0).size());
        assertEquals(26.348, chartDef.values.get(0).get(0));
        assertEquals(24.926, chartDef.values.get(0).get(1));
        assertEquals(22.820, chartDef.values.get(0).get(2));
        
        assertEquals(3, chartDef.values.get(1).size());
        assertEquals(57.098, chartDef.values.get(1).get(0));
        assertEquals(57.437, chartDef.values.get(1).get(1));
        assertEquals(54.840, chartDef.values.get(1).get(2));
        
        // Verify line colors were set
        assertFalse(chartDef.lineColors.isEmpty());
        assertEquals(2, chartDef.lineColors.size()); // Should have colors for both series
    }
    
    @Test
    void testSetReportDataWithNull() {
        ChartDefinition chartDef = new ChartDefinition();
        chartDef.setReportData((ReportResult) null);
        
        // Should not throw exception and should leave data structures empty
        assertTrue(chartDef.data.isEmpty());
        assertTrue(chartDef.values.isEmpty());
        assertTrue(chartDef.measurementNames.isEmpty());
    }
    
    @Test
    void testSetReportDataWithEmptyDataset() {
        ChartDefinition chartDef = new ChartDefinition();
        
        ReportResult reportResult = new ReportResult();
        reportResult.datasets = new ArrayList<>();
        reportResult.datasets.add(new Dataset()); // Empty dataset
        
        chartDef.setReportData(reportResult);
        
        // Should handle empty dataset gracefully
        assertTrue(chartDef.data.isEmpty());
        assertTrue(chartDef.values.isEmpty());
        assertTrue(chartDef.measurementNames.isEmpty());
    }

    @Test
    void testSetReportDataWithJsonString() throws Exception {
        // Read the JSON file content
        java.nio.file.Path path = java.nio.file.Paths.get("doc/report-device.json");
        String jsonContent = new String(java.nio.file.Files.readAllBytes(path));
        
        // Create ChartDefinition and call the new method
        ChartDefinition chartDef = new ChartDefinition();
        chartDef.setReportData(jsonContent);
        
        // Verify the results - should be the same as the manual test
        assertNotNull(chartDef.data);
        assertEquals(2, chartDef.data.size()); // Should have 2 series (temperature and humidity)
        
        assertNotNull(chartDef.values);
        assertEquals(2, chartDef.values.size()); // Should have 2 series
        
        assertNotNull(chartDef.measurementNames);
        assertEquals(2, chartDef.measurementNames.size());
        assertEquals("temperature", chartDef.measurementNames.get(0));
        assertEquals("humidity", chartDef.measurementNames.get(1));
        
        // Verify we have 10 data points (as in the JSON file)
        assertEquals(10, chartDef.data.get(0).size());
        assertEquals(10, chartDef.data.get(1).size());
        
        // Verify first data point
        assertEquals(1721252572735L, chartDef.data.get(0).get(0).timestamp);
        assertEquals(26.3484803182423, chartDef.data.get(0).get(0).value);
        
        assertEquals(1721252572735L, chartDef.data.get(1).get(0).timestamp);
        assertEquals(57.098673317433686, chartDef.data.get(1).get(0).value);
        
        // Verify last data point
        assertEquals(1721252581735L, chartDef.data.get(0).get(9).timestamp);
        assertEquals(29.709664424538644, chartDef.data.get(0).get(9).value);
        
        assertEquals(1721252581735L, chartDef.data.get(1).get(9).timestamp);
        assertEquals(53.782570188005934, chartDef.data.get(1).get(9).value);
    }

    @Test
    void testSetReportDataWithNullJsonString() {
        ChartDefinition chartDef = new ChartDefinition();
        chartDef.setReportData((String) null);
        
        // Should handle null gracefully
        assertTrue(chartDef.data.isEmpty());
        assertTrue(chartDef.values.isEmpty());
        assertTrue(chartDef.measurementNames.isEmpty());
    }

    @Test
    void testSetReportDataWithEmptyJsonString() {
        ChartDefinition chartDef = new ChartDefinition();
        chartDef.setReportData("");
        
        // Should handle empty string gracefully
        assertTrue(chartDef.data.isEmpty());
        assertTrue(chartDef.values.isEmpty());
        assertTrue(chartDef.measurementNames.isEmpty());
    }

    @Test
    void testSetReportDataWithInvalidJsonString() {
        ChartDefinition chartDef = new ChartDefinition();
        chartDef.setReportData("{invalid json");
        
        // Should handle invalid JSON gracefully without throwing exception
        assertTrue(chartDef.data.isEmpty());
        assertTrue(chartDef.values.isEmpty());
        assertTrue(chartDef.measurementNames.isEmpty());
    }
}