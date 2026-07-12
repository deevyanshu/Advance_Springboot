package com.deevyanshu.advancekafka.Model;

import lombok.Data;

@Data
public class NotificationRequest {
    private String userId;
    private String message;
}
