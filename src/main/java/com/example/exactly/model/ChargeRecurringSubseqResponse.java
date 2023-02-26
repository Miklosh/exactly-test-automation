package com.example.exactly.model;

import lombok.Data;

@Data
public class ChargeRecurringSubseqResponse {

    private Data data;

    @lombok.Data
    public static class Data {
        private String type;
        private String id;
    }

}
