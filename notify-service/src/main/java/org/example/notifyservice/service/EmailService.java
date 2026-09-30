package org.example.notifyservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${notification.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendBookingCreatedEmail(String recipient) {
        if (!StringUtils.hasText(recipient)) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(from)) {
            message.setFrom(from);
        }
        message.setTo(recipient);
        message.setSubject("Xác nhận đặt vé xem phim");
        message.setText("Đặt vé thành công. Yêu cầu đặt vé xem phim của bạn đã được hệ thống tiếp nhận");
        mailSender.send(message);
    }
}
