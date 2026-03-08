package com.signomix.reports.adapter.in;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.port.in.AuthPort;
import com.signomix.reports.port.in.PagePort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/api/reports")
public class PageApi {

    @Inject
    Logger logger;

    @Inject
    PagePort pagePort;

    @Inject
    AuthPort authPort;

    @Path("/page")
    @POST
    public Response getPage(@HeaderParam("Authentication") String token,
            String definition, @QueryParam("header") Boolean header, @QueryParam("title") Boolean title) {
        User user = authPort.getUser(token);
        if (user == null) {
            ReportResult result = new ReportResult();
            result.status = 401;
            result.errorMessage = "Unauthorized";
            return Response.ok().entity(result).build();
            // return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String pageSource = pagePort.getPageSource(user, definition, header==null ? true : header, title==null ? true : title);
        if(pageSource == null) {
            ReportResult result = new ReportResult();
            result.status = 400;
            result.errorMessage = "Invalid page definition";
            return Response.ok().entity(result).build();
        }
        return Response.ok().entity(pageSource).build();
    }

    @Path("/page/{id}")
    @GET
    public Response getPageById(@HeaderParam("Authentication") String token,
            @PathParam("id") String id, @QueryParam("header") Boolean header, @QueryParam("title") Boolean title) {
        User user = authPort.getUser(token);
        if (user == null) {
            ReportResult result = new ReportResult();
            result.status = 401;
            result.errorMessage = "Unauthorized";
            return Response.ok().entity(result).build();
        }
        String pageSource = pagePort.getPageSourceById(user, id, header==null ? true : header, title==null ? true : title);
        if(pageSource == null) {
            ReportResult result = new ReportResult();
            result.status = 400;
            result.errorMessage = "Invalid page ID";
            return Response.ok().entity(result).build();
        }
        return Response.ok().entity(pageSource).build();
    }
}
