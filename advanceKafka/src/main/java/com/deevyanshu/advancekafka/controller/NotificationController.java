package com.deevyanshu.advancekafka.controller;

import com.deevyanshu.advancekafka.Model.NotificationRequest;
import com.deevyanshu.advancekafka.service.NotificationProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationProducer producer;

    @PostMapping("/post")
    public ResponseEntity<String> sendBulk(@RequestBody NotificationRequest request) {
        // Send a burst of 5 messages to see partitions in action
        for (int i = 1; i <= 5; i++) {
            NotificationRequest req = new NotificationRequest();
            // Vary the User ID so Kafka hashes them to different partitions
            req.setUserId(request.getUserId() + "_" + i);
            req.setMessage(request.getMessage() + " #" + i);
            producer.sendNotification(req);
        }
        return ResponseEntity.ok("Bulk notification process triggered.");
    }
}
