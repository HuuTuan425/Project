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
    private final AppUserRepository appUserRepository;
    private final AccountService accountService;

    public AccountBootstrap(
            @Value("${app.bootstrap-admin.username:}") String username,
            @Value("${app.bootstrap-admin.password:}") String password,
            AppUserRepository appUserRepository,
            AccountService accountService
    ) {
        this.username = username.trim();
        this.password = password;
        this.appUserRepository = appUserRepository;
        this.accountService = accountService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() && password.isBlank()) {
            logger.warn("No initial administrator configured. Set BOOTSTRAP_ADMIN_USERNAME and BOOTSTRAP_ADMIN_PASSWORD.");
            return;
        }
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Both bootstrap administrator environment variables must be set.");
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

        accountService.createAccount(username, password, AccountRole.ADMIN);
        logger.info("Created the initial administrator account '{}'.", username);
    }
}
