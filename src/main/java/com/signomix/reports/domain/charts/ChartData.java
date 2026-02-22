package com.signomix.reports.domain.charts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChartData {
    public final long timestamp;
    public final double value;

    @JsonCreator
    public ChartData(@JsonProperty("timestamp") long timestamp, @JsonProperty("value") double value) {
        this.timestamp = timestamp;
        this.value = value;
    }
}
