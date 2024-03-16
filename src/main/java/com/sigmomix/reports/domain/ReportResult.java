package com.sigmomix.reports.domain;

import java.sql.Timestamp;
import java.util.HashMap;

import com.signomix.common.db.DataQuery;

public class ReportResult {
    public HashMap<String, Dataset> data;
    public HashMap<String, DatasetHeader> headers;
    public String title;
    public String description;
    public Long id;
    public Timestamp created;
    public DataQuery query;

    public ReportResult error(String message) {
        this.title = "Error";
        this.description = message;
        return this;
    }

    /**
     * Get header names for a dataset of a given name
     * 
     * @param datasetName
     * @return ArrayList<String>
     */
    public DatasetHeader getHeaders(String datasetName) {
        return headers.get(datasetName);
    }

    /**
     * Get data for a dataset of a given name
     * 
     * @param datasetName
     * @return ArrayList<ArrayList<Double>>
     */
    public Dataset getData(String datasetName) {
        return data.get(datasetName);
    }

    public void addDatasetHeader(DatasetHeader header) {
        headers.put(header.name, header);
    }

    public void addDataset(Dataset dataset) {
        data.put(dataset.name, dataset);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.created = timestamp;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataQuery getQuery() {
        return query;
    }

    public void setQuery(DataQuery query) {
        this.query = query;
    }

}