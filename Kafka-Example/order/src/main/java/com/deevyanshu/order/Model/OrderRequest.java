package com.deevyanshu.order.Model;

import lombok.Data;

@Data
public class OrderRequest {
    private String productId;
    private double amount;
}
