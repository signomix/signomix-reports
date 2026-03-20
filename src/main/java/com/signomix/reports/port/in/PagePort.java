package com.signomix.reports.port.in;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.signomix.common.User;
import com.signomix.reports.domain.dashboard.PageBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PagePort {

    @Inject
    Logger logger;

    @Inject
    PageBuilder pageBuilder;

    public String getPageSource(
        User user,
        String definition,
        boolean header,
        boolean title
    ) {
        try {
            return pageBuilder.buildPage(user, definition, header, title);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public String getPageSourceById(
        User user,
        String id,
        boolean header,
        boolean title,
        String timeZone
    ) {
        try {
            return pageBuilder.buildPageById(user, id, header, title, timeZone);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
}
