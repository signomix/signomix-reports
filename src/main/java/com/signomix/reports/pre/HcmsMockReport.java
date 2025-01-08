package com.signomix.reports.pre;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.Report;
import com.signomix.common.db.ReportIface;
import com.signomix.common.db.ReportResult;
import com.signomix.common.hcms.Document;

import io.agroal.api.AgroalDataSource;

public class HcmsMockReport extends Report implements ReportIface {

    private static final Logger logger = Logger.getLogger(HcmsMockReport.class);

    @Override
    public ReportResult getReportResult(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            User user) {

        ReportResult result;
        String fileLocation = query.getParameter("file");
        String url = query.getParameter("url");
        logger.debug("file: " + fileLocation);
        logger.debug("url: " + url);

        // read HCMS document from URL
        // the document should contain the report result in JSON format
        String json;
        try {
            json = readFromUrl(url);
        } catch (Exception e) {
            result = new ReportResult();
            result.errorMessage = "Error reading from URL: " + e.getMessage();
            logger.warn(result.errorMessage);
            return result;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Document doc = objectMapper.readValue(json, Document.class);
            // once we have the document, we can parse it's content to get the report result
            result = objectMapper.readValue(doc.content, ReportResult.class);
            logger.debug("ReportResult: " + result);
            return result;
        } catch (JsonProcessingException ex) {
            logger.warn("Error parsing JSON: " + ex.getMessage());
            result = new ReportResult();
            result.errorMessage = "Error parsing JSON: " + ex.getMessage();
            return null;
        }
    }

    private String readFromUrl(String fileUrl) throws Exception {
        StringBuffer content = new StringBuffer();
        URL url = new URL(fileUrl);
        URLConnection urlConnection = url.openConnection();

        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        reader.close();
        return content.toString();
    }

    @Override
    public ReportResult getReportResult(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            Integer organization,
            Integer tenant,
            String path,
            User user) {
        throw new UnsupportedOperationException("Unimplemented method 'getReportHtml'");

    }

    @Override
    public String getReportHtml(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, Integer organization, Integer tenant, String path, User user, Boolean withHeader) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReportHtml'");
    }

    @Override
    public String getReportHtml(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, User user, Boolean withHeader) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReportHtml'");
    }

    @Override
    public String getReportCsv(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, Integer organization, Integer tenant, String path, User user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReportCsv'");
    }

    @Override
    public String getReportCsv(AgroalDataSource olapDs, AgroalDataSource oltpDs, AgroalDataSource logsDs,
            DataQuery query, User user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReportCsv'");
    }



}
