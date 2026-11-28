package com.vuhuutuan.Project.controller;

import com.vuhuutuan.Project.repository.MatHangRepository;
import com.vuhuutuan.Project.service.PurchasingService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SupplierProposalValidationTest {
    @Test
    void invalidPhoneAndEmailShowErrorsWithoutSaving() {
        var service = mock(PurchasingService.class);
        var controller = new PurchasingController(service, mock(MatHangRepository.class));
        var form = new SupplierProposalForm();
        form.setName("Nhà cung cấp");
        form.setPhone("123abc");
        form.setEmail("khong-hop-le");
        var errors = new BeanPropertyBindingResult(form, "supplierProposalForm");

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            new SpringValidatorAdapter(factory.getValidator()).validate(form, errors);
        }

        assertNotNull(errors.getFieldError("phone"));
        assertNotNull(errors.getFieldError("email"));
        assertEquals("Thông tin không hợp lệ.", errors.getFieldError("phone").getDefaultMessage());
        assertEquals("Thông tin không hợp lệ.", errors.getFieldError("email").getDefaultMessage());
        assertEquals("nhaphang/nhacungcap", controller.proposeSupplier(
                form, errors, new ExtendedModelMap(), new MockHttpSession(),
                new RedirectAttributesModelMap()));
        verify(service, never()).createSupplierProposal(any(), anyString());
    }

    @Test
    void validAndEmptyOptionalContactDetailsAreAccepted() {
        var form = new SupplierProposalForm();
        form.setName("Nhà cung cấp");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertFalse(validator.validate(form).iterator().hasNext());
            form.setPhone("0981234567");
            form.setEmail("supplier@example.com");
            assertFalse(validator.validate(form).iterator().hasNext());
        }
    }
}
