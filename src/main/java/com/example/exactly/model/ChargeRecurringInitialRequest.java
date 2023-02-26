package com.example.exactly.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

// import com.fasterxml.jackson.databind.ObjectMapper; // version 2.11.1
// import com.fasterxml.jackson.annotation.JsonProperty; // version 2.11.1
/* ObjectMapper om = new ObjectMapper();
Root root = om.readValue(myJsonString, Root.class); */
@Data
public class ChargeRecurringInitialRequest {
    private Data data;

    @lombok.Data
    public static class Attributes {
        private String projectId;
        private String paymentMethod;
        private String amount;
        private String currency;
        private Recurring recurring;
        private String referenceId;
    }

    @lombok.Data
    public static class Data {
        private String type;
        private Attributes attributes;
    }

    @lombok.Data
    public static class Recurring {
        private String scenario;
        private List<String> expectedInitiators;
    }
}
