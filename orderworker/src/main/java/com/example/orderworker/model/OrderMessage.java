package com.example.orderworker.model;

import java.util.List;
import lombok.Data;

@Data
public class OrderMessage {
    private String orderId;
    private String customerId;
    private List<String> productIds;
}