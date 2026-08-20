package com.deevyanshu.notificationservice.order;

public class OrderEvent {
    private String orderNumber;
    private String email;

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public OrderEvent(String orderNumber, String email) {
        this.orderNumber = orderNumber;
        this.email = email;
    }
}
