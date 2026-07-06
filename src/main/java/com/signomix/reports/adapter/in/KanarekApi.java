package com.signomix.reports.adapter.in;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.DataQueryException;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.domain.AuthLogic;
import com.signomix.reports.port.in.AuthPort;
import com.signomix.reports.port.in.ReportPort;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jboss.logging.Logger;

@ApplicationScoped
//@Path("/api/iot/gdata/{groupEui}")
@Path("/api/reports/gdata/{groupEui}")
public class KanarekApi {

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

    @GET
    @Produces("text/plain")
    public CompletionStage<Response> getReport(
        @QueryParam("tid") String token,
        @PathParam("groupEui") String groupEui
    ) {
        return runBlocking(() -> {
            User user = authPort.getUser(token);
            if (user == null) {
                ReportResult result = new ReportResult();
                result.status = 401;
                result.errorMessage = "Unauthorized";
                return Response.ok().entity(result).build();
            }
            String query =
                "report com.signomix.reports.pre.kanarek.KanarekReport group " +
                groupEui +
                " channel pm2_5,pm10,pressure,temperature,humidity,latitude,longitude,pm2_5avg,pm10avg limit 1 notnull";

            DataQuery dataQuery;
            try {
                dataQuery = DataQuery.parse(query);
            } catch (DataQueryException e) {
                logger.warn(e.getMessage());
                ReportResult result = new ReportResult();
                result.status = 400;
                result.errorMessage = e.getMessage();
                return Response.ok().entity(result).build();
            }
            return Response.ok(
                reportPort.getReportResultFormatted(dataQuery, user, false),
                "text/plain"
            ).build();
        });
    }
}
