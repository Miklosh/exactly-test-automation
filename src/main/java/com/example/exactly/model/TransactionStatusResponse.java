package com.example.exactly.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class TransactionStatusResponse {
    private Data data;

    @lombok.Data
    public static class Action{
        private String type;
        private Attributes attributes;
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
        private Date createdAt;
        private List<Action> actions;
        private PaymentMethod paymentMethod;
        private String action;
        private String url;
    }

    @lombok.Data
    public static class Data{
        private String type;
        private String id;
        private Attributes attributes;
    }

    @lombok.Data
    public static class PaymentMethod{
        private String type;
    }

    @lombok.Data
    public static class Processing{
        private String currency;
        private String amount;
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
