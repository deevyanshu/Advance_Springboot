package com.deevyanshu.advancekafka.Model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequest {
    private String userId;
    private String message;


}
