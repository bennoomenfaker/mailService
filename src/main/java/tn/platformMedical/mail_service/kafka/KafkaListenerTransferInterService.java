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
public class KafkaListenerTransferInterService {

    private final JavaMailSender emailSender;
    private final ObjectMapper objectMapper;
    private final TemplateEngine templateEngine;

    private static final String TOPIC_NAME = "equipment-service-transfer-events";  // Nouveau topic

    @KafkaListener(topics = TOPIC_NAME, groupId = "email-service-group")
    public void listenEquipmentTransferEvent(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            log.info("Received event: {}", eventNode);

            // Champs obligatoires avec vérification
            String serialCode = eventNode.path("serialCode").asText();
            String oldServiceName = eventNode.path("oldServiceId").asText();
            String newServiceName = eventNode.path("newServiceId").asText();

            // Champs optionnels avec valeurs par défaut
            String firstName = eventNode.path("firstName").asText("Utilisateur inconnu");
            String lastName = eventNode.path("lastName").asText("");
            String description = eventNode.path("description").asText("Aucune description fournie");
            String equipmentName = eventNode.path("name").asText(serialCode); // Utilise serialCode comme fallback

            // Récupération des emails
            List<String> emails = new ArrayList<>();
            if (eventNode.has("emailsToNotify")) {
                for (JsonNode emailNode : eventNode.get("emailsToNotify")) {
                    emails.add(emailNode.asText());
                }
            }

            // Vérification des données minimales
            if (emails.isEmpty()) {
                log.warn("Aucun email à notifier trouvé dans l'événement");
                return;
            }

            // Génération du contenu HTML
            Context context = new Context();
            context.setVariable("serialCode", serialCode);
            context.setVariable("equipmentName", equipmentName);
            context.setVariable("oldServiceName", oldServiceName);
            context.setVariable("newServiceName", newServiceName);
            context.setVariable("description", description);
            context.setVariable("firstName", firstName);
            context.setVariable("lastName", lastName);
            context.setVariable("equipmentLink", "http://localhost:3000");
            context.setVariable("addEquipmentLink", "http://localhost:3000/manage-equipments/add-new-equipment-to-service?serialCode=" + serialCode); // Ajoute serialCode à l'URL

            String htmlContent = templateEngine.process("changementService", context);

            // Envoi des emails
            for (String email : emails) {
                try {
                    sendEmailWithHtmlContent(email,
                            "Transfert d'équipement entre services: " + equipmentName,
                            htmlContent);
                } catch (MessagingException e) {
                    log.error("Erreur lors de l'envoi à {}: {}", email, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Erreur de traitement du message: {}", e.getMessage());
            log.debug("Message complet: {}", message, e);
        }
    }

    private void sendEmailWithHtmlContent(String to, String subject, String content) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

        // Définir l'expéditeur avec un nom personnalisé
        String senderEmail = "noreplay@healthministry"; // L'email d'expéditeur
        String senderName = "Ministère de Santé"; // Nom d'expéditeur
        helper.setFrom(senderEmail, senderName);

        // Définir les autres paramètres
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true); // 'true' pour indiquer qu'il s'agit d'un email HTML

        // Envoi de l'email
        emailSender.send(mimeMessage);
    }

}
