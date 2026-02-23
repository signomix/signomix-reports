package com.signomix.reports.domain.charts;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.signomix.common.db.Dataset;
import com.signomix.common.db.DatasetHeader;
import com.signomix.common.db.DatasetRow;
import com.signomix.common.db.ReportResult;

public class ChartDefinition {
    public String title;
    public String xAxisName;
    public String yAxisName;
    public String xAxisUnit;
    public String yAxisUnit;
    public List<List<ChartData>> data = new ArrayList<>();
    public List<List<Double>> values = new ArrayList<>();
    public List<String> measurementNames = new ArrayList<>();
    public List<String> lineColors = new ArrayList<>();
    public String fillColor;
    public int width = 800;
    public int height = 600;
    public Integer titleFontSize;
    public int chartFontSize = 12;
    public Double xAxisMin;
    public Double xAxisMax;

    public static final int MAX_SERIES = 6;
    public static final List<String> DEFAULT_COLORS = List.of(
            "#000000", "#0000FF", "#FF0000", "#00FF00", "#FFA500", "#800080"
    );

    public ChartDefinition() {
    }

    /**
     * Sets the chart data based on the provided ReportResult. 
     * It processes the first dataset and populates the chart data, values, and measurement names accordingly.
     * @param reportResult
     */
    public void setReportData(ReportResult reportResult) {
        if (reportResult == null || reportResult.datasets == null || reportResult.datasets.isEmpty()) {
            return;
        }

        // Get the first dataset (as per requirement: "ReportResult zawiera DataSet jednego źródła danych")
        Dataset dataset = reportResult.datasets.get(0);
        
        if (dataset == null || dataset.data == null || dataset.data.isEmpty()) {
            return;
        }

        // Clear existing data
        this.data.clear();
        this.values.clear();
        this.measurementNames.clear();

        // Get header information for measurement names
        if (reportResult.headers != null && !reportResult.headers.isEmpty()) {
            DatasetHeader header = reportResult.headers.get(0);
            if (header != null && header.columns != null) {
                this.measurementNames.addAll(header.columns);
            }
        }

        // Process each value series separately
        int numValuesPerRow = dataset.data.get(0).values.size();
        
        // Initialize data structures for each series
        for (int seriesIndex = 0; seriesIndex < numValuesPerRow; seriesIndex++) {
            this.data.add(new ArrayList<>());
            this.values.add(new ArrayList<>());
        }

        // Populate data and values
        for (DatasetRow row : dataset.data) {
            if (row.values != null) {
                for (int seriesIndex = 0; seriesIndex < row.values.size(); seriesIndex++) {
                    Object valueObj = row.values.get(seriesIndex);
                    
                    // Convert to double (handle different numeric types)
                    double value = 0.0;
                    if (valueObj instanceof Number) {
                        value = ((Number) valueObj).doubleValue();
                    }
                    
                    // Add to data structure
                    ChartData chartData = new ChartData(row.timestamp, value);
                    this.data.get(seriesIndex).add(chartData);
                    
                    // Add to values structure
                    this.values.get(seriesIndex).add(value);
                }
            }
        }

        // Set line colors if not already set
        if (this.lineColors.isEmpty() && !this.data.isEmpty()) {
            for (int i = 0; i < Math.min(this.data.size(), DEFAULT_COLORS.size()); i++) {
                this.lineColors.add(DEFAULT_COLORS.get(i));
            }
        }
    }

    public void setReportData(String jsonReportResult) {
        if (jsonReportResult == null || jsonReportResult.trim().isEmpty()) {
            return;
        }

        try {
            // Parse the JSON string into ReportResult object using ReportResult's static parse method
            ReportResult reportResult = ReportResult.parse(jsonReportResult);
            
            // Delegate to the existing setReportData method
            setReportData(reportResult);
            
        } catch (Exception e) {
            // Log the error and leave data structures unchanged
            // In a real application, you might want to log this error
            // For now, we'll silently fail to maintain existing behavior
        }
    }

    @JsonCreator
    public ChartDefinition(
            @JsonProperty("data") List<List<ChartData>> data,
            @JsonProperty("values") List<List<Double>> values,
            @JsonProperty("title") String title,
            @JsonProperty("xAxisName") String xAxisName,
            @JsonProperty("xAxisUnit") String xAxisUnit,
            @JsonProperty("yAxisName") String yAxisName,
            @JsonProperty("yAxisUnit") String yAxisUnit,
            @JsonProperty("measurementNames") List<String> measurementNames,
            @JsonProperty("width") int width,
            @JsonProperty("height") int height,
            @JsonProperty("titleFontSize") Integer titleFontSize,
            @JsonProperty("chartFontSize") int chartFontSize,
            @JsonProperty("lineColors") List<String> lineColors,
            @JsonProperty("fillColor") String fillColor,
            @JsonProperty("xAxisMin") Double xAxisMin,
            @JsonProperty("xAxisMax") Double xAxisMax) {
        if (data != null) {
            this.data = data;
        }
        if (values != null) {
            this.values = values;
        }
        this.title = title;
        this.xAxisName = xAxisName;
        this.xAxisUnit = xAxisUnit;
        this.yAxisName = yAxisName;
        this.yAxisUnit = yAxisUnit;
        if (measurementNames != null) {
            this.measurementNames = measurementNames;
        }
        if (width > 0) {
            this.width = width;
        }
        if (height > 0) {
            this.height = height;
        }
        this.titleFontSize = titleFontSize;
        if (chartFontSize > 0) {
            this.chartFontSize = chartFontSize;
        }
        if (lineColors != null) {
            this.lineColors = lineColors;
        }
        this.fillColor = fillColor;
        this.xAxisMin = xAxisMin;
        this.xAxisMax = xAxisMax;
    }
}
