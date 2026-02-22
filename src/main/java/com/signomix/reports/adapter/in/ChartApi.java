package com.signomix.reports.adapter.in;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.reports.domain.AuthLogic;
import com.signomix.reports.domain.charts.ChartDefinition;
import com.signomix.reports.domain.charts.ChartGenerator;
import com.signomix.reports.port.in.AuthPort;

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
}
