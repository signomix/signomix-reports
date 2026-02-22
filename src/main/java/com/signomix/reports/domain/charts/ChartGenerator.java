package com.signomix.reports.domain.charts;

import java.awt.Color;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

public class ChartGenerator {

    private ChartDefinition def = new ChartDefinition();

    public ChartGenerator() {
    }

    public ChartGenerator addDataSet(List<ChartData> data) {
        if (this.def.data.size() < ChartDefinition.MAX_SERIES) {
            this.def.data.add(data);
        }
        return this;
    }

    public ChartGenerator addValueSet(List<Double> values) {
        if (this.def.values.size() < ChartDefinition.MAX_SERIES) {
            this.def.values.add(values);
        }
        return this;
    }

    public ChartGenerator setData(List<ChartData> data) {
        this.def.data.clear();
        this.def.data.add(data);
        return this;
    }

    public ChartGenerator setValues(List<Double> values) {
        this.def.values.clear();
        this.def.values.add(values);
        return this;
    }

    public ChartGenerator setMeasurementNames(List<String> measurementNames) {
        this.def.measurementNames = measurementNames;
        return this;
    }

    public ChartGenerator addMeasurementName(String measurementName) {
        if (this.def.measurementNames.size() < ChartDefinition.MAX_SERIES) {
            this.def.measurementNames.add(measurementName);
        }
        return this;
    }

    public ChartGenerator setLineColors(List<String> lineColors) {
        this.def.lineColors = lineColors;
        return this;
    }

    public ChartGenerator addLineColor(String lineColor) {
        if (this.def.lineColors.size() < ChartDefinition.MAX_SERIES) {
            this.def.lineColors.add(lineColor);
        }
        return this;
    }

    public ChartGenerator setTitle(String title) {
        this.def.title = title;
        return this;
    }

    public ChartGenerator setXAxisName(String xAxisName) {
        this.def.xAxisName = xAxisName;
        return this;
    }

    public ChartGenerator setXAxisUnit(String xAxisUnit) {
        this.def.xAxisUnit = xAxisUnit;
        return this;
    }

    public ChartGenerator setYAxisName(String yAxisName) {
        this.def.yAxisName = yAxisName;
        return this;
    }

    public ChartGenerator setYAxisUnit(String yAxisUnit) {
        this.def.yAxisUnit = yAxisUnit;
        return this;
    }

    public ChartGenerator setMeasurementName(String measurementName) {
        this.def.measurementNames.clear();
        this.def.measurementNames.add(measurementName);
        return this;
    }

    public ChartGenerator setWidth(int width) {
        this.def.width = width;
        return this;
    }

    public ChartGenerator setHeight(int height) {
        this.def.height = height;
        return this;
    }

    public ChartGenerator setTitleFontSize(Integer titleFontSize) {
        this.def.titleFontSize = titleFontSize;
        return this;
    }

    public ChartGenerator setChartFontSize(int chartFontSize) {
        this.def.chartFontSize = chartFontSize;
        return this;
    }

    public ChartGenerator setLineColor(String lineColor) {
        this.def.lineColors.clear();
        this.def.lineColors.add(lineColor);
        return this;
    }

    public ChartGenerator setFillColor(String fillColor) {
        this.def.fillColor = fillColor;
        return this;
    }

    public ChartGenerator setXAxisMin(Double xAxisMin) {
        this.def.xAxisMin = xAxisMin;
        return this;
    }

    public ChartGenerator setXAxisMax(Double xAxisMax) {
        this.def.xAxisMax = xAxisMax;
        return this;
    }

    public String createChart(ChartDefinition def) throws IOException {
        this.def = def;
        if (def.values != null && !def.values.isEmpty()) {
            return createValueChart();
        } else {
            return createTimeSeriesChart();
        }
    }

