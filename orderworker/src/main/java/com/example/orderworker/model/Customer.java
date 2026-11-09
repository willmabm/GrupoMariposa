package com.example.orderworker.model;

import lombok.Data;

@Data
public class Customer {
    private String customerId;
    private String name;
    private boolean active;
}