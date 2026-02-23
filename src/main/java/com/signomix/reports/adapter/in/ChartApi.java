package com.signomix.reports.adapter.in;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.ReportDefinition;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.domain.AuthLogic;
import com.signomix.reports.domain.charts.ChartDefinition;
import com.signomix.reports.domain.charts.ChartGenerator;
import com.signomix.reports.port.in.AuthPort;
import com.signomix.reports.port.in.ReportPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/api/reports/charts")
public class ChartApi {

    @Inject
    Logger logger;

    @Inject
    AuthPort authPort;

    @Inject
    AuthLogic authLogic;

    @Inject
    ReportPort reportPort;

    @POST
    @Produces("image/svg+xml")
    public Response getChart(@HeaderParam("Authentication") String token, ChartDefinition chartDefinition) {
        User user = authPort.getUser(token);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        try {
            ChartGenerator generator = new ChartGenerator();
            String svg = generator.createChart(chartDefinition);
            return Response.ok(svg).build();
        } catch (Exception e) {
            logger.error("Error generating chart", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/from-report")
    @Produces("image/svg+xml")
    public Response getChartFromReport(@HeaderParam("Authentication") String token, 
                                      ChartDefinition chartDefinition, 
                                      ReportDefinition reportDefinition) {
        User user = authPort.getUser(token);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        
        try {
            // Create DataQuery from ReportDefinition
            DataQuery dataQuery = new DataQuery();
            if (reportDefinition.dql != null && !reportDefinition.dql.isEmpty()) {
                dataQuery.setSource(reportDefinition.dql);
            } else {
                logger.error("ReportDefinition does not contain valid DQL query");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("ReportDefinition does not contain valid DQL query")
                        .build();
            }
            
            // Fetch report data using the data query
            ReportResult reportResult = reportPort.getReportResult(dataQuery, user);
            
            if (reportResult == null || reportResult.status != null && reportResult.status != 200) {
                logger.error("Error fetching report data: " + (reportResult != null ? reportResult.errorMessage : "Null result"));
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error fetching report data: " + (reportResult != null ? reportResult.errorMessage : "Null result"))
                        .build();
            }
            
            // Set the report data to the chart definition
            chartDefinition.setReportData(reportResult);
            
            // Generate the chart
            ChartGenerator generator = new ChartGenerator();
            String svg = generator.createChart(chartDefinition);
            
            return Response.ok(svg).build();
            
        } catch (Exception e) {
            logger.error("Error generating chart from report", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error generating chart from report: " + e.getMessage())
                    .build();
        }
    }
}
