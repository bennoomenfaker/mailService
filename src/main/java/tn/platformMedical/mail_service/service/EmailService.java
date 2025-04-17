package tn.platformMedical.mail_service.service;

import org.springframework.web.multipart.MultipartFile;
import tn.platformMedical.mail_service.entity.EmailDetails;
import tn.platformMedical.mail_service.entity.PasswordResetConfirmationRequest;
import tn.platformMedical.mail_service.entity.PasswordResetRequest;

import java.util.concurrent.CompletableFuture;

public interface EmailService {

    // Method
    // To send a simple email
    CompletableFuture<String> sendSimpleMail(EmailDetails details);

    // Method
    // To send an email with attachment
    String sendMailWithAttachment(EmailDetails details);


    public CompletableFuture<String> sendMail(MultipartFile[] files, String to, String[] cc, String subject, String body);
    void sendPasswordResetEmail(PasswordResetRequest request);
    void sendPasswordResetConfirmationEmail(PasswordResetConfirmationRequest request);

}