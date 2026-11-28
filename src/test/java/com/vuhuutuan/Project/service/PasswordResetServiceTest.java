package com.vuhuutuan.Project.service;

import com.vuhuutuan.Project.model.AccountRole;
import com.vuhuutuan.Project.model.AppUser;
import com.vuhuutuan.Project.model.PasswordResetToken;
import com.vuhuutuan.Project.repository.AppUserRepository;
import com.vuhuutuan.Project.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {
    private final AppUserRepository accounts = mock(AppUserRepository.class);
    private final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JavaMailSender mail = mock(JavaMailSender.class);
    private final PasswordResetService service =
            new PasswordResetService(accounts, tokens, encoder, mail, "http://localhost:8081", "");
    private final String rawToken = "1" + "a".repeat(43);

    @Test
    void resetRequestCreatesTenMinuteLinkAndEmailsItsDuration() {
        AppUser account = new AppUser("user", "hash", AccountRole.CASHIER);
        account.setEmail("user@example.com");
        when(accounts.findAllByEmailIgnoreCase(account.getEmail())).thenReturn(List.of(account));
        LocalDateTime before = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        service.requestReset(account.getEmail());

        LocalDateTime after = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).save(saved.capture());
        LocalDateTime expiresAt = saved.getValue().getExpiresAt();
        assertFalse(expiresAt.isBefore(before.plusMinutes(10)));
        assertFalse(expiresAt.isAfter(after.plusMinutes(10)));
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(message.capture());
        assertTrue(message.getValue().getText().contains("hết hạn sau 10 phút"));
    }

    @Test
    void validLinkCanBeOpenedButExpiredOrMissingLinkCannot() {
        AppUser account = new AppUser("user", "hash", AccountRole.CASHIER);
        PasswordResetToken token = new PasswordResetToken(account, "hash", LocalDateTime.now().plusMinutes(1));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        assertDoesNotThrow(() -> service.validateResetLink(rawToken));

        token.update("hash", LocalDateTime.now().minusNanos(1));
        assertThrows(IllegalArgumentException.class, () -> service.validateResetLink(rawToken));
        token.update("hash", LocalDateTime.now().plusMinutes(1));
        account.setEnabled(false);
        assertThrows(IllegalArgumentException.class, () -> service.validateResetLink(rawToken));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.validateResetLink(rawToken));
        assertThrows(IllegalArgumentException.class, () -> service.validateResetLink(null));
        assertThrows(IllegalArgumentException.class, () -> service.validateResetLink("a".repeat(43)));
    }

    @Test
    void expiredLinkCannotChangePasswordAfterFormWasOpened() {
        AppUser account = new AppUser("user", "old-hash", AccountRole.CASHIER);
        PasswordResetToken token = new PasswordResetToken(account, "hash", LocalDateTime.now().minusSeconds(1));
        when(tokens.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(token));

        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(rawToken, "new-password"));
        verify(accounts, never()).save(account);
        verify(tokens, never()).delete(token);
    }
}
