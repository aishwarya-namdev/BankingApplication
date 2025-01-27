package com.bankX.BankingApplication.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bankX.BankingApplication.model.Customer;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class); 

    @Autowired
    private JavaMailSender emailSender; 

    public void sendEmailNotification(Customer customer, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("your_email@example.com"); // Replace with your sender email
            message.setTo(customer.getEmail());
            message.setSubject(subject);
            message.setText(body);
            emailSender.send(message);
        } catch (MailException ex) {
            // Handle email sending exceptions (e.g., log, retry)
            logger.error("Failed to send email notification to " + customer.getEmail(), ex); 
        }
    }
}