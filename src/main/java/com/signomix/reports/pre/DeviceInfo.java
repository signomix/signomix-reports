package com.signomix.reports.pre;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.ReportResult;
import com.signomix.common.iot.Device;
import com.signomix.reports.domain.Report;
import com.signomix.reports.domain.ReportIface;

import io.agroal.api.AgroalDataSource;

public class DeviceInfo extends Report implements ReportIface {

    @Override
    public ReportResult getReportResult(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            Integer organization,
            Integer tenant,
            String path,
            User user) {

        if (!isAuthorized()) {
            return new ReportResult().error(403, "Not authorized");
        }
        ReportResult result = new ReportResult(query);
        result.contentType = "text/html";
        result.content = "<h1>Test</h1><p>Test</p>";
        return result;
    }

    @Override
    public ReportResult getReportResult(
            AgroalDataSource olapDs,
            AgroalDataSource oltpDs,
            AgroalDataSource logsDs,
            DataQuery query,
            User user) {

        if (!isAuthorized()) {
            return new ReportResult().error(403, "Not authorized");
        }

        Device device = new Device();
        String sql = "SELECT * FROM devices WHERE eui = ? AND ( userid = ? OR team LIKE ? OR administrators LIKE ?)";
        try(Connection conn = oltpDs.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, query.getEui());
            ps.setString(2, user.uid);
            ps.setString(3, "%,"+user.uid+",%");
            ps.setString(4, "%,"+user.uid+",%");
            try(ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    device.setName(rs.getString("name"));
                    device.setType(rs.getString("type"));
                    device.setDescription(rs.getString("description"));
                }else{
                    return new ReportResult().error(404, "Device not found");
                }
            }
        } catch (SQLException e) {
            return new ReportResult().error(500, e.getMessage());
        }

        sql="SELECT last(ts,ts)FROM devicestatus where eui=?";
        try(Connection conn = olapDs.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, query.getEui());
            try(ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    device.setLastSeen(rs.getTimestamp(1).getTime());
                }
            }
        } catch (SQLException e) {
            return new ReportResult().error(500, e.getMessage());
        }

        ReportResult result = new ReportResult(query);
        result.contentType = "text/html";
        String content = """
                <p>
                <b>{name}</b><br>
                EUI: {eui}<br>
                Typ: {type}<br>
                Aktywność: {lastSeen}
                </p>
                <p>
                {description}
                </p>
                        """;
        content = content.replace("{eui}", query.getEui());
        try {
            content = content.replace("{name}", device.getName());
            content = content.replace("{type}", device.getType());
            content = content.replace("{description}", device.getDescription());
        } catch (Exception e) {
            content = content.replace("{name}", "Device not found");
            content = content.replace("{type}", "Device not found");
            content = content.replace("{description}", "Device not found");
        }
        ZonedDateTime utc = Instant.ofEpochMilli(device.getLastSeen()).atZone(ZoneOffset.UTC);
        content = content.replace("{lastSeen}", utc.toString());

        result.content = content;
        return result;
    }

}
