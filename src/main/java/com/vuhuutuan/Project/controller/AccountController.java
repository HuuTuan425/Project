package com.vuhuutuan.Project.controller;

import com.vuhuutuan.Project.model.AccountRole;
import com.vuhuutuan.Project.model.AppUser;
import com.vuhuutuan.Project.model.SessionUser;
import com.vuhuutuan.Project.config.SessionAuthInterceptor;
import com.vuhuutuan.Project.service.AccountService;
import com.vuhuutuan.Project.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class AccountController {
    private static final int ACCOUNT_PAGE_SIZE = 5;

    private final AccountService accountService;
    private final PasswordResetService passwordResetService;

    public AccountController(AccountService accountService, PasswordResetService passwordResetService) {
        this.accountService = accountService;
        this.passwordResetService = passwordResetService;
    }

    private String currentUsername(HttpSession session) {
        SessionUser user = (SessionUser) session.getAttribute(SessionAuthInterceptor.SESSION_USER_ATTRIBUTE);
        return user == null ? "" : user.username();
    }

    @GetMapping("/tai-khoan")
    public String accounts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) AccountRole role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "false") boolean deleted,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        addAccountList(model, q, role, enabled, deleted, page);
        model.addAttribute("newAccount", new CreateAccountForm());
        model.addAttribute("roles", availableRoles());
        return "taikhoan/danhsach";
    }

    @GetMapping("/tai-khoan/{id}/sua")
    public String editAccount(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        AppUser account;
        try {
            account = accountService.findById(id);
        } catch (java.util.NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản.");
            return "redirect:/tai-khoan";
        }
        if (account.isDeleted()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể sửa tài khoản đã xóa.");
            return "redirect:/tai-khoan?deleted=true";
        }
        UpdateAccountForm form = new UpdateAccountForm();
        form.setFullName(account.getFullName());
        form.setPhone(account.getPhone());
        form.setEmail(account.getEmail());
        form.setRole(account.getRole() == AccountRole.KHAC ? null : account.getRole());
        model.addAttribute("account", account);
        model.addAttribute("profileForm", form);
        model.addAttribute("roles", availableRoles());
        return "taikhoan/sua";
    }

    @PostMapping("/tai-khoan/{id}/sua")
    public String updateAccount(
            @PathVariable Long id,
            @Valid @ModelAttribute("profileForm") UpdateAccountForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (form.getRole() == AccountRole.KHAC) {
            bindingResult.rejectValue("role", "unsupported", "Loại tài khoản này không còn được hỗ trợ.");
        }
        if (bindingResult.hasErrors()) {
            AppUser account;
            try {
                account = accountService.findById(id);
            } catch (java.util.NoSuchElementException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản.");
                return "redirect:/tai-khoan";
            }
            model.addAttribute("account", account);
            model.addAttribute("roles", availableRoles());
            return "taikhoan/sua";
        }
        try {
            accountService.updateProfile(id, form.getFullName(), form.getPhone(), form.getEmail(),
                    form.getRole(), currentUsername(session));
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật hồ sơ tài khoản.");
        } catch (IllegalArgumentException | java.util.NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/tai-khoan";
    }

    @PostMapping("/tai-khoan")
    public String create(
            @Valid @ModelAttribute("newAccount") CreateAccountForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (form.getUsername() != null) {
            form.setUsername(form.getUsername().trim());
            if (!form.getUsername().isEmpty() && accountService.usernameExists(form.getUsername())) {
                bindingResult.rejectValue("username", "duplicate", "Tên đăng nhập đã tồn tại.");
            }
        }
        if (form.getPassword() != null
                && form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            bindingResult.rejectValue("password", "tooLong", "Mật khẩu không được dài quá 72 byte UTF-8.");
        }
        if (form.getRole() == AccountRole.KHAC) {
            bindingResult.rejectValue("role", "unsupported", "Loại tài khoản này không còn được hỗ trợ.");
        }

        if (bindingResult.hasErrors()) {
            addAccountList(model, null, null, null, false, 0);
            model.addAttribute("roles", availableRoles());
            return "taikhoan/danhsach";
        }

        accountService.createAccount(
                form.getUsername(), form.getPassword(), form.getRole(),
                form.getFullName(), form.getPhone(), form.getEmail()
        );
        redirectAttributes.addFlashAttribute("successMessage", "Đã tạo tài khoản thành công.");
        return "redirect:/tai-khoan";
    }

    @PostMapping("/tai-khoan/{id}/trang-thai")
    public String changeStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            accountService.setEnabled(id, enabled, currentUsername(session));
            redirectAttributes.addFlashAttribute("successMessage",
                    enabled ? "Đã mở khóa tài khoản." : "Đã khóa tài khoản.");
        } catch (IllegalArgumentException | java.util.NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/tai-khoan";
    }

    @PostMapping("/tai-khoan/{id}/dat-lai-mat-khau")
    public String sendPasswordReset(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            passwordResetService.sendResetLinkToAccount(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã gửi liên kết đặt lại mật khẩu đến email của tài khoản.");
        } catch (IllegalArgumentException | java.util.NoSuchElementException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/tai-khoan";
    }

    @PostMapping("/tai-khoan/{id}/xoa")
    public String deleteAccount(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            accountService.deleteAccount(id, currentUsername(session));
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã chuyển tài khoản sang trạng thái đã xóa; lịch sử vẫn được giữ.");
        } catch (IllegalArgumentException | java.util.NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/tai-khoan";
    }

    private List<AccountRole> availableRoles() {
        return List.of(AccountRole.ADMIN, AccountRole.CASHIER, AccountRole.WAREHOUSE,
                AccountRole.PURCHASING, AccountRole.MANAGER);
    }

    private void addAccountList(
            Model model,
            String q,
            AccountRole role,
            Boolean enabled,
            boolean deleted,
            int page
    ) {
        String search = q == null ? "" : q.trim();
        String searchPattern = search.isEmpty()
                ? null : "%" + search.toLowerCase(java.util.Locale.ROOT) + "%";
        int requestedPage = Math.max(0, page);
        Page<AppUser> accountPage = accountService.searchAccounts(
                searchPattern,
                role,
                enabled,
                deleted,
                PageRequest.of(requestedPage, ACCOUNT_PAGE_SIZE,
                        Sort.by(Sort.Direction.ASC, "username"))
        );
        int totalPages = Math.max(1, accountPage.getTotalPages());
        int currentPage = Math.min(requestedPage, totalPages - 1);
        if (currentPage != requestedPage) {
            accountPage = accountService.searchAccounts(
                    searchPattern,
                    role,
                    enabled,
                    deleted,
                    PageRequest.of(currentPage, ACCOUNT_PAGE_SIZE,
                            Sort.by(Sort.Direction.ASC, "username"))
            );
        }

        long totalAccounts = accountPage.getTotalElements();
        model.addAttribute("accounts", accountPage.getContent());
        model.addAttribute("q", search);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedEnabled", enabled);
        model.addAttribute("deleted", deleted);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalAccounts", totalAccounts);
        model.addAttribute("pageStart", totalAccounts == 0 ? 0 : currentPage * ACCOUNT_PAGE_SIZE + 1);
        model.addAttribute("pageEnd",
                (int) Math.min((long) (currentPage + 1) * ACCOUNT_PAGE_SIZE, totalAccounts));
    }
}
