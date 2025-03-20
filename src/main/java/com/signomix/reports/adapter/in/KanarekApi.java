package com.signomix.reports.adapter.in;

import org.jboss.logging.Logger;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.DataQueryException;
import com.signomix.common.db.ReportResult;
import com.signomix.reports.domain.AuthLogic;
import com.signomix.reports.port.in.AuthPort;
import com.signomix.reports.port.in.ReportPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

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

    @GET
    @Produces("text/plain")
    public Response getReport(@QueryParam("tid") String token,
            @PathParam("groupEui") String groupEui) {
        User user = authPort.getUser(token);
        if (user == null) {
            ReportResult result = new ReportResult();
            result.status = 401;
            result.errorMessage = "Unauthorized";
            return Response.ok().entity(result).build();
            // return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String query = "report com.signomix.reports.pre.kanarek.KanarekReport group " + groupEui 
        + "channel temperature,humidity,latitude,longitude,pm25,pm10 limit 1 notnull";
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
        return Response.ok(reportPort.getReportResultFormatted(dataQuery, user,false),"text/plain").build();
    }



}
