package com.vuhuutuan.Project.config;

import com.vuhuutuan.Project.model.AppUser;
import com.vuhuutuan.Project.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

final class AccountStatusFilter extends OncePerRequestFilter {
    static final String AUTH_PASSWORD_HASH_ATTRIBUTE = "accountPasswordHash";

    private final AppUserRepository appUserRepository;

    AccountStatusFilter(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            AppUser account = appUserRepository.findByUsernameIgnoreCase(authentication.getName())
                    .orElse(null);
            boolean roleChanged = account != null && authentication.getAuthorities().stream()
                    .noneMatch(authority -> authority.getAuthority().equals("ROLE_" + account.getRole().name()));
            var session = request.getSession(false);
            Object authenticatedPasswordHash = session == null
                    ? null : session.getAttribute(AUTH_PASSWORD_HASH_ATTRIBUTE);
            boolean passwordChanged = account != null && authenticatedPasswordHash instanceof String hash
                    && !account.getPasswordHash().equals(hash);
            if (account == null || !account.isEnabled() || account.isDeleted() || roleChanged || passwordChanged) {
                SecurityContextHolder.clearContext();
                if (session != null) {
                    session.invalidate();
                }
                String reason = account == null ? "locked"
                        : account.isDeleted() ? "deleted" : !account.isEnabled() ? "locked"
                        : roleChanged ? "roleChanged" : "passwordChanged";
                response.sendRedirect(request.getContextPath() + "/dang-nhap?" + reason);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
