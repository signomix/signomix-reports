package com.signomix.reports.adapter.in;

import java.util.List;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.reports.domain.AuthLogic;
import com.signomix.reports.port.in.AuthPort;
import com.signomix.reports.port.in.ReportPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
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

    @Inject
    AuthLogic authLogic;

    @Path("/single")
    @GET
    public Response getCompiledReport(@HeaderParam("Authentication") String token,
            @QueryParam("query") String query) {
        User user = authPort.getUser(token);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok().entity(reportPort.getReportResult(query, user)).build();
    }

    @Path("/single")
    @GET
    @Consumes("application/json")
    public Response getCompiledReport2(@HeaderParam("Authentication") String token,
            DataQuery query) {
                logger.info("getCompiledReport2");
        //User user = authPort.getUser(token);
        User user = authLogic.getUserFromToken(token);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok().entity(reportPort.getReportResult(query, user)).build();
    }

    @Path("/single")
    @GET
    public Response getReport(@HeaderParam("Authentication") String token,
            @QueryParam("organization") Integer organization,
            @QueryParam("tenant") Integer tenant,
            @QueryParam("path") String path,
            @QueryParam("query") String query,
            @QueryParam("language") String language) {
        return Response.ok().entity(reportPort.getReportResult(query, organization, tenant, path, language)).build();
    }
    
    @Path("/multi")
    @GET
    @Consumes("application/json")
    public Response getCompiledMultiReport(@HeaderParam("Authentication") String token,
            List<DataQuery> queryList) {
                logger.info("getCompiledMultiReport");
        User user = authPort.getUser(token);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok().entity(reportPort.getReportResult(queryList.get(0), user)).build();
    }



}
