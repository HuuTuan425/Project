package com.vuhuutuan.Project.service;

import com.vuhuutuan.Project.model.AccountRole;
import com.vuhuutuan.Project.model.AppUser;
import com.vuhuutuan.Project.repository.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AccountService {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean usernameExists(String username) {
        return appUserRepository.existsByUsernameIgnoreCase(username.trim());
    }

    public List<AppUser> findAll() {
        return appUserRepository.findAllByDeletedAtIsNullOrderByUsernameAsc();
    }

    public Page<AppUser> searchAccounts(
            String search,
            AccountRole role,
            Boolean enabled,
            boolean deleted,
            Pageable pageable
    ) {
        return appUserRepository.searchAccounts(search, role, enabled, deleted, pageable);
    }

    public AppUser findById(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản."));
    }

    @Transactional
    public void createAccount(
            String username,
            String password,
            AccountRole role,
            String fullName,
            String phone,
            String email
    ) {
        String normalizedUsername = username.trim();
        if (appUserRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu không được dài quá 72 byte UTF-8.");
        }
        String normalizedEmail = normalizeRequiredEmail(email);

        AppUser account = new AppUser(
                normalizedUsername,
                passwordEncoder.encode(password),
                role
        );
        account.setFullName(fullName.trim());
        account.setPhone(normalizeOptional(phone));
        account.setEmail(normalizedEmail);
        appUserRepository.save(account);
    }

    @Transactional
    public void setEnabled(Long id, boolean enabled, String actingUsername) {
        AppUser account = appUserRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản."));
        if (account.isDeleted()) {
            throw new IllegalArgumentException("Không thể thay đổi trạng thái tài khoản đã xóa.");
        }
        if (!enabled && account.getUsername().equalsIgnoreCase(actingUsername)) {
            throw new IllegalArgumentException("Bạn không thể tự khóa tài khoản đang đăng nhập.");
        }
        if (!enabled && account.getRole() == AccountRole.ADMIN
                && account.isEnabled()
                && appUserRepository.countByRoleAndEnabledTrue(AccountRole.ADMIN) <= 1) {
            throw new IllegalArgumentException("Không thể khóa tài khoản quản trị viên đang hoạt động cuối cùng.");
        }
        account.setEnabled(enabled);
        appUserRepository.save(account);
    }

    @Transactional
    public void deleteAccount(Long id, String actingUsername) {
        AppUser account = appUserRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản."));
        if (account.getUsername().equalsIgnoreCase(actingUsername)) {
            throw new IllegalArgumentException("Bạn không thể xóa tài khoản đang đăng nhập.");
        }
        if (account.isDeleted()) {
            throw new IllegalArgumentException("Tài khoản đã được xóa trước đó.");
        }
        if (account.isEnabled() && account.getRole() == AccountRole.ADMIN
                && appUserRepository.countByRoleAndEnabledTrue(AccountRole.ADMIN) <= 1) {
            throw new IllegalArgumentException("Không thể xóa tài khoản quản trị viên đang hoạt động cuối cùng.");
        }
        account.setEnabled(false);
        account.setDeletedAt(LocalDateTime.now());
        appUserRepository.save(account);
    }

    @Transactional
    public void updateProfile(
            Long id,
            String fullName,
            String phone,
            String email,
            AccountRole role,
            String actingUsername
    ) {
        AppUser account = appUserRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản."));
        if (account.isDeleted()) {
            throw new IllegalArgumentException("Không thể sửa tài khoản đã xóa.");
        }
        if (role == null || role == AccountRole.KHAC) {
            throw new IllegalArgumentException("Loại tài khoản không hợp lệ.");
        }
        String normalizedEmail = normalizeRequiredEmail(email);
        if (account.getUsername().equalsIgnoreCase(actingUsername) && account.getRole() != role) {
            throw new IllegalArgumentException("Bạn không thể tự thay đổi quyền của tài khoản đang đăng nhập.");
        }
        if (account.isEnabled() && account.getRole() == AccountRole.ADMIN && role != AccountRole.ADMIN
                && appUserRepository.countByRoleAndEnabledTrue(AccountRole.ADMIN) <= 1) {
            throw new IllegalArgumentException("Không thể hạ quyền quản trị viên đang hoạt động cuối cùng.");
        }
        account.setFullName(fullName.trim());
        account.setPhone(normalizeOptional(phone));
        account.setEmail(normalizedEmail);
        account.setRole(role);
        appUserRepository.save(account);
    }

    @Transactional
    public String recordLogin(String username) {
        AppUser account = appUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản đăng nhập."));
        account.setLastLoginAt(LocalDateTime.now());
        appUserRepository.save(account);
        return account.getPasswordHash();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeRequiredEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập email để có thể đặt lại mật khẩu.");
        }
        String normalizedEmail = email.trim();
        if (normalizedEmail.length() > 150) {
            throw new IllegalArgumentException("Email không được quá 150 ký tự.");
        }
        return normalizedEmail;
    }
}
