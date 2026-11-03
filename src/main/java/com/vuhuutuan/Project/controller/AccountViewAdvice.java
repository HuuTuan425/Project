package com.vuhuutuan.Project.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice(annotations = Controller.class)
public class AccountViewAdvice {
    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, "ROLE_ADMIN");
    }

    @ModelAttribute("isCashier")
    public boolean isCashier(Authentication authentication) {
        return hasRole(authentication, "ROLE_CASHIER");
    }

    @ModelAttribute("isWarehouse")
    public boolean isWarehouse(Authentication authentication) {
        return hasRole(authentication, "ROLE_WAREHOUSE");
    }

    @ModelAttribute("isManager")
    public boolean isManager(Authentication authentication) {
        return hasRole(authentication, "ROLE_MANAGER");
    }

    @ModelAttribute("currentUsername")
    public String currentUsername(Authentication authentication) {
        return authentication == null ? "" : authentication.getName();
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
