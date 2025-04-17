package tn.platformMedical.mail_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetConfirmationRequest {
    private String email;
    private String firstname;
    private String lastname;
}