package com.vuhuutuan.Project.service;

import com.vuhuutuan.Project.model.AppUser;
import com.vuhuutuan.Project.model.PasswordResetToken;
import com.vuhuutuan.Project.repository.AppUserRepository;
import com.vuhuutuan.Project.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.NoSuchElementException;

@Service
public class PasswordResetService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_EXPIRY_MINUTES = 30;

    private final AppUserRepository appUserRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String baseUrl;
    private final String fromAddress;

    public PasswordResetService(
            AppUserRepository appUserRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            @Value("${app.password-reset.base-url:http://localhost:8081}") String baseUrl,
            @Value("${app.password-reset.from:}") String fromAddress
    ) {
        this.appUserRepository = appUserRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.fromAddress = fromAddress;
    }

    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        var matchingAccounts = appUserRepository.findAllByEmailIgnoreCase(email.trim()).stream()
                .filter(account -> account.isEnabled() && !account.isDeleted())
                .toList();
        if (matchingAccounts.size() == 1) {
            sendResetLink(matchingAccounts.getFirst(), false);
        } else if (matchingAccounts.size() > 1) {
            logger.warn("Skipped password reset because the email matches {} active accounts.",
                    matchingAccounts.size());
        }
    }

    @Transactional
    public void sendResetLinkToAccount(Long accountId) {
        AppUser account = appUserRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản."));
        if (!account.isEnabled() || account.isDeleted()) {
            throw new IllegalArgumentException("Không thể gửi liên kết cho tài khoản đang bị khóa.");
        }
        if (account.getEmail() == null || account.getEmail().isBlank()) {
            throw new IllegalArgumentException("Tài khoản chưa có địa chỉ email.");
        }
        if (!sendResetLink(account, true)) {
            throw new IllegalStateException("Không gửi được email đặt lại mật khẩu. Hãy kiểm tra cấu hình email.");
        }
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        validatePassword(newPassword);
        PasswordResetToken resetToken = tokenRepository.findByTokenHashForUpdate(hashToken(rawToken))
                .orElseThrow(() -> invalidToken());
        if (!resetToken.getExpiresAt().isAfter(LocalDateTime.now())
                || !resetToken.getAccount().isEnabled()
                || resetToken.getAccount().isDeleted()) {
            throw invalidToken();
        }

        AppUser account = resetToken.getAccount();
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        appUserRepository.save(account);
        tokenRepository.delete(resetToken);
    }

    private boolean sendResetLink(AppUser account, boolean reportFailure) {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String tokenHash = hashToken(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);

        PasswordResetToken resetToken = tokenRepository.findById(account.getId())
                .orElseGet(() -> new PasswordResetToken(account, tokenHash, expiresAt));
        resetToken.update(tokenHash, expiresAt);
        tokenRepository.save(resetToken);

        String link = baseUrl + "/dat-lai-mat-khau?token=" + rawToken;
        SimpleMailMessage message = new SimpleMailMessage();
        if (!fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setTo(account.getEmail());
        message.setSubject("Đặt lại mật khẩu");
        message.setText("""
                Xin chào %s,

                Hãy mở liên kết dưới đây để đặt lại mật khẩu. Liên kết chỉ dùng được một lần và hết hạn sau %d phút:
                %s

                Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.
                """.formatted(account.getFullName(), TOKEN_EXPIRY_MINUTES, link));
        try {
            mailSender.send(message);
            return true;
        } catch (MailException | IllegalStateException exception) {
            tokenRepository.delete(resetToken);
            logger.error("Could not send password reset email for account id {}", account.getId(), exception);
            return !reportFailure;
        }
    }

    private String hashToken(String rawToken) {
        if (rawToken == null || rawToken.length() != 43) {
            throw invalidToken();
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 64
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự và không quá 72 byte UTF-8.");
        }
    }

    private IllegalArgumentException invalidToken() {
        return new IllegalArgumentException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
    }
}
