package org.wso2.custom.elastic.publisher.publish;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.synapse.aspects.flow.statistics.publishing.PublishingEvent;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.wso2.carbon.das.data.publisher.util.PublisherUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ElasticStatisticsPublisher {

    private static final Log log = LogFactory.getLog(ElasticStatisticsPublisher.class);


    public static void process( PublishingFlow publishingFlow, TransportClient client ) {

        Map<String, Object> mapping = new HashMap<String, Object>();

        mapping.put("flowid", publishingFlow.getMessageFlowId());
        mapping.put("host", PublisherUtil.getHostAddress());

        mapping.put("type",publishingFlow.getEvent(0).getComponentType());
        mapping.put("name",publishingFlow.getEvent(0).getComponentName());

        long time = publishingFlow.getEvent(0).getStartTime();
        Date date = new Date(time);

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = dateFormat.format(date);

        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        String formattedTime = timeFormat.format(date);

        String timestampElastic = formattedDate + "T" + formattedTime + "Z";
        mapping.put("@timestamp",timestampElastic);

        boolean success = true;

        ArrayList<PublishingEvent> events = publishingFlow.getEvents();

        for ( PublishingEvent event:events ) {
//            log.info(event.getComponentType());
//            log.info(event.getComponentName());
//            log.info(event.getFaultCount());
//            log.info(event.getEntryPoint());

            if( event.getFaultCount()>0 ){
                success = false;
                break;
            }
        }

        mapping.put("success",success);

        log.info("FlowID : " + mapping.get("flowid"));
        log.info("Host : " + mapping.get("host"));
        log.info("Type : " + mapping.get("type"));
        log.info("Name : " + mapping.get("name"));
        log.info("Success : " + mapping.get("success"));
        log.info("Timestamp : " + mapping.get("@timestamp"));


        ObjectMapper objectMapper = new ObjectMapper();

        try {

            String jsonString = objectMapper.writeValueAsString(mapping);

            publish(jsonString, client);

        } catch (JsonProcessingException e) {

            e.printStackTrace();
            log.error("Error in converting to json string " + e);

        }


    }

    private static boolean publish ( String jsonToSend, TransportClient client ) {

        log.info(jsonToSend);

        try {

            IndexResponse response = client.prepareIndex("twitter", "tweet")
                    .setSource(jsonToSend)
                    .get();

            return true;

        } catch (NoNodeAvailableException e) {

            log.error("No available Elasticsearch Nodes to connect. Please give correct configurations and run Elasticsearch.");

            return false;

        }

    }
}
