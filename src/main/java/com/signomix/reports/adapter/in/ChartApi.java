package com.signomix.reports.adapter.in;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.ReportDefinition;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.domain.AuthLogic;
import com.signomix.reports.domain.charts.ChartDefinition;
import com.signomix.reports.domain.charts.ChartGenerator;
import com.signomix.reports.domain.charts.ChartReportRequest;
import com.signomix.reports.port.in.AuthPort;
import com.signomix.reports.port.in.ReportPort;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jboss.logging.Logger;

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

    @Inject
    Vertx vertx;

    private CompletionStage<Response> runBlocking(
        java.util.function.Supplier<Response> supplier
    ) {
        CompletableFuture<Response> future = new CompletableFuture<>();
        vertx.executeBlocking(
            promise -> {
                try {
                    Response resp = supplier.get();
                    promise.complete(resp);
                } catch (Throwable t) {
                    logger.error("Error executing blocking task", t);
                    promise.complete(
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(t.getMessage())
                            .build()
                    );
                }
            },
            false,
            ar -> {
                if (ar.succeeded()) {
                    future.complete((Response) ar.result());
                } else {
                    logger.error("Failed to execute blocking task", ar.cause());
                    future.complete(
                        Response.status(
                            Response.Status.INTERNAL_SERVER_ERROR
                        ).build()
                    );
                }
            }
        );
        return future;
    }

    @POST
    @Produces("image/svg+xml")
    public CompletionStage<Response> getChart(
        @HeaderParam("Authentication") String token,
        ChartDefinition chartDefinition
    ) {
        return runBlocking(() -> {
            User user = authPort.getUser(token);
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            ChartGenerator generator = new ChartGenerator();
            try {
                String svg = generator.createChart(chartDefinition);
                return Response.ok(svg).build();
            } catch (IOException e) {
                return Response.serverError().build();
            }
        });
    }

    @POST
    @Path("/from-report")
    @Produces("image/svg+xml")
    public CompletionStage<Response> getChartFromReport(
        @HeaderParam("Authentication") String token,
        ChartReportRequest chartReportRequest
    ) {
        return runBlocking(() -> {
            User user = authPort.getUser(token);
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            ReportDefinition reportDefinition =
                chartReportRequest.reportDefinition;
            ChartDefinition chartDefinition =
                chartReportRequest.chartDefinition;

            // Create DataQuery from ReportDefinition
            DataQuery dataQuery = new DataQuery();
            if (
                reportDefinition.dql != null && !reportDefinition.dql.isEmpty()
            ) {
                dataQuery.setSource(reportDefinition.dql);
            } else {
                logger.error(
                    "ReportDefinition does not contain valid DQL query"
                );
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("ReportDefinition does not contain valid DQL query")
                    .build();
            }

            // Fetch report data using the data query
            ReportResult reportResult = reportPort.getReportResult(
                dataQuery,
                user
            );

            if (
                reportResult == null ||
                (reportResult.status != null && reportResult.status != 200)
            ) {
                logger.error(
                    "Error fetching report data: " +
                        (reportResult != null
                            ? reportResult.errorMessage
                            : "Null result")
                );
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(
                        "Error fetching report data: " +
                            (reportResult != null
                                ? reportResult.errorMessage
                                : "Null result")
                    )
                    .build();
            }

            // Set the report data to the chart definition
            chartDefinition.setReportData(reportResult);

            // Generate the chart
            ChartGenerator generator = new ChartGenerator();
            try {
                String svg = generator.createChart(chartDefinition);

                return Response.ok(svg).build();
            } catch (IOException e) {
                //TODO: generator.createErrorText(e.getMessage())
                return Response.serverError().build();
            }
        });
    }
}
