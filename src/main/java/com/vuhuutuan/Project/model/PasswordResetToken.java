package com.vuhuutuan.Project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {
    @Id
    @Column(name = "account_id")
    private Long accountId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "account_id", nullable = false)
    private AppUser account;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(AppUser account, String tokenHash, LocalDateTime expiresAt) {
        this.account = account;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public void update(String tokenHash, LocalDateTime expiresAt) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public AppUser getAccount() {
        return account;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
