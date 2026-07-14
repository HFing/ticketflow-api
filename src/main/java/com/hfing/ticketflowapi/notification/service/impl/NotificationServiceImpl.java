package com.hfing.ticketflowapi.notification.service.impl;

import com.hfing.ticketflowapi.event.entity.Event;
import com.hfing.ticketflowapi.notification.dto.UserRegisteredEvent;
import com.hfing.ticketflowapi.notification.service.NotificationService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j(topic = "NOTIFICATION-SERVICE")
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromAddress;

    @Override
    public void sendRegistrationEmail(UserRegisteredEvent event) {
        log.info("Sending registration email to {}", event.email());
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            helper.setFrom(fromAddress);
            helper.setTo(event.email());
            helper.setSubject("Welcome to TicketFlow!");
            
            String htmlContent = buildWelcomeEmailHtml(event.firstName(), event.lastName());
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Registration email sent successfully to {}", event.email());
        } catch (Exception e) {
            log.error("Failed to send registration email to {}", event.email(), e);
        }
    }

    private String buildWelcomeEmailHtml(String firstName, String lastName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to TicketFlow</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background-color: #f4f6fa;
                        margin: 0;
                        padding: 0;
                        color: #333333;
                    }
                    .container {
                        max-width: 600px;
                        margin: 30px auto;
                        background-color: #ffffff;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 15px rgba(0,0,0,0.05);
                    }
                    .header {
                        background: linear-gradient(135deg, #6366f1 0%%, #a855f7 100%%);
                        padding: 40px 20px;
                        text-align: center;
                        color: #ffffff;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 28px;
                        font-weight: 700;
                        letter-spacing: -0.5px;
                    }
                    .header p {
                        margin: 5px 0 0 0;
                        font-size: 14px;
                        opacity: 0.9;
                    }
                    .content {
                        padding: 40px 30px;
                        line-height: 1.6;
                    }
                    .greeting {
                        font-size: 20px;
                        font-weight: 600;
                        color: #1f2937;
                        margin-bottom: 15px;
                    }
                    .body-text {
                        font-size: 16px;
                        color: #4b5563;
                        margin-bottom: 30px;
                    }
                    .cta-container {
                        text-align: center;
                        margin: 35px 0;
                    }
                    .cta-button {
                        background: linear-gradient(135deg, #6366f1 0%%, #a855f7 100%%);
                        color: #ffffff !important;
                        text-decoration: none;
                        padding: 14px 30px;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 16px;
                        display: inline-block;
                        box-shadow: 0 4px 10px rgba(99, 102, 241, 0.3);
                    }
                    .features {
                        border-top: 1px solid #e5e7eb;
                        padding-top: 25px;
                        margin-top: 25px;
                    }
                    .feature-item {
                        margin-bottom: 15px;
                        font-size: 14px;
                        color: #6b7280;
                    }
                    .feature-icon {
                        color: #6366f1;
                        font-weight: bold;
                        margin-right: 8px;
                    }
                    .footer {
                        background-color: #f9fafb;
                        padding: 25px 20px;
                        text-align: center;
                        font-size: 12px;
                        color: #9ca3af;
                        border-top: 1px solid #e5e7eb;
                    }
                    .footer a {
                        color: #6366f1;
                        text-decoration: none;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>TicketFlow</h1>
                        <p>Your Gateway to Seamless Event Experiences</p>
                    </div>
                    <div class="content">
                        <div class="greeting">Hello %s %s,</div>
                        <div class="body-text">
                            Thank you for joining TicketFlow! We are thrilled to have you as part of our community. 
                            Your account is now active and ready. Start exploring popular events, secure your tickets, 
                            and create unforgettable memories with just a few clicks.
                        </div>
                        <div class="cta-container">
                            <a href="http://localhost:8080" class="cta-button">Explore Events Now</a>
                        </div>
                        <div class="features">
                            <div class="feature-item">
                                <span class="feature-icon">✓</span> <strong>Easy Booking:</strong> Find and book your favorite events in seconds.
                            </div>
                            <div class="feature-item">
                                <span class="feature-icon">✓</span> <strong>Instant E-Tickets:</strong> Get your tickets delivered directly to your inbox and app wallet.
                            </div>
                            <div class="feature-item">
                                <span class="feature-icon">✓</span> <strong>Secure Payments:</strong> Multiple payment options with top-tier security standards.
                            </div>
                        </div>
                    </div>
                    <div class="footer">
                        <p>You received this email because you registered an account at TicketFlow.</p>
                        <p>&copy; 2026 TicketFlow Inc. All rights reserved.</p>
                        <p><a href="#">Privacy Policy</a> &bull; <a href="#">Support Center</a></p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(firstName, lastName);
    }
}