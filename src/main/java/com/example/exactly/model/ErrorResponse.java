package com.example.exactly.model;

import lombok.Data;

import java.util.ArrayList;

@Data
public class ErrorResponse {

    private ArrayList<Error> errors;

    @Data
    private class Error {
        private String code;
        private String title;
        private Meta meta;
    }

    @Data
    private class Meta {
        private String referenceId;
    }

}
