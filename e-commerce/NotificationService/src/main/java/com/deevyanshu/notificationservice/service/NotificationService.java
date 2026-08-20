package com.deevyanshu.notificationservice.service;

import com.deevyanshu.notificationservice.order.OrderEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final JavaMailSender javaMailSender;

    public NotificationService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @KafkaListener(topics="order-placed")
    public void listen(OrderEvent orderEvent) {
        // Process the order event and send notification
        System.out.println("Received order event: " + orderEvent);
        // Here you can implement the logic to send email or SMS notification
        MimeMessagePreparator messagePreparator= mimeMessage ->{
            MimeMessageHelper messageHelper= new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom("test@gmail.com");
            messageHelper.setTo(orderEvent.getEmail());
            messageHelper.setSubject("Order Placed");
            messageHelper.setText("Your order with ID "+orderEvent.getOrderNumber()+" has been placed successfully.");
        };
        try{
            javaMailSender.send(messagePreparator);
            System.out.println("Email sent successfully.");
        }catch (Exception e) {
            System.err.println("Error occurred while sending email: " + e.getMessage());
        }
    }
}
