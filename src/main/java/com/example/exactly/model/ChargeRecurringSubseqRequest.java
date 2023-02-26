package com.example.exactly.model;

import lombok.Data;

@Data
public class ChargeRecurringSubseqRequest {

    private Data data;

    @lombok.Data
    public static class Attributes {
        private String projectId;
        private String paymentMethod;
        private String amount;
        private String currency;
        private Recurring recurring;
        private String originalReferenceId;
    }

    @lombok.Data
    public static class Data {
        private String type;
        private Attributes attributes;
    }

    @lombok.Data
    public static class Recurring {
        private String initiator;
    }


}
