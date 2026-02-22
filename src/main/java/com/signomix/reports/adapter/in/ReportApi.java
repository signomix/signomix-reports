package com.signomix.reports.adapter.in;

import java.util.List;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.DataQueryException;
import com.signomix.common.db.ReportDefinition;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.domain.AuthLogic;
import com.signomix.reports.port.in.AuthPort;
import com.signomix.reports.port.in.ReportPort;

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
            @QueryParam("query") String query, @QueryParam("header") Boolean header) {
        User user = authPort.getUser(token);
        if (user == null) {
            ReportResult result = new ReportResult();
            result.status = 401;
            result.errorMessage = "Unauthorized";
            return Response.ok().entity(result).build();
            // return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        DataQuery dataQuery;
        try {
            dataQuery = DataQuery.parse(query);
            logger.info("getCompiledReport: " + dataQuery.getSource());
        } catch (DataQueryException e) {
            logger.warn(e.getMessage());
            ReportResult result = new ReportResult();
            result.status = 400;
            result.errorMessage = e.getMessage();
            return Response.ok().entity(result).build();
        }
        switch (dataQuery.getFormat()) {
            case "csv":
                return Response.ok().entity(reportPort.getReportResultFormatted(dataQuery, user, header)).build();
            case "html":
                return Response.ok().entity(reportPort.getReportResultFormatted(dataQuery, user, header)).build();
            default:
                return Response.ok().entity(reportPort.getReportResult(dataQuery, user)).build();
        }
    }

    @Path("/query")
    @GET
    @Consumes("application/json")
    public Response getCompiledReport2(@HeaderParam("Authentication") String token,
            DataQuery dataQuery) {
        logger.info("getCompiledReport2");
        // User user = authPort.getUser(token);
        User user = authLogic.getUserFromToken(token);
        if (user == null) {
            ReportResult result = new ReportResult();
            result.status = 401;
            result.errorMessage = "Unauthorized";
            return Response.ok().entity(result).build();
            // return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        switch (dataQuery.getFormat()) {
            case "csv":
                return Response.ok(reportPort.getReportResultFormatted(dataQuery, user, false), "text/csv").build();
            case "html":
                return Response.ok(reportPort.getReportResultFormatted(dataQuery, user, false), MediaType.TEXT_HTML)
                        .build();
            default:
                return Response.ok().entity(reportPort.getReportResult(dataQuery, user)).build();
        }
    }

    @Path("/orgreport")
    @GET
    public Response getReport(@HeaderParam("Authentication") String token,
            @QueryParam("organization") Integer organization,
            @QueryParam("tenant") Integer tenant,
            @QueryParam("path") String path,
            @QueryParam("query") String query,
            @QueryParam("language") String language) {
        User user = authLogic.getUserFromToken(token);
        if (user == null) {
            ReportResult result = new ReportResult();
            result.status = 401;
            result.errorMessage = "Unauthorized";
            return Response.ok().entity(result).build();
            // return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok().entity(reportPort.getReportResult(query, organization, tenant, path, language, user))
                .build();
    }

    @Path("/twins")
    @GET
    public Response getTwinsReport(@HeaderParam("Authentication") String token,
            @QueryParam("organization") Integer organization,
            @QueryParam("tenant") Integer tenant,
            @QueryParam("path") String path,
            @QueryParam("query") String query,
            @QueryParam("language") String language) {
        User user = authLogic.getUserFromToken(token);
        if (user == null) {
            ReportResult result = new ReportResult();
            result.status = 401;
            result.errorMessage = "Unauthorized";
            return Response.ok().entity(result).build();
            // return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok().entity(reportPort.getTwinsReportResult(query, organization, tenant, path, language, user))
                .build();
    }

    @Path("/multi")
    @GET
    @Consumes("application/json")
    public Response getCompiledMultiReport(@HeaderParam("Authentication") String token,
            List<DataQuery> queryList) {
        logger.info("getCompiledMultiReport");
        User user = authPort.getUser(token);
        if (user == null) {
            if (user == null) {
                ReportResult result = new ReportResult();
                result.status = 401;
                result.errorMessage = "Unauthorized";
                return Response.ok().entity(result).build();
                // return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        }
        return Response.ok().entity(reportPort.getReportResult(queryList.get(0), user)).build();
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
    public Response getReports(@HeaderParam("Authentication") String token) {
        User user = authLogic.getUserFromToken(token);
        if (user == null) {
            ReportResult result = new ReportResult();
            result.status = 401;
            result.errorMessage = "Unauthorized";
            return Response.ok().entity(result).build();
            // return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok().entity(reportPort.getReportDefinitionsForUser(user)).build();
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
    public Response getReportDefinition(@HeaderParam("Authentication") String token,
            @QueryParam("id") Integer id) {
        User user = authLogic.getUserFromToken(token);
        if (user == null) {
            ReportResult result = new ReportResult();
            result.status = 401;
            result.errorMessage = "Unauthorized";
            return Response.ok().entity(result).build();
            // return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok().entity(reportPort.getReportDefinition(id, user)).build();
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
    public Response createReportDefinition(@HeaderParam("Authentication") String token,
            ReportDefinition reportDefinition) {
        User user = authLogic.getUserFromToken(token);
        if (user == null) {
            ReportResult result = new ReportResult();
            result.status = 401;
            result.errorMessage = "Unauthorized";
            return Response.ok().entity(result).build();
            // return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        try {
            reportPort.createReportDefinition(reportDefinition, user);
            return Response.ok().build();
        } catch (Exception e) {
            logger.error("Error creating report definition", e);
            ReportResult result = new ReportResult();
            result.status = 500;
            result.errorMessage = "Error creating report definition: " + e.getMessage();
            return Response.ok().entity(result).build();
        }
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
    public Response updateReportDefinition(@HeaderParam("Authentication") String token,
            ReportDefinition reportDefinition) {
        User user = authLogic.getUserFromToken(token);
        if (user == null) {
            ReportResult result = new ReportResult();
            result.status = 401;
            result.errorMessage = "Unauthorized";
            return Response.ok().entity(result).build();
            // return Response.status(Response.Status.UNAUTHORIZED).build();    
        }
        try {
            reportPort.updateReportDefinition(reportDefinition, user);
            return Response.ok().build();
        } catch (Exception e) {
            logger.error("Error updating report definition", e);
            ReportResult result = new ReportResult();
            result.status = 500;
            result.errorMessage = "Error updating report definition: " + e.getMessage();
            return Response.ok().entity(result).build();
        }
    }
}
