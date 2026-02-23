package com.signomix.reports.port.in;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.signomix.reports.domain.dashboard.PageBuilder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PagePort {

    @Inject
     Logger logger;


    public String getPageSource(String definition) {
        try {
            return PageBuilder.buildPage(definition);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
}
