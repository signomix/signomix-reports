package com.signomix.reports.domain.charts;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