    private String createTimeSeriesChart() throws IOException {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        for (int i = 0; i < def.data.size(); i++) {
            String measurementName = (i < def.measurementNames.size()) ? def.measurementNames.get(i) : "Series " + (i + 1);
            TimeSeries series = new TimeSeries(measurementName);
            for (ChartData d : def.data.get(i)) {
                series.add(new Millisecond(new java.util.Date(d.timestamp)), d.value);
            }
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                def.title,
                def.xAxisName,
                def.yAxisName,
                dataset,
                !def.measurementNames.isEmpty(),
                true,
                false);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.white);

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            String colorStr = (i < def.lineColors.size()) ? def.lineColors.get(i) : ChartDefinition.DEFAULT_COLORS.get(i % ChartDefinition.DEFAULT_COLORS.size());
            renderer.setSeriesPaint(i, Color.decode(colorStr));
        }

        if (def.fillColor != null) {
            for (int i = 0; i < dataset.getSeriesCount(); i++) {
                renderer.setSeriesFillPaint(i, Color.decode(def.fillColor));
                renderer.setSeriesShapesFilled(i, true);
            }
        } else {
            for (int i = 0; i < dataset.getSeriesCount(); i++) {
                renderer.setSeriesShapesFilled(i, false);
            }
        }

        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        domainAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
        if (def.xAxisMin != null) {
            domainAxis.setMinimumDate(new java.util.Date(def.xAxisMin.longValue()));
        }
        if (def.xAxisMax != null) {
            domainAxis.setMaximumDate(new java.util.Date(def.xAxisMax.longValue()));
        }
        if ((def.xAxisName == null || def.xAxisName.isEmpty()) && (def.xAxisUnit == null || def.xAxisUnit.isEmpty())) {
            domainAxis.setVisible(false);
            domainAxis.setTickMarksVisible(false);
        }

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        if (def.yAxisName != null && !def.yAxisName.isEmpty() || def.yAxisUnit != null && !def.yAxisUnit.isEmpty()) {
            if (def.yAxisUnit != null && def.yAxisName != null) {
                rangeAxis.setLabel(def.yAxisName + " [" + def.yAxisUnit + "]");
            } else if (def.yAxisName != null) {
                rangeAxis.setLabel(def.yAxisName);
            } else {
                rangeAxis.setLabel("[" + def.yAxisUnit + "]");
            }
        } else {
            plot.setRangeGridlinesVisible(false);
            rangeAxis.setVisible(false);
            rangeAxis.setTickMarksVisible(false);
        }

        if (def.titleFontSize != null) {
            chart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, def.titleFontSize));
        } else {
            chart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, def.chartFontSize));
        }
        
        java.awt.Font chartFont = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, def.chartFontSize);
        plot.getDomainAxis().setLabelFont(chartFont);
        plot.getDomainAxis().setTickLabelFont(chartFont);
        plot.getRangeAxis().setLabelFont(chartFont);
        plot.getRangeAxis().setTickLabelFont(chartFont);
        chart.getLegend().setItemFont(chartFont);
        chart.setBorderVisible(false);
        plot.setOutlineVisible(false);

        // Convert chart to SVG
        org.w3c.dom.DOMImplementation domImpl = org.apache.batik.dom.GenericDOMImplementation.getDOMImplementation();
        String svgNS = "http://www.w3.org/2000/svg";
        org.w3c.dom.Document document = domImpl.createDocument(svgNS, "svg", null);
        org.apache.batik.svggen.SVGGraphics2D svgGenerator = new org.apache.batik.svggen.SVGGraphics2D(document);

        chart.draw(svgGenerator, new java.awt.geom.Rectangle2D.Double(0, 0, def.width, def.height));

        StringWriter sw = new StringWriter();
        try {
            svgGenerator.stream(sw, true);
        } catch (org.apache.batik.svggen.SVGGraphics2DIOException e) {
            throw new IOException("Error generating SVG", e);
        }
        return sw.toString();
    }

    private String createValueChart() throws IOException {
        org.jfree.data.xy.XYSeriesCollection dataset = new org.jfree.data.xy.XYSeriesCollection();
        int maxValues = 0;
        for (int i = 0; i < def.values.size(); i++) {
            String measurementName = (i < def.measurementNames.size()) ? def.measurementNames.get(i) : "Series " + (i + 1);
            org.jfree.data.xy.XYSeries series = new org.jfree.data.xy.XYSeries(measurementName);
            int x = 0;
            for (Double v : def.values.get(i)) {
                series.add(x++, v);
            }
            dataset.addSeries(series);
            if (def.values.get(i).size() > maxValues) {
                maxValues = def.values.get(i).size();
            }
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                def.title,
                def.xAxisName,
                def.yAxisName,
                dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL,
                !def.measurementNames.isEmpty(),
                true,
                false);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.white);

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            String colorStr = (i < def.lineColors.size()) ? def.lineColors.get(i) : ChartDefinition.DEFAULT_COLORS.get(i % ChartDefinition.DEFAULT_COLORS.size());
            renderer.setSeriesPaint(i, Color.decode(colorStr));
        }

        if (def.fillColor != null) {
            for (int i = 0; i < dataset.getSeriesCount(); i++) {
                renderer.setSeriesFillPaint(i, Color.decode(def.fillColor));
                renderer.setSeriesShapesFilled(i, true);
            }
        } else {
            for (int i = 0; i < dataset.getSeriesCount(); i++) {
                renderer.setSeriesShapesFilled(i, false);
            }
        }

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        if (def.xAxisMin != null) {
            domainAxis.setRange(def.xAxisMin, def.xAxisMax != null ? def.xAxisMax : maxValues);
        }
        if (def.xAxisMax != null) {
            domainAxis.setRange(def.xAxisMin != null ? def.xAxisMin : 0, def.xAxisMax);
        }
        if ((def.xAxisName == null || def.xAxisName.isEmpty()) && (def.xAxisUnit == null || def.xAxisUnit.isEmpty())) {
            domainAxis.setVisible(false);
            domainAxis.setTickMarksVisible(false);
        }
        domainAxis.setTickLabelsVisible(false);


        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        if (def.yAxisName != null && !def.yAxisName.isEmpty() || def.yAxisUnit != null && !def.yAxisUnit.isEmpty()) {
            if (def.yAxisUnit != null && def.yAxisName != null) {
                rangeAxis.setLabel(def.yAxisName + " [" + def.yAxisUnit + "]");
            } else if (def.yAxisName != null) {
                rangeAxis.setLabel(def.yAxisName);
            } else {
                rangeAxis.setLabel("[" + def.yAxisUnit + "]");
            }
        } else {
            plot.setRangeGridlinesVisible(false);
            rangeAxis.setVisible(false);
            rangeAxis.setTickMarksVisible(false);
        }

        if (chart.getTitle() != null && def.titleFontSize != null) {
            chart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, def.titleFontSize));
        } else if (chart.getTitle() != null) {
            chart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, def.chartFontSize));
        }
        
        java.awt.Font chartFont = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, def.chartFontSize);
        plot.getDomainAxis().setLabelFont(chartFont);
        plot.getDomainAxis().setTickLabelFont(chartFont);
        plot.getRangeAxis().setLabelFont(chartFont);
        plot.getRangeAxis().setTickLabelFont(chartFont);
        if(chart.getLegend()!=null){
            chart.getLegend().setItemFont(chartFont);
        }
        chart.setBorderVisible(false);
        plot.setOutlineVisible(false);

        // Convert chart to SVG
        org.w3c.dom.DOMImplementation domImpl = org.apache.batik.dom.GenericDOMImplementation.getDOMImplementation();
        String svgNS = "http://www.w3.org/2000/svg";
        org.w3c.dom.Document document = domImpl.createDocument(svgNS, "svg", null);
        org.apache.batik.svggen.SVGGraphics2D svgGenerator = new org.apache.batik.svggen.SVGGraphics2D(document);

        chart.draw(svgGenerator, new java.awt.geom.Rectangle2D.Double(0, 0, def.width, def.height));

        StringWriter sw = new StringWriter();
        try {
            svgGenerator.stream(sw, true);
        } catch (org.apache.batik.svggen.SVGGraphics2DIOException e) {
            throw new IOException("Error generating SVG", e);
        }
        return sw.toString();
    }
}
