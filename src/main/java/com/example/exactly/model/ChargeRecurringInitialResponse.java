package com.example.exactly.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChargeRecurringInitialResponse {
    private Data data;
    private List<Included> included;

    @lombok.Data
    public static class Data {
        private String type;
        private String id;
    }

    @lombok.Data
    public static class Included {
        private String type;
        private String id;
        private Attributes attributes;
    }

    @lombok.Data
    public static class Attributes {
        private String action;
        private String url;
    }
    
}