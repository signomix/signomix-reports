/*
 * Copyright 2021 Grzegorz Skorupa <g.skorupa at gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.signomix.reports.pre.kanarek;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signomix.common.db.Dataset;
import com.signomix.common.db.DatasetHeader;
import com.signomix.common.db.DatasetRow;
import com.signomix.common.db.ReportResult;
import com.signomix.common.iot.ChannelData;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KanarekFormatter {

    Logger logger = Logger.getLogger(KanarekFormatter.class.getName());

    //private Map args;

    /**
     * Translates response result
     *
     * @param data        response data
     *
     * @return response as JSON string
     */
    public String format(ReportResult data) {
        // logger.info("formatting Kanarek response");
        //args.clear();
        /*
         * args.put(JsonWriter.PRETTY_PRINT, true);
         * args.put(JsonWriter.DATE_FORMAT, "dd/MMM/yyyy:kk:mm:ss Z");
         * args.put(JsonWriter.SKIP_NULL_FIELDS, true);
         * args.put(JsonWriter.TYPE, false);
         */
        ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(
            new java.text.SimpleDateFormat("dd/MMM/yyyy:kk:mm:ss Z")
        );
        mapper.setDefaultPrettyPrinter(
            new com.fasterxml.jackson.core.util.DefaultPrettyPrinter()
        );
        KanarekDto kdto = new KanarekDto();
        ChannelData cdata;
        // String ownerName = result.getHeaders().getFirst("X-Group-Name");
        // String groupHref = result.getHeaders().getFirst("X-Group-Dashboard-Href");
        String ownerName = "Otwarta Sieć Rzeczy - udostępnione dla Kanarka";
        String groupHref = "https://signomix.com/gt/asmp";

        String channelName;
        DatasetHeader header = data.headers.get(0);
        ArrayList<String> columns = header.columns;

        for (int k = 0; k < data.datasets.size(); k++) {
            Dataset dataset = data.datasets.get(k);
            for (int i = 0; i < dataset.data.size(); i++) {
                try {
                    KanarekStationDto kStation = new KanarekStationDto();
                    if (null != ownerName && !ownerName.isBlank()) {
                        kStation.owner = ownerName;
                    }
                    if (null != groupHref) {
                        kStation.href = groupHref;
                    }
                    DatasetRow row = dataset.data.get(i);
                    try {
                        kStation.id = Long.parseLong(dataset.eui, 16);
                    } catch (Exception e) {
                        logger.warn("malformed station EUI: " + e.getMessage());
                    }
                    kStation.name = dataset.eui;
                    for (int j = 0; j < columns.size(); j++) {
                        channelName = columns.get(j);
                        switch (channelName.toUpperCase()) {
                            case "LATITUDE":
                            case "GPS_LATITUDE":
                            case "LAT":
                                kStation.lat = (Double) row.values.get(j);
                                break;
                            case "LONGITUDE":
                            case "GPS_LONGITUDE":
                            case "LON":
                                kStation.lon = (Double) row.values.get(j);
                                break;
                            default:
                                KanarekValue kv = new KanarekValue(
                                    channelName,
                                    (Double) row.values.get(j),
                                    row.timestamp
                                );
                                if (null != kv.type) {
                                    kStation.values.add(kv);
                                }
                        }
                    }
                    if (
                        null != kStation.id &&
                        null != kStation.lat &&
                        null != kStation.lon
                    ) {
                        kdto.stations.add(kStation);
                    } else {
                        logger.warn(
                            "Unable to add Kanarek Station data - requred values not set."
                        );
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        //return JsonWriter.objectToJson(kdto, args) + "\r\n";
        try {
            return mapper.writeValueAsString(kdto);
        } catch (Exception e) {
            logger.error("KanarekFormatter: " + e.getMessage());
            return "";
        }
    }
}
