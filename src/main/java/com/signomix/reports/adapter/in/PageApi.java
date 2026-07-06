package com.signomix.reports.adapter.in;

import com.signomix.common.User;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.port.in.AuthPort;
import com.signomix.reports.port.in.PagePort;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jboss.logging.Logger;

@ApplicationScoped
@Path("/api/reports")
public class PageApi {

    @Inject
    Logger logger;

    @Inject
    PagePort pagePort;

    @Inject
    AuthPort authPort;

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

    @Path("/page")
    @POST
    public CompletionStage<Response> getPage(
        @HeaderParam("Authentication") String token,
        String definition,
        @QueryParam("header") Boolean header,
        @QueryParam("title") Boolean title,
        @QueryParam("tz") String timeZone
    ) {
        return runBlocking(() -> {
            User user = authPort.getUser(token);
            if (user == null) {
                ReportResult result = new ReportResult();
                result.status = 401;
                result.errorMessage = "Unauthorized";
                return Response.ok().entity(result).build();
            }
            String timeZoneName =
                timeZone != null && !timeZone.isEmpty() ? timeZone : "UTC";
            timeZoneName = URLDecoder.decode(
                timeZoneName,
                StandardCharsets.UTF_8
            );
            // validate timeZoneName
            ArrayList<String> availableTimeZones = new ArrayList<>();
            for (String tz : TimeZone.getAvailableIDs()) {
                availableTimeZones.add(tz);
            }
            if (!availableTimeZones.contains(timeZoneName)) {
                ReportResult result = new ReportResult();
                result.status = 400;
                result.errorMessage = "Invalid time zone";
                return Response.ok().entity(result).build();
            }
            String pageSource = pagePort.getPageSource(
                user,
                definition,
                header == null ? true : header,
                title == null ? true : title,
                timeZoneName
            );
            if (pageSource == null) {
                ReportResult result = new ReportResult();
                result.status = 400;
                result.errorMessage = "Invalid page definition";
                return Response.ok().entity(result).build();
            }
            return Response.ok().entity(pageSource).build();
        });
    }

    @Path("/page/{id}")
    @GET
    public CompletionStage<Response> getPageById(
        @HeaderParam("Authentication") String token,
        @PathParam("id") String id,
        @QueryParam("header") Boolean header,
        @QueryParam("title") Boolean title,
        @QueryParam("tz") String timeZone
    ) {
        return runBlocking(() -> {
            User user = authPort.getUser(token);
            if (user == null) {
                ReportResult result = new ReportResult();
                result.status = 401;
                result.errorMessage = "Unauthorized";
                return Response.ok().entity(result).build();
            }
            String timeZoneName =
                timeZone != null && !timeZone.isEmpty() ? timeZone : "UTC";
            timeZoneName = URLDecoder.decode(
                timeZoneName,
                StandardCharsets.UTF_8
            );
            logger.debug(
                "Received request for page ID: " +
                    id +
                    " with time zone: " +
                    timeZone +
                    " decoded time zone: " +
                    timeZoneName
            );
            // validate timeZoneName
            ArrayList<String> availableTimeZones = new ArrayList<>();
            for (String tz : TimeZone.getAvailableIDs()) {
                availableTimeZones.add(tz);
            }
            if (!availableTimeZones.contains(timeZoneName)) {
                ReportResult result = new ReportResult();
                result.status = 400;
                result.errorMessage = "Invalid time zone";
                return Response.ok().entity(result).build();
            }
            String pageSource = pagePort.getPageSourceById(
                user,
                id,
                header == null ? true : header,
                title == null ? true : title,
                timeZoneName
            );
            if (pageSource == null) {
                ReportResult result = new ReportResult();
                result.status = 400;
                result.errorMessage = "Invalid page ID";
                return Response.ok().entity(result).build();
            }
            return Response.ok().entity(pageSource).build();
        });
    }
}
