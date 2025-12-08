package com.library.service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Service for handling email operations
 * @author Library Team
 * @version 1.0
 */
public class EmailService {
    private final String username;
    private final String password;

    public EmailService(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public EmailService() {
        try {
            // Load from current directory (project root)
            Dotenv dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMissing()
                    .load();

            this.username = dotenv.get("EMAIL_USERNAME");
            this.password = dotenv.get("EMAIL_PASSWORD");

            // Validate that credentials were loaded
            if (this.username == null || this.password == null) {
                throw new RuntimeException("Email credentials not found in .env file");
            }

            System.out.println("EmailService initialized with username: " + this.username);

        } catch (Exception e) {
            System.err.println("Failed to load email credentials from .env file: " + e.getMessage());
            System.err.println("Please create a .env file in the project root with EMAIL_USERNAME and EMAIL_PASSWORD");
            throw new RuntimeException("Email service initialization failed", e);
        }
    }

    /**
     * Sends an email to the specified recipient
     * @param to the recipient email address
     * @param subject the email subject
     * @param body the email body
     */
    public void sendEmail(String to, String subject, String body) {
        // SMTP configuration
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // Create a session with authentication
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            // Build email message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);

            // Send email
            Transport.send(message);

            System.out.println("Email sent successfully to " + to);

        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
