package tn.platformMedical.mail_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import tn.platformMedical.mail_service.entity.EmailDetails;
import tn.platformMedical.mail_service.entity.PasswordResetConfirmationRequest;
import tn.platformMedical.mail_service.entity.PasswordResetRequest;

import java.io.File;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailServiceImpl implements  EmailService {
    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired private SpringTemplateEngine templateEngine;
    @Value("${spring.mail.username}") private String sender;

    @Override
    @Async
    public CompletableFuture<String> sendSimpleMail(EmailDetails details) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            // Créer le contexte et ajouter les variables nécessaires
            Context context = new Context();
            context.setVariable("subject", details.getSubject());
            context.setVariable("msgBody", details.getMsgBody());
            context.setVariable("recipient", details.getRecipient());
            context.setVariable("sender", sender);

            String emailContent = templateEngine.process("emailTemplate", context);

            mimeMessageHelper.setFrom(sender);
            mimeMessageHelper.setTo(details.getRecipient());
            mimeMessageHelper.setSubject(details.getSubject());
            mimeMessageHelper.setText(emailContent, true); // 'true' signifie envoi HTML

            // Envoyer l'email
            javaMailSender.send(mimeMessage);
            return CompletableFuture.completedFuture( "Mail Sent Successfully...");
        } catch (MessagingException e) {
            return CompletableFuture.completedFuture( "Error while sending mail!!!");
        }
    }

    @Override
    public String sendMailWithAttachment(EmailDetails details) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            // Créer le contexte et ajouter les variables nécessaires
            Context context = new Context();
            context.setVariable("subject", details.getSubject());
            context.setVariable("msgBody", details.getMsgBody());
            context.setVariable("sender", sender);
            context.setVariable("recipient", details.getRecipient());


            String emailContent = templateEngine.process("emailTemplate", context);

            mimeMessageHelper.setFrom(sender);
            mimeMessageHelper.setTo(details.getRecipient());
            mimeMessageHelper.setSubject(details.getSubject());
            mimeMessageHelper.setText(emailContent, true); // 'true' signifie envoi HTML

            // Ajouter une pièce jointe
            FileSystemResource file = new FileSystemResource(new File(details.getAttachment()));
            mimeMessageHelper.addAttachment(file.getFilename(), file);

            javaMailSender.send(mimeMessage);
            return "Mail sent Successfully";
        } catch (MessagingException e) {
            return "Error while sending mail!!!";
        }

    }


    @Override
    @Async
    public CompletableFuture<String> sendMail(MultipartFile[] files, String to, String[] cc, String subject, String body) {
        if (body == null || body.isEmpty()) {
            return CompletableFuture.completedFuture("Le corps de l'email ne peut pas être vide.");
        }

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            mimeMessageHelper.setFrom(sender);
            mimeMessageHelper.setTo(to);
            if (cc != null && cc.length > 0) {
                mimeMessageHelper.setCc(cc);
            }
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setText(body, true); // true for HTML content

            if (files != null && files.length > 0) {
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        mimeMessageHelper.addAttachment(
                                file.getOriginalFilename(),
                                new ByteArrayResource(file.getBytes())
                        );
                    }
                }
            }

            javaMailSender.send(mimeMessage);
            return CompletableFuture.completedFuture("Email envoyé avec succès !");

        } catch (Exception e) {

            e.printStackTrace();
            return CompletableFuture.completedFuture("Erreur lors de l'envoi de l'email : " + e.getMessage());
        }
    }



    @Override
    @Async
    public void sendPasswordResetEmail(PasswordResetRequest request) {
        // Créer le contexte pour le template Thymeleaf
        Context context = new Context();
        context.setVariable("recipient", request.getEmail());
        context.setVariable("subject", "Réinitialisation de mot de passe");
        context.setVariable("msgBody", "Cliquez sur le lien ci-dessous pour réinitialiser votre mot de passe.");
        context.setVariable("token", request.getToken()); // Ajoutez le token ici
        context.setVariable("firstname", request.getFirstname()); // Ajoutez le prénom
        context.setVariable("lastname", request.getLastname());   // Ajoutez le nom

        // Générer le contenu HTML à partir du template
        String htmlContent = templateEngine.process("emailTemplate", context); // 'emailTemplate' est le nom du fichier HTML sans l'extension

        // Envoyer l'email en tant que message MIME
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(request.getEmail());
            helper.setSubject("Réinitialisation de mot de passe");
            helper.setText(htmlContent, true); // Le second paramètre `true` signifie que le contenu est en HTML

            javaMailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send password reset email.");
        }
    }

    @Async
    public void sendPasswordResetConfirmationEmail(PasswordResetConfirmationRequest request) {
        // Créer le contexte pour le template Thymeleaf
        Context context = new Context();
        context.setVariable("firstname", request.getFirstname());
        context.setVariable("lastname", request.getLastname());
        context.setVariable("subject", "Confirmation de réinitialisation de mot de passe");
        context.setVariable("confirmationMessage", "Votre mot de passe a été réinitialisé avec succès.");

        // Générer le contenu HTML à partir du template
        String htmlContent = templateEngine.process("confirmationEmailTemplate", context);

        // Envoi de l'email
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(request.getEmail());
            helper.setSubject("Confirmation de réinitialisation de mot de passe");
            helper.setText(htmlContent, true); // Le second paramètre `true` signifie que le contenu est en HTML

            javaMailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send password reset confirmation email.");
        }
    }
}