package tn.platformMedical.mail_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailDTO {
    private String email;
    private String subject;
    private String message;
}