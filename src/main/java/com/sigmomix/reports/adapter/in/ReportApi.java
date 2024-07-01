package com.sigmomix.reports.adapter.in;

import org.jboss.logging.Logger;

import com.sigmomix.reports.port.in.AuthPort;
import com.sigmomix.reports.port.in.ReportPort;
import com.signomix.common.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/api/reports")
public class ReportApi {

    @Inject
    Logger logger;

    @Inject
    ReportPort reportPort;

    @Inject
    AuthPort authPort;

    @GET
    public Response getCompiledReport(@HeaderParam("Authentication") String token,
            @QueryParam("query") String query,
            @QueryParam("language") String language) {
        User user = authPort.getUser(token);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok().entity(reportPort.getReportResult(query, language, user)).build();
    }

    @GET
    public Response getReport(@QueryParam("organization") Integer organization,
            @QueryParam("tenant") Integer tenant,
            @QueryParam("path") String path,
            @QueryParam("query") String query,
            @QueryParam("language") String language) {
        return Response.ok().entity(reportPort.getReportResult(query, organization, tenant, path, language)).build();
    }

}
