package com.signomix.reports.pre;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.signomix.common.User;
import com.signomix.common.db.DataQuery;
import com.signomix.common.db.Report;
import com.signomix.common.db.ReportIface;
import com.signomix.common.db.ReportResult;
import com.signomix.common.iot.Device;

import io.agroal.api.AgroalDataSource;

public class DeviceConfig extends Report implements ReportIface {

    @Override
    public ReportResult getReportResult(
        AgroalDataSource olapDs,
        AgroalDataSource oltpDs,
        AgroalDataSource logsDs,
        DataQuery query,
        Integer organization,
        Integer tenant,
        String path,
        User user
    ) {
        if (!isAuthorized()) {
            return new ReportResult().error(403, "Not authorized");
        }
        Device device = new Device();
        String sql =
            "SELECT * FROM devices WHERE eui = ? AND organization = ?";
        try (
            Connection conn = oltpDs.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, query.getEui());
            ps.setLong(2, user.organization);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    device.setConfiguration(rs.getString("configuration"));
                } else {
                    return new ReportResult().error(404, "Device not found");
                }
                rs.close();
            }
        } catch (SQLException e) {
            return new ReportResult().error(500, e.getMessage());
        }

        ReportResult result = new ReportResult(query);
        result.contentType = "text/plain";
        result.content = device.getConfiguration();
        return result;
    }

    @Override
    public ReportResult getReportResult(
        AgroalDataSource olapDs,
        AgroalDataSource oltpDs,
        AgroalDataSource logsDs,
        DataQuery query,
        User user
    ) {
        if (!isAuthorized()) {
            return new ReportResult().error(403, "Not authorized");
        }

        Device device = new Device();
        String sql =
            "SELECT * FROM devices WHERE eui = ? AND ( userid = ? OR team LIKE ? OR administrators LIKE ?)";
        try (
            Connection conn = oltpDs.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, query.getEui());
            ps.setString(2, user.uid);
            ps.setString(3, "%," + user.uid + ",%");
            ps.setString(4, "%," + user.uid + ",%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    device.setConfiguration(rs.getString("configuration"));
                } else {
                    return new ReportResult().error(404, "Device not found");
                }
                rs.close();
            }
        } catch (SQLException e) {
            return new ReportResult().error(500, e.getMessage());
        }

        ReportResult result = new ReportResult(query);
        result.contentType = "text/plain";
        result.content = device.getConfiguration();
        return result;
    }

    @Override
    public String getReportHtml(
        AgroalDataSource olapDs,
        AgroalDataSource oltpDs,
        AgroalDataSource logsDs,
        DataQuery query,
        Integer organization,
        Integer tenant,
        String path,
        User user,
        Boolean withHeader
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getReportHtml'"
        );
    }

    @Override
    public String getReportHtml(
        AgroalDataSource olapDs,
        AgroalDataSource oltpDs,
        AgroalDataSource logsDs,
        DataQuery query,
        User user,
        Boolean withHeader
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getReportHtml'"
        );
    }

    @Override
    public String getReportCsv(
        AgroalDataSource olapDs,
        AgroalDataSource oltpDs,
        AgroalDataSource logsDs,
        DataQuery query,
        Integer organization,
        Integer tenant,
        String path,
        User user
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getReportCsv'"
        );
    }

    @Override
    public String getReportCsv(
        AgroalDataSource olapDs,
        AgroalDataSource oltpDs,
        AgroalDataSource logsDs,
        DataQuery query,
        User user
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getReportCsv'"
        );
    }

    @Override
    public String getReportFormat(
        AgroalDataSource olapDs,
        AgroalDataSource oltpDs,
        AgroalDataSource logsDs,
        DataQuery query,
        User user,
        String format
    ) {
        // TODO: Implement this method
        throw new UnsupportedOperationException(
            "Unimplemented method 'getReportCsv'"
        );
    }

    @Override
    public String getReportFormat(
        AgroalDataSource olapDs,
        AgroalDataSource oltpDs,
        AgroalDataSource logsDs,
        DataQuery query,
        Integer organization,
        Integer tenant,
        String path,
        User user,
        String format
    ) {
        // TODO: Implement this method
        throw new UnsupportedOperationException(
            "Unimplemented method 'getReportCsv'"
        );
    }
}
