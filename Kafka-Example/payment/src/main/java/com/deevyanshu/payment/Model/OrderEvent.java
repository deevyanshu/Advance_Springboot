package com.deevyanshu.payment.Model;

import lombok.Data;

@Data
public class OrderEvent {
    private String id;
    private String productId;
    private double amount;
    private String status;
}
