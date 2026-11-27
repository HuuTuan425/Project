package com.vuhuutuan.Project.config;

import com.vuhuutuan.Project.model.AccountRole;
import com.vuhuutuan.Project.model.AppUser;
import com.vuhuutuan.Project.model.SessionUser;
import com.vuhuutuan.Project.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionAuthInterceptor implements HandlerInterceptor {
    public static final String SESSION_USER_ATTRIBUTE = "SESSION_USER";

    private final AppUserRepository appUserRepository;

    public SessionAuthInterceptor(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        String path = request.getServletPath();
        if (isPublicPath(path)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        SessionUser sessionUser = session == null
                ? null
                : (SessionUser) session.getAttribute(SESSION_USER_ATTRIBUTE);
        if (sessionUser == null) {
            response.sendRedirect(request.getContextPath() + "/dang-nhap");
            return false;
        }

        AppUser account = appUserRepository.findById(sessionUser.id()).orElse(null);
        if (account == null || account.isDeleted() || !account.isEnabled()
                || account.getRole() != sessionUser.role()
                || !account.getPasswordHash().equals(sessionUser.passwordHash())) {
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/dang-nhap?locked");
            return false;
        }

        if (!isAllowed(path, request.getMethod(), account.getRole())) {
            response.sendRedirect(request.getContextPath() + "/khong-co-quyen");
            return false;
        }
        return true;
    }

    private boolean isPublicPath(String path) {
        return path.equals("/")
                || path.equals("/dang-nhap")
                || path.equals("/dang-xuat")
                || path.equals("/khong-co-quyen")
                || path.equals("/quen-mat-khau")
                || path.equals("/dat-lai-mat-khau")
                || path.startsWith("/css/")
                || path.equals("/error");
    }

    private boolean isAllowed(String path, String method, AccountRole role) {
        if (role == AccountRole.ADMIN) {
            return true;
        }
        if (path.startsWith("/tai-khoan")) {
            return false;
        }
        if (path.equals("/hoa-don/bao-cao") || path.startsWith("/hoa-don/bao-cao")) {
            return role == AccountRole.MANAGER;
        }
        if (path.equals("/khach-hang/them")) {
            return (role == AccountRole.ADMIN || role == AccountRole.CASHIER)
                    && ("GET".equals(method) || "POST".equals(method));
        }
        if (path.startsWith("/khach-hang")) {
            return false;
        }
        if (path.equals("/mat-hang/tim-kiem")) {
            return "GET".equals(method)
                    && (role == AccountRole.CASHIER || role == AccountRole.WAREHOUSE
                    || role == AccountRole.PURCHASING);
        }
        if (path.equals("/mat-hang")) {
            return "GET".equals(method)
                    && (role == AccountRole.WAREHOUSE || role == AccountRole.PURCHASING);
        }
        if (path.startsWith("/mat-hang")) {
            return role == AccountRole.WAREHOUSE;
        }
        if (path.equals("/nhap-hang/nha-cung-cap")) {
            return "GET".equals(method)
                    ? role == AccountRole.PURCHASING || role == AccountRole.WAREHOUSE
                    || role == AccountRole.MANAGER
                    : role == AccountRole.PURCHASING;
        }
        if (path.matches("^/nhap-hang/nha-cung-cap/\\d+/sua$")) {
            return "GET".equals(method) && role == AccountRole.PURCHASING;
        }
        if (path.equals("/nhap-hang/nha-cung-cap/de-xuat")) {
            return "POST".equals(method)
                    && (role == AccountRole.WAREHOUSE || role == AccountRole.PURCHASING);
        }
        if (path.matches("^/nhap-hang/nha-cung-cap/de-xuat/\\d+/(duyet|tu-choi)$")) {
            return "POST".equals(method) && role == AccountRole.PURCHASING;
        }
        if (path.startsWith("/nhap-hang/nha-cung-cap/")) {
            return "POST".equals(method) && role == AccountRole.PURCHASING;
        }
        if (path.equals("/nhap-hang") || path.equals("/nhap-hang/tao")) {
            return "GET".equals(method)
                    ? role == AccountRole.PURCHASING || role == AccountRole.WAREHOUSE
                    || role == AccountRole.MANAGER
                    : role == AccountRole.PURCHASING;
        }
        if (path.matches("^/nhap-hang/\\d+/nhan$")) {
            return "POST".equals(method) && role == AccountRole.WAREHOUSE;
        }
        if (path.startsWith("/nhap-hang/")) {
            return role == AccountRole.PURCHASING
                    || role == AccountRole.WAREHOUSE
                    || role == AccountRole.MANAGER;
        }
        if (path.equals("/hoa-don/bao-cao")) {
            return "GET".equals(method) && role == AccountRole.MANAGER;
        }
        if (path.equals("/hoa-don")) {
            return "GET".equals(method) && role == AccountRole.MANAGER;
        }
        if (path.equals("/hoa-don/lap")) {
            return role == AccountRole.CASHIER
                    && ("GET".equals(method) || "POST".equals(method));
        }
        if (path.matches("^/hoa-don/\\d+/huy$")) {
            return "POST".equals(method) && role == AccountRole.MANAGER;
        }
        if (path.matches("^/hoa-don/\\d+(/pdf)?$")) {
            return "GET".equals(method)
                    && (role == AccountRole.CASHIER || role == AccountRole.MANAGER);
        }
        return false;
    }
}
