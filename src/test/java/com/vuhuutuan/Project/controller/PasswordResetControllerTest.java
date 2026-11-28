package com.vuhuutuan.Project.controller;

import com.vuhuutuan.Project.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class PasswordResetControllerTest {
    private final PasswordResetService service = mock(PasswordResetService.class);
    private final PasswordResetController controller = new PasswordResetController(service);

    @Test
    void validLinkDisplaysFormWithoutCaching() {
        var model = new ExtendedModelMap();
        var response = new MockHttpServletResponse();
        var redirect = new RedirectAttributesModelMap();

        assertEquals("datlaimatkhau", controller.resetForm("valid", model, response, redirect));
        assertTrue(model.containsAttribute("resetForm"));
        assertEquals("no-store", response.getHeader("Cache-Control"));
    }

    @Test
    void expiredLinkRedirectsToRequestPageWithoutShowingForm() {
        var model = new ExtendedModelMap();
        var response = new MockHttpServletResponse();
        var redirect = new RedirectAttributesModelMap();
        doThrow(new IllegalArgumentException("Liên kết đã hết hạn."))
                .when(service).validateResetLink("expired");

        assertEquals("redirect:/quen-mat-khau", controller.resetForm("expired", model, response, redirect));
        assertFalse(model.containsAttribute("resetForm"));
        assertEquals("Liên kết đã hết hạn.", redirect.getFlashAttributes().get("resetError"));
        assertEquals("no-store", response.getHeader("Cache-Control"));
    }
}
