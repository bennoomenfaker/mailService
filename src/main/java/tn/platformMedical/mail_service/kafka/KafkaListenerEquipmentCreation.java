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
public class KafkaListenerEquipmentCreation {

    private final JavaMailSender emailSender;
    private final ObjectMapper objectMapper;
    private final TemplateEngine templateEngine;

    private static final String TOPIC_NAME = "equipment-service-create-equipment";

    @KafkaListener(topics = TOPIC_NAME, groupId = "email-service-group")
    public void listenEquipmentCreationEvent(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            log.info("Notification de création d'équipement reçue : {}", eventNode);

            // Extraction des données de l'événement EquipmentCreationEvent
            String equipmentName = eventNode.path("equipmentName").asText();
            String serialCode = eventNode.path("serialCode").asText();
            String hospitalName = eventNode.path("hospitalName").asText();
            String acquisitionDate = eventNode.path("acquisitionDate").asText();
            String amount = eventNode.path("amount").asText();
            String startDateWarranty = eventNode.path("startDateWarranty").asText();
            String endDateWarranty = eventNode.path("endDateWarranty").asText();

            // Extraction des destinataires
            List<String> emails = new ArrayList<>();
            if (eventNode.has("recipients")) {
                for (JsonNode emailNode : eventNode.get("recipients")) {
                    emails.add(emailNode.asText());
                }
            }

            if (emails.isEmpty()) {
                log.warn("Aucun email à notifier trouvé dans l'événement");
                return;
            }

            // Préparation du contexte pour le template Thymeleaf
            Context context = new Context();
            context.setVariable("equipmentName", equipmentName);
            context.setVariable("serialCode", serialCode);
            context.setVariable("hospitalName", hospitalName);
            context.setVariable("acquisitionDate", acquisitionDate);
            context.setVariable("amount", amount);
            context.setVariable("startDateWarranty", startDateWarranty);
            context.setVariable("endDateWarranty", endDateWarranty);
            context.setVariable("addToInventoryLink", "http://localhost:3000/manage-equipments/add-new-equipment-to-hospital?serialCode=" + serialCode);

            // Génération du contenu HTML pour l'email
            String htmlContent = templateEngine.process("equipmentCreation", context);

            // Envoi des emails
            for (String email : emails) {
                try {
                    sendEmailWithHtmlContent(
                            email,
                            "Nouvel équipement affecté à votre hôpital",
                            htmlContent
                    );
                    log.info("Email envoyé avec succès à {}", email);
                } catch (MessagingException e) {
                    log.error("Erreur lors de l'envoi à {}: {}", email, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Erreur de traitement du message: {}", e.getMessage());
            log.debug("Message complet: {}", message, e);
        }
    }

    private void sendEmailWithHtmlContent(String to, String subject, String content)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom("noreply@healthministry", "Ministère de la Santé");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true); // true = HTML

        emailSender.send(mimeMessage);
    }
}
