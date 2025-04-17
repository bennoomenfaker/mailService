package tn.platformMedical.mail_service.controller;


import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.platformMedical.mail_service.entity.EmailDetails;
import tn.platformMedical.mail_service.entity.PasswordResetConfirmationRequest;
import tn.platformMedical.mail_service.entity.PasswordResetRequest;
import tn.platformMedical.mail_service.service.EmailService;

import java.util.concurrent.CompletableFuture;

@RestController
@AllArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")

public class EmailRestController {

    private final EmailService emailService;

    @PostMapping("/sendMail")
    public CompletableFuture<String> sendMail(@RequestBody EmailDetails details) {

        return    emailService.sendSimpleMail(details); // Lancement asynchrone
    }

    @PostMapping("/sendMailWithAttachment")
    public ResponseEntity<String> sendMailWithAttachment(@RequestBody EmailDetails details) {
        emailService.sendMailWithAttachment(details); // Lancement asynchrone
        return ResponseEntity.ok("Email is being sent."); // Réponse immédiate
    }

    @PostMapping("/send")
    public CompletableFuture<String> sendMail(
            @RequestParam(value = "file", required = false) MultipartFile[] file,
            @RequestParam String to,
            @RequestParam(required = false) String[] cc,
            @RequestParam String subject,
            @RequestParam String body) {

        return emailService.sendMail(file, to, cc, subject, body);
    }

    //forgot-password
    @PostMapping("/sendPasswordResetEmail")
    public ResponseEntity<String> sendPasswordResetEmail(@RequestBody PasswordResetRequest request) {
        try {
            // Utiliser les champs firstname et lastname si nécessaire
            emailService.sendPasswordResetEmail(request);
            System.out.println(request);
            return ResponseEntity.ok("Password reset email sent successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error sending password reset email: " + e.getMessage());
        }
    }

    //resetpassword
    @PostMapping("/confirmPasswordReset")
    public ResponseEntity<String> confirmPasswordReset(@RequestBody PasswordResetConfirmationRequest request) {
        try {
            emailService.sendPasswordResetConfirmationEmail(request);
            return ResponseEntity.ok("Password reset confirmation email sent successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error sending password reset confirmation email: " + e.getMessage());
        }
    }

}
