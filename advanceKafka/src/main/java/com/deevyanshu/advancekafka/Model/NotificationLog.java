package com.deevyanshu.advancekafka.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class NotificationLog {
    @Id
    @GeneratedValue
    private Long id;
    private String userId;
    private String message;
    private String status; // SUCCESS or FAILED
}
