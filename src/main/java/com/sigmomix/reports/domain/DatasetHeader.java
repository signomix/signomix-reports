package com.sigmomix.reports.domain;

import java.util.ArrayList;

/**
 * A dataset header is a list of column names.
 */
public class DatasetHeader {
    public ArrayList<String> columns;
    public String name;

    public DatasetHeader(String name) {
        this.name = name;
        columns = new ArrayList<String>();
    }

}
