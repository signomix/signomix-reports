package com.sigmomix.reports.adapter.in;

import com.sigmomix.reports.port.in.ReportPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/api/reports")
public class ReportApi {

    @Inject
    ReportPort reportPort;
    
    @GET
    public Response getReport(@QueryParam("organization") Integer organization,
            @QueryParam("tenant") Integer tenant, 
            @QueryParam("path") String path, 
            @QueryParam("query") String query,
            @QueryParam("language") String language) {
        return Response.ok().entity(reportPort.getReportResult(query, organization, tenant, path, language)).build();
    }

}
