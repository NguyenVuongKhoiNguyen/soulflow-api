package com.poly.models.requests;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record SepayWebhookRequest(
    @NotNull @Positive Long id,
    @NotBlank @Size(max = 100) String gateway,
    @Size(max = 30) String transactionDate,
    @NotBlank @Size(max = 100) String accountNumber,
    @Size(max = 100) String subAccount,
    @Size(max = 255) String code,
    @Size(max = 4000) String content,
    @NotBlank @Pattern(regexp = "in|out") String transferType,
    @NotNull @Positive @Digits(integer = 18, fraction = 2) BigDecimal transferAmount,
    @Size(max = 255) String referenceCode
) {}
