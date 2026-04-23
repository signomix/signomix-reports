package com.signomix.reports.adapter.in;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.TimeZone;

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
    public Response getPage(
        @HeaderParam("Authentication") String token,
        String definition,
        @QueryParam("header") Boolean header,
        @QueryParam("title") Boolean title,
        @QueryParam("tz") String timeZone
    ) {
        User user = authPort.getUser(token);
        if (user == null) {
            ReportResult result = new ReportResult();
            result.status = 401;
            result.errorMessage = "Unauthorized";
            return Response.ok().entity(result).build();
            // return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String timeZoneName = (timeZone != null && !timeZone.isEmpty()) ? timeZone : "UTC";
        timeZoneName = URLDecoder.decode(timeZoneName, StandardCharsets.UTF_8);
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
    }

    @Path("/page/{id}")
    @GET
    public Response getPageById(
        @HeaderParam("Authentication") String token,
        @PathParam("id") String id,
        @QueryParam("header") Boolean header,
        @QueryParam("title") Boolean title,
        @QueryParam("tz") String timeZone
    ) {
        User user = authPort.getUser(token);
        if (user == null) {
            ReportResult result = new ReportResult();
            result.status = 401;
            result.errorMessage = "Unauthorized";
            return Response.ok().entity(result).build();
        }
        String timeZoneName = (timeZone != null && !timeZone.isEmpty()) ? timeZone : "UTC";
        timeZoneName = URLDecoder.decode(timeZoneName, StandardCharsets.UTF_8);
        logger.debug("Received request for page ID: " + id + " with time zone: " + timeZone + " decoded time zone: " + timeZoneName);
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
    }
}
