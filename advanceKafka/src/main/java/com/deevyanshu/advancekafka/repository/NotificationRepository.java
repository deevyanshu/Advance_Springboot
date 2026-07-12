package com.deevyanshu.advancekafka.repository;

import com.deevyanshu.advancekafka.Model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationLog, Long> {
}
