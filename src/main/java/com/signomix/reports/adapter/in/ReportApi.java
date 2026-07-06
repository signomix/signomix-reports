package com.signomix.reports.adapter.in;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.DataQueryException;
import com.signomix.common.db.ReportDefinition;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.domain.AuthLogic;
import com.signomix.reports.port.in.AuthPort;
import com.signomix.reports.port.in.ReportPort;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jboss.logging.Logger;

@ApplicationScoped
@Path("/api/reports")
public class ReportApi {

    @Inject
    Logger logger;

    @Inject
    ReportPort reportPort;

    @Inject
    AuthPort authPort;

    @Inject
    AuthLogic authLogic;

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

    @Path("/single")
    @GET
    public CompletionStage<Response> getCompiledReport(
        @HeaderParam("Authentication") String token,
        @QueryParam("query") String query,
        @QueryParam("header") Boolean header
    ) {
        CompletableFuture<Response> future = new CompletableFuture<>();
        vertx.executeBlocking(
            promise -> {
                try {
                    User user = authPort.getUser(token);
                    if (user == null) {
                        ReportResult result = new ReportResult();
                        result.status = 401;
                        result.errorMessage = "Unauthorized";
                        promise.complete(Response.ok().entity(result).build());
                        return;
                    }
                    DataQuery dataQuery;
                    try {
                        dataQuery = DataQuery.parse(query);
                        logger.info(
                            "getCompiledReport: " + dataQuery.getSource()
                        );
                    } catch (DataQueryException e) {
                        logger.warn(e.getMessage());
                        ReportResult result = new ReportResult();
                        result.status = 400;
                        result.errorMessage = e.getMessage();
                        promise.complete(Response.ok().entity(result).build());
                        return;
                    }
                    switch (dataQuery.getFormat()) {
                        case "csv":
                            promise.complete(
                                Response.ok(
                                    reportPort.getReportResultFormatted(
                                        dataQuery,
                                        user,
                                        header
                                    ),
                                    "text/csv"
                                ).build()
                            );
                            return;
                        case "html":
                            promise.complete(
                                Response.ok(
                                    reportPort.getReportResultFormatted(
                                        dataQuery,
                                        user,
                                        header
                                    ),
                                    MediaType.TEXT_HTML
                                ).build()
                            );
                            return;
                        default:
                            promise.complete(
                                Response.ok()
                                    .entity(
                                        reportPort.getReportResult(
                                            dataQuery,
                                            user
                                        )
                                    )
                                    .build()
                            );
                            return;
                    }
                } catch (Exception ex) {
                    logger.error("Error executing report", ex);
                    promise.complete(
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(ex.getMessage())
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

    @Path("/query")
    @GET
    @Consumes("application/json")
    public CompletionStage<Response> getCompiledReport2(
        @HeaderParam("Authentication") String token,
        DataQuery dataQuery
    ) {
        return runBlocking(() -> {
            logger.info("getCompiledReport2");
            User user = authLogic.getUserFromToken(token);
            if (user == null) {
                ReportResult result = new ReportResult();
                result.status = 401;
                result.errorMessage = "Unauthorized";
                return Response.ok().entity(result).build();
            }
            switch (dataQuery.getFormat()) {
                case "csv":
                    return Response.ok(
                        reportPort.getReportResultFormatted(
                            dataQuery,
                            user,
                            false
                        ),
                        "text/csv"
                    ).build();
                case "html":
                    return Response.ok(
                        reportPort.getReportResultFormatted(
                            dataQuery,
                            user,
                            false
                        ),
                        MediaType.TEXT_HTML
                    ).build();
                default:
                    return Response.ok()
                        .entity(reportPort.getReportResult(dataQuery, user))
                        .build();
            }
        });
    }

    @Path("/orgreport")
    @GET
    public CompletionStage<Response> getReport(
        @HeaderParam("Authentication") String token,
        @QueryParam("organization") Integer organization,
        @QueryParam("tenant") Integer tenant,
        @QueryParam("path") String path,
        @QueryParam("query") String query,
        @QueryParam("language") String language
    ) {
        return runBlocking(() -> {
            User user = authLogic.getUserFromToken(token);
            if (user == null) {
                ReportResult result = new ReportResult();
                result.status = 401;
                result.errorMessage = "Unauthorized";
                return Response.ok().entity(result).build();
            }
            return Response.ok()
                .entity(
                    reportPort.getReportResult(
                        query,
                        organization,
                        tenant,
                        path,
                        language,
                        user
                    )
                )
                .build();
        });
    }

    @Path("/twins")
    @GET
    public CompletionStage<Response> getTwinsReport(
        @HeaderParam("Authentication") String token,
        @QueryParam("organization") Integer organization,
        @QueryParam("tenant") Integer tenant,
        @QueryParam("path") String path,
        @QueryParam("query") String query,
        @QueryParam("language") String language
    ) {
        return runBlocking(() -> {
            User user = authLogic.getUserFromToken(token);
            if (user == null) {
                ReportResult result = new ReportResult();
                result.status = 401;
                result.errorMessage = "Unauthorized";
                return Response.ok().entity(result).build();
            }
            return Response.ok()
                .entity(
                    reportPort.getTwinsReportResult(
                        query,
                        organization,
                        tenant,
                        path,
                        language,
                        user
                    )
                )
                .build();
        });
    }

    @Path("/multi")
    @GET
    @Consumes("application/json")
    public CompletionStage<Response> getCompiledMultiReport(
        @HeaderParam("Authentication") String token,
        List<DataQuery> queryList
    ) {
        return runBlocking(() -> {
            logger.info("getCompiledMultiReport");
            User user = authPort.getUser(token);
            if (user == null) {
                ReportResult result = new ReportResult();
                result.status = 401;
                result.errorMessage = "Unauthorized";
                return Response.ok().entity(result).build();
            }
            return Response.ok()
                .entity(reportPort.getReportResult(queryList.get(0), user))
                .build();
        });
    }

    /**
     * Get the list of available report definitions available for the authenticated
     * user.
     *
     * @param token
     * @return
     */
    @Path("/reports")
    @GET
    public CompletionStage<Response> getReports(
        @HeaderParam("Authentication") String token
    ) {
        return runBlocking(() -> {
            User user = authLogic.getUserFromToken(token);
            if (user == null) {
                ReportResult result = new ReportResult();
                result.status = 401;
                result.errorMessage = "Unauthorized";
                return Response.ok().entity(result).build();
            }
            return Response.ok()
                .entity(reportPort.getReportDefinitionsForUser(user))
                .build();
        });
    }

    /**
     * Get the report definition by id. The user must be authenticated and have
     * access to the report definition.
     *
     * @param token
     * @param id
     * @return
     */
    @Path("/report")
    @GET
    public CompletionStage<Response> getReportDefinition(
        @HeaderParam("Authentication") String token,
        @QueryParam("id") Integer id
    ) {
        return runBlocking(() -> {
            User user = authLogic.getUserFromToken(token);
            if (user == null) {
                ReportResult result = new ReportResult();
                result.status = 401;
                result.errorMessage = "Unauthorized";
                return Response.ok().entity(result).build();
            }
            return Response.ok()
                .entity(reportPort.getReportDefinition(id, user))
                .build();
        });
    }

    /**
     * Create a report definition.
     * The user must be authenticated and have access to the report definition.
     *
     * @param token
     * @param reportDefinition
     * @return
     */
    @Path("/report")
    @POST
    @Consumes("application/json")
    public CompletionStage<Response> createReportDefinition(
        @HeaderParam("Authentication") String token,
        ReportDefinition reportDefinition
    ) {
        return runBlocking(() -> {
            User user = authLogic.getUserFromToken(token);
            if (user == null) {
                ReportResult result = new ReportResult();
                result.status = 401;
                result.errorMessage = "Unauthorized";
                return Response.ok().entity(result).build();
            }
            try {
                reportPort.createReportDefinition(reportDefinition, user);
                return Response.ok().build();
            } catch (Exception e) {
                logger.error("Error creating report definition", e);
                ReportResult result = new ReportResult();
                result.status = 500;
                result.errorMessage =
                    "Error creating report definition: " + e.getMessage();
                return Response.ok().entity(result).build();
            }
        });
    }

    /**
     * Update a report definition.
     * The user must be authenticated and have access to the report definition.
     *
     * @param token
     * @param id
     * @param reportDefinition
     * @return
     */
    @Path("/report")
    @PUT
    @Consumes("application/json")
    public CompletionStage<Response> updateReportDefinition(
        @HeaderParam("Authentication") String token,
        ReportDefinition reportDefinition
    ) {
        return runBlocking(() -> {
            User user = authLogic.getUserFromToken(token);
            if (user == null) {
                ReportResult result = new ReportResult();
                result.status = 401;
                result.errorMessage = "Unauthorized";
                return Response.ok().entity(result).build();
            }
            try {
                reportPort.updateReportDefinition(reportDefinition, user);
                return Response.ok().build();
            } catch (Exception e) {
                logger.error("Error updating report definition", e);
                ReportResult result = new ReportResult();
                result.status = 500;
                result.errorMessage =
                    "Error updating report definition: " + e.getMessage();
                return Response.ok().entity(result).build();
            }
        });
    }
}
