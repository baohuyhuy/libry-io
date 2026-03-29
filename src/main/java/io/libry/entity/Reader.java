package io.libry.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity(name = "readers")
@Data
public class Reader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reader_id", nullable = false)
    private Long readerId;

    @NotBlank(message = "Full name cannot be blank")
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @NotBlank(message = "ID card number cannot be blank")
    @Column(name = "id_card_number", nullable = false, unique = true)
    private String idCardNumber;

    @NotNull(message = "Date of birth cannot be null")
    @Column(name = "dob", nullable = false)
    private LocalDate dob;

    @Column(name = "gender", length = 10)
    private String gender;

    @Email(message = "Email must be valid")
    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "address")
    private String address;

    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDate creationDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        if (creationDate == null) {
            creationDate = LocalDate.now();
        }
        if (expiryDate == null) {
            expiryDate = creationDate.plusMonths(48);
        }
    }
}
