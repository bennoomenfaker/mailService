package tn.platformMedical.mail_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class PasswordResetRequest {
    private String token;
    private LocalDateTime expirationDate;
    private String email;
    private String firstname;
    private String lastname;
}
