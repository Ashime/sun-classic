package com.valiantgaming.webserver.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

// firstName and the security question/answer pairs are nullable on Profile (see the Profile table's
// Allow Nulls column) - only the fields below are left @NotBlank/required to match that schema.
public record RegisterAccountRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String email,
        String firstName,
        @NotBlank String lastName,
        @Min(1) @Max(12) int birthMonth,
        @Min(1) @Max(31) int birthDay,
        String securityQuestion1,
        String answer1,
        String securityQuestion2,
        String answer2,
        String securityQuestion3,
        String answer3
) {}
