package tn.platformMedical.mail_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import tn.platformMedical.mail_service.entity.PasswordResetConfirmationRequest;
import tn.platformMedical.mail_service.entity.PasswordResetRequest;
import tn.platformMedical.mail_service.service.EmailService;


@Slf4j
@Service
public class KafkaConsumerService {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public KafkaConsumerService(EmailService emailService) {
        this.emailService = emailService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = "password-reset-topic", groupId = "mail-service-group")
    public void listenPasswordResetEvent(String eventJson) {
        try {
            PasswordResetRequest event = objectMapper.readValue(eventJson, PasswordResetRequest.class);
            emailService.sendPasswordResetEmail(event);
        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement de réinitialisation de mot de passe : {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "password-reset-confirmation-topic", groupId = "mail-service-group")
    public void listenPasswordResetConfirmationEvent(String eventJson) {
        try {
            PasswordResetConfirmationRequest event = objectMapper.readValue(eventJson, PasswordResetConfirmationRequest.class);
            emailService.sendPasswordResetConfirmationEmail(event);
        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement de confirmation de réinitialisation du mot de passe : {}", e.getMessage());
        }
    }
}
