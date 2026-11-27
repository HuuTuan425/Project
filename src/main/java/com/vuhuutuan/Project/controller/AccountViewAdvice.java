package com.vuhuutuan.Project.controller;

import com.vuhuutuan.Project.config.SessionAuthInterceptor;
import com.vuhuutuan.Project.model.SessionUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice(annotations = Controller.class)
public class AccountViewAdvice {
    @ModelAttribute("isAdmin")
    public boolean isAdmin(HttpSession session) {
        SessionUser user = getUser(session);
        return user != null && user.isAdmin();
    }

    @ModelAttribute("isCashier")
    public boolean isCashier(HttpSession session) {
        SessionUser user = getUser(session);
        return user != null && user.isCashier();
    }

    @ModelAttribute("isWarehouse")
    public boolean isWarehouse(HttpSession session) {
        SessionUser user = getUser(session);
        return user != null && user.isWarehouse();
    }

    @ModelAttribute("isPurchasing")
    public boolean isPurchasing(HttpSession session) {
        SessionUser user = getUser(session);
        return user != null && user.isPurchasing();
    }

    @ModelAttribute("isManager")
    public boolean isManager(HttpSession session) {
        SessionUser user = getUser(session);
        return user != null && user.isManager();
    }

    @ModelAttribute("currentUsername")
    public String currentUsername(HttpSession session) {
        SessionUser user = getUser(session);
        return user == null ? "" : user.username();
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    private SessionUser getUser(HttpSession session) {
        return (SessionUser) session.getAttribute(SessionAuthInterceptor.SESSION_USER_ATTRIBUTE);
    }
}
