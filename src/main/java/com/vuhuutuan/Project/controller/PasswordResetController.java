package com.vuhuutuan.Project.controller;

import com.vuhuutuan.Project.service.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/quen-mat-khau")
    public String requestForm() {
        return "quenmatkhau";
    }

    @PostMapping("/quen-mat-khau")
    public String requestReset(@RequestParam(required = false) String email, Model model) {
        passwordResetService.requestReset(email);
        model.addAttribute("requestSubmitted", true);
        return "quenmatkhau";
    }

    @GetMapping("/dat-lai-mat-khau")
    public String resetForm(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        response.setHeader("Cache-Control", "no-store");
        try {
            passwordResetService.validateResetLink(token);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("resetError", exception.getMessage());
            return "redirect:/quen-mat-khau";
        }
        PasswordResetForm form = new PasswordResetForm();
        form.setToken(token);
        model.addAttribute("resetForm", form);
        return "datlaimatkhau";
    }

    @PostMapping("/dat-lai-mat-khau")
    public String reset(
            @Valid @ModelAttribute("resetForm") PasswordResetForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (form.getPassword() != null
                && form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            bindingResult.rejectValue("password", "tooLong",
                    "Mật khẩu không được dài quá 72 byte UTF-8.");
        }
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Mật khẩu xác nhận không khớp.");
        }
        if (bindingResult.hasErrors()) {
            return "datlaimatkhau";
        }

        try {
            passwordResetService.resetPassword(form.getToken(), form.getPassword());
            redirectAttributes.addFlashAttribute("passwordResetSuccess",
                    "Mật khẩu đã được cập nhật. Vui lòng đăng nhập lại.");
            return "redirect:/dang-nhap";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("resetError", exception.getMessage());
            return "datlaimatkhau";
        }
    }
}
