package com.sigmomix.reports.domain;

import java.util.ArrayList;

/**
 * A dataset is a list of rows where each row is a list of numbers.
 */
public class Dataset {
    /** The total number of matching rows in the database without considering the query limit */
    public Long size;
    /** The resulting data */
    public ArrayList<DatasetRow> data;
    /** Dataset name */
    public String name;

    public Dataset() {
        data = new ArrayList<DatasetRow>();
    }

    public Dataset(String name) {
        this.name = name;
        data = new ArrayList<DatasetRow>();
    }
}
