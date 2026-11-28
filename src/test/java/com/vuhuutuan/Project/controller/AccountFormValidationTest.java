package com.vuhuutuan.Project.controller;

import com.vuhuutuan.Project.model.AccountRole;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountFormValidationTest {
    @Test
    void emailIsRequiredWhenCreatingOrUpdatingAnAccount() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            var createForm = new CreateAccountForm();
            createForm.setUsername("employee");
            createForm.setPassword("password123");
            createForm.setRole(AccountRole.CASHIER);
            createForm.setFullName("Nhân viên");
            assertTrue(validator.validate(createForm).stream()
                    .anyMatch(error -> error.getPropertyPath().toString().equals("email")));
            createForm.setEmail("employee@example.com");
            assertFalse(validator.validate(createForm).stream()
                    .anyMatch(error -> error.getPropertyPath().toString().equals("email")));

            var updateForm = new UpdateAccountForm();
            updateForm.setFullName("Nhân viên");
            updateForm.setRole(AccountRole.CASHIER);
            assertTrue(validator.validate(updateForm).stream()
                    .anyMatch(error -> error.getPropertyPath().toString().equals("email")));
            updateForm.setEmail("employee@example.com");
            assertFalse(validator.validate(updateForm).stream()
                    .anyMatch(error -> error.getPropertyPath().toString().equals("email")));
        }
    }
}
