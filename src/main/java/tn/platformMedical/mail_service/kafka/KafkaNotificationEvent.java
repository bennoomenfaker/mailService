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
public class KafkaNotificationEvent {

    private final JavaMailSender emailSender;
    private final ObjectMapper objectMapper;
    private final TemplateEngine templateEngine;

    private static final String TOPIC_NAME = "notification-events-mail";

    @KafkaListener(topics = TOPIC_NAME, groupId = "email-service-group")
    public void listenMaintenanceNotification(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            log.info("✅ Notification reçue : {}", eventNode);

            String notificationMessage = eventNode.path("message").asText();
            String subject = eventNode.path("title").asText("Notification de Maintenance"); // Utiliser le titre de l'événement

            List<String> recipients = new ArrayList<>();
            if (eventNode.has("recipients")) {
                for (JsonNode emailNode : eventNode.get("recipients")) {
                    recipients.add(emailNode.asText());
                }
            }

            if (recipients.isEmpty()) {
                log.warn("❗ Aucun destinataire trouvé dans la notification.");
                return;
            }

            Context context = new Context();
            context.setVariable("notificationMessage", notificationMessage);

            String templateName = null;

            if (notificationMessage.contains("pièce de rechange")) {
                String[] parts = notificationMessage.split(" \\[|\\] à ");
                if (parts.length == 3) {
                    String sparePartInfo = parts[1];
                    String hospitalName = parts[2].substring(0, parts[2].indexOf(" est prévue"));

                    String[] sparePartDetails = sparePartInfo.split(" - ");
                    if (sparePartDetails.length == 2) {
                        context.setVariable("entityType", "pièce de rechange");
                        context.setVariable("entityName", sparePartDetails[1]);
                        context.setVariable("serialCode", sparePartDetails[0]);
                        context.setVariable("hospitalName", hospitalName);
                        templateName = "maintenance-entity";
                    }
                }
            } else if (notificationMessage.contains("équipement")) {
                String[] parts = notificationMessage.split(" \\[|\\] à ");
                if (parts.length == 3) {
                    String equipmentInfo = parts[1];
                    String hospitalName = parts[2].substring(0, parts[2].indexOf(" est prévue"));

                    String[] equipmentDetails = equipmentInfo.split(" - ");
                    if (equipmentDetails.length == 2) {
                        context.setVariable("entityType", "équipement");
                        context.setVariable("entityName", equipmentDetails[1]);
                        context.setVariable("serialCode", equipmentDetails[0]);
                        context.setVariable("hospitalName", hospitalName);
                        templateName = "maintenance-entity";
                    } else {
                        log.warn("❗ Format de détails d'équipement non reconnu : {}", equipmentInfo);
                        templateName = "maintenance-default"; // Template par défaut
                    }
                }
            }

            String htmlContent = templateEngine.process(templateName, context);

            for (String recipient : recipients) {
                try {
                    sendEmailWithHtmlContent(recipient, subject, htmlContent);
                } catch (MessagingException e) {
                    log.error("Erreur lors de l'envoi à {}: {}", recipient, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement de la notification Kafka : {}", e.getMessage(), e);
        }
    }

    private void sendEmailWithHtmlContent(String to, String subject, String content)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

        String senderEmail = "noreplay@healthministry";
        String senderName = "Ministère de Santé";
        helper.setFrom(senderEmail, senderName);

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true); // HTML content

        emailSender.send(mimeMessage);
    }
}