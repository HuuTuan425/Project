package com.vuhuutuan.Project.config;

import com.vuhuutuan.Project.model.AccountRole;
import com.vuhuutuan.Project.repository.AppUserRepository;
import com.vuhuutuan.Project.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class AccountBootstrap implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(AccountBootstrap.class);

    private final String username;
    private final String password;
    private final String email;
    private final AppUserRepository appUserRepository;
    private final AccountService accountService;

    public AccountBootstrap(
            @Value("${app.bootstrap-admin.username:}") String username,
            @Value("${app.bootstrap-admin.password:}") String password,
            @Value("${app.bootstrap-admin.email:}") String email,
            AppUserRepository appUserRepository,
            AccountService accountService
    ) {
        this.username = username.trim();
        this.password = password;
        this.email = email.trim();
        this.appUserRepository = appUserRepository;
        this.accountService = accountService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() && password.isBlank()) {
            logger.warn("No initial administrator configured. Set BOOTSTRAP_ADMIN_USERNAME, "
                    + "BOOTSTRAP_ADMIN_PASSWORD, and BOOTSTRAP_ADMIN_EMAIL.");
            return;
        }
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Bootstrap administrator username and password must both be set.");
        }
        if (username.length() > 50) {
            throw new IllegalStateException("Bootstrap administrator username cannot exceed 50 characters.");
        }
        if (password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Bootstrap administrator password must be at least 8 characters and at most 72 UTF-8 bytes.");
        }
        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        if (email.isBlank()) {
            throw new IllegalStateException("Bootstrap administrator email must be set.");
        }
        if (email.length() > 150 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalStateException("Bootstrap administrator email must be valid and at most 150 characters.");
        }

        accountService.createAccount(username, password, AccountRole.ADMIN, username, null, email);
        logger.info("Created the initial administrator account '{}'.", username);
    }
}
