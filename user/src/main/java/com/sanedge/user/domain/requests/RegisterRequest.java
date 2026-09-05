package com.sanedge.user.domain.requests;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @Size(min = 2, message = "First name must be at least 2 characters")
    @NotBlank(message = "First name wajib diisi")
    private String firstname;

    @NotBlank(message = "Username wajib diisi")
    private String username;

    @Size(min = 2, message = "Last name must be at least 2 characters")
    @NotBlank(message = "Last name wajib diisi")
    private String lastname;

    @Email(message = "Email tidak valid")
    @NotBlank(message = "Email wajib diisi")
    private String email;

    @Size(min = 6, message = "Password minimal 6 karakter")
    @NotBlank(message = "Password wajib diisi")
    private String password;

    @Size(min = 6, message = "Confirm password minimal 6 karakter")
    @NotBlank(message = "Confirm password wajib diisi")
    private String confirmPassword;

    private Set<String> roleNames;
}