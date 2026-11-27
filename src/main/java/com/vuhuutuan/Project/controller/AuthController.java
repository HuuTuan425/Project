package com.vuhuutuan.Project.controller;

import com.vuhuutuan.Project.model.AppUser;
import com.vuhuutuan.Project.model.SessionUser;
import com.vuhuutuan.Project.repository.AppUserRepository;
import com.vuhuutuan.Project.config.SessionAuthInterceptor;
import com.vuhuutuan.Project.service.AccountService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;

    public AuthController(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            AccountService accountService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountService = accountService;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        SessionUser user = (SessionUser) session.getAttribute(SessionAuthInterceptor.SESSION_USER_ATTRIBUTE);
        if (user == null) {
            return "redirect:/dang-nhap";
        }
        return defaultPage(user);
    }

    @GetMapping("/dang-nhap")
    public String login() {
        return "taikhoandangnhap";
    }

    @PostMapping("/dang-nhap")
    public String authenticate(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request,
            HttpSession session
    ) {
        AppUser account = appUserRepository.findByUsernameIgnoreCase(username.trim()).orElse(null);
        if (account == null
                || account.isDeleted()
                || !account.isEnabled()
                || !passwordEncoder.matches(password, account.getPasswordHash())) {
            return "redirect:/dang-nhap?error";
        }

        accountService.recordLogin(account.getUsername());
        request.changeSessionId();
        SessionUser sessionUser = new SessionUser(
                account.getId(), account.getUsername(), account.getRole(), account.getPasswordHash());
        session.setAttribute(SessionAuthInterceptor.SESSION_USER_ATTRIBUTE, sessionUser);
        return "redirect:" + defaultPage(sessionUser);
    }

    @PostMapping("/dang-xuat")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/dang-nhap?logout";
    }

    @GetMapping("/khong-co-quyen")
    public String accessDenied() {
        return "taikhoankhongquyen";
    }

    private String defaultPage(SessionUser user) {
        if (user.isWarehouse() || user.isPurchasing()) {
            return "/nhap-hang";
        }
        if (user.isManager()) {
            return "/hoa-don/bao-cao";
        }
        return "/hoa-don/lap";
    }
}
