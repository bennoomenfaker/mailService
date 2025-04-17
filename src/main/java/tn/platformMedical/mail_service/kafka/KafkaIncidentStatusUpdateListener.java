package tn.platformMedical.mail_service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class KafkaIncidentStatusUpdateListener  {

    private final JavaMailSender emailSender;
    private final ObjectMapper objectMapper;
    private final TemplateEngine templateEngine;

    private static final String TOPIC = "incident-status-update-events";

    @KafkaListener(topics = TOPIC, groupId = "email-service-group")
    public void listenIncidentStatusUpdate(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            log.info("📩 Mise à jour d'incident reçue : {}", root);

            String equipmentName = root.path("equipmentName").asText();
            String serialCode = root.path("serialCode").asText();
            String oldStatus = root.path("oldStatus").asText();
            String newStatus = root.path("newStatus").asText();
            String description = root.path("description").asText();
            String initiatorFullName = root.path("initiatorFirstName").asText() + " " + root.path("initiatorLastName").asText();
            String initiatorEmail = root.path("initiatorEmail").asText();

            List<String> recipients = new ArrayList<>();
            if (root.has("recipients")) {
                for (JsonNode emailNode : root.get("recipients")) {
                    recipients.add(emailNode.asText());
                }
            }

            // Préparation du contexte Thymeleaf
            Context context = new Context();
            context.setVariable("equipmentName", equipmentName);
            context.setVariable("serialCode", serialCode);
            context.setVariable("oldStatus", oldStatus);
            context.setVariable("newStatus", newStatus);
            context.setVariable("description", description);
            context.setVariable("initiatorFullName", initiatorFullName);
            context.setVariable("initiatorEmail", initiatorEmail);

            String subject = "📋 Mise à jour d’un incident sur l’équipement " + equipmentName;
            String htmlContent = templateEngine.process("incident-status-update", context);

            for (String recipient : recipients) {
                try {
                    sendEmailWithHtmlContent(recipient, subject, htmlContent);
                } catch (MessagingException e) {
                    log.error("❌ Échec d'envoi de l'email à {} : {}", recipient, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement de l’événement d’incident : {}", e.getMessage(), e);
        }
    }

    private void sendEmailWithHtmlContent(String to, String subject, String content)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

        String senderEmail = "noreply@healthministry";
        String senderName = "Ministère de la Santé";
        helper.setFrom(senderEmail, senderName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true); // Enable HTML

        emailSender.send(mimeMessage);
    }
}
