package com.example.exactly.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionStatusResponse {
    private Data data;

    @lombok.Data
    public static class Data{
        private String type;
        private String id;
        private Attributes attributes;
    }

    @lombok.Data
    public static class Action{
        private String type;
        private Attributes attributes;
    }

    @lombok.Data
    public static class Customer {
        private String ipCountry;
    }

    @lombok.Data
    public static class Attributes{
        private Recurring recurring;
        private String environmentMode;
        private String status;
        private Processing processing;
        private Source source;
        private String originalId;
        private String projectId;
        private String referenceId;
        private Date createdAt;
        private List<Action> actions;
        private PaymentMethod paymentMethod;
        private String action;
        private String url;
        private Customer customer;
    }

    @lombok.Data
    public static class PaymentMethod{
        private String type;
    }

    @lombok.Data
    public static class Processing{
        private String currency;
        private String amount;
        private String resultCode;
        private Date processedAt;
    }

    @lombok.Data
    public static class Recurring{
        private String scenario;
        private List<String> expectedInitiators;
        private String initiator;
    }

    @lombok.Data
    public static class Source{
        private String iin;
        private String last4;
        private String issuerCountry;
        private String brand;
    }

}
