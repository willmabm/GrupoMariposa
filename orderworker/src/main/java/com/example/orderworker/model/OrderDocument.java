package com.example.orderworker.model;

import java.util.List;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "orders")
public class OrderDocument {
    @Id
    private String id;
    private String orderId;
    private String customerId;
    private List<Product> products;
}