package com.vuhuutuan.Project.controller;

import com.vuhuutuan.Project.model.LoaiMatHang;
import com.vuhuutuan.Project.model.MatHang;
import com.vuhuutuan.Project.service.MatHangService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Controller
@RequestMapping("/mat-hang")
public class MatHangController {
    private final MatHangService matHangService;

    public MatHangController(MatHangService matHangService) {
        this.matHangService = matHangService;
    }

    @GetMapping
    public String danhSach(@RequestParam(required = false) String q, Model model) {
        String search = q == null ? "" : q.trim();
        Long productId = null;
        if (!search.isEmpty()) {
            try {
                productId = Long.valueOf(search);
            } catch (NumberFormatException ignored) {
                // Non-numeric searches are matched against product and manufacturer names.
            }
        }
        String query = search.isEmpty() ? null : "%" + search.toLowerCase(Locale.ROOT) + "%";
        List<MatHang> products = matHangService.searchProducts(productId, query);
        model.addAttribute("dsMatHang", products);
        model.addAttribute("q", search);
        model.addAttribute("totalMatHang", products.size());
        return "mathang/danhsach";
    }

    @GetMapping("/tim-kiem")
    @ResponseBody
    public List<ProductSearchResult> timKiem(@RequestParam(defaultValue = "") String q) {
        return matHangService.searchAvailable(q).stream()
                .map(product -> new ProductSearchResult(
                        product.getMaMatHang(),
                        product.getTenMatHang(),
                        product.getGiaApDung(java.time.LocalDateTime.now()),
                        product.getQuantityInStock()
                ))
                .toList();
    }

    @GetMapping("/them")
    public String formThem(Model model) {
        model.addAttribute("matHang", new MatHang());
        addFormOptions(model);
        return "mathang/form";
    }

    @PostMapping("/them")
    public String them(
            @Valid @ModelAttribute("matHang") MatHang matHang,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        validatePromotion(matHang, bindingResult);
        if (bindingResult.hasErrors()) {
            addFormOptions(model);
            return "mathang/form";
        }

        matHang.setQuantityInStock(0);
        matHangService.save(matHang);
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm mặt hàng.");
        return "redirect:/mat-hang";
    }

    @GetMapping("/sua/{id}")
    public String formSua(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("matHang", matHangService.findById(id));
        } catch (NoSuchElementException exception) {
            return "redirect:/mat-hang";
        }
        addFormOptions(model);
        return "mathang/form";
    }

    @PostMapping("/sua/{id}")
    public String sua(
            @PathVariable Long id,
            @Valid @ModelAttribute("matHang") MatHang matHang,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (matHang.getMaMatHang() != null && !id.equals(matHang.getMaMatHang())) {
            bindingResult.reject("matHang", "Mã mặt hàng không hợp lệ.");
        }
        MatHang persisted;
        try {
            persisted = matHangService.findById(id);
        } catch (NoSuchElementException exception) {
            return "redirect:/mat-hang";
        }
        matHang.setMaMatHang(id);
        matHang.setQuantityInStock(persisted.getQuantityInStock());
        validatePromotion(matHang, bindingResult);
        if (bindingResult.hasErrors()) {
            addFormOptions(model);
            return "mathang/form";
        }

        matHangService.save(matHang);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật mặt hàng.");
        return "redirect:/mat-hang";
    }

    @PostMapping("/xoa/{id}")
    public String xoa(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            matHangService.deleteById(id);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Đã ẩn mặt hàng khỏi danh sách; lịch sử giao dịch được giữ nguyên.");
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        } catch (NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/mat-hang";
    }

    private void addFormOptions(Model model) {
        model.addAttribute("loaiMatHangs", Arrays.asList(LoaiMatHang.values()));
    }

    private void validatePromotion(MatHang matHang, BindingResult bindingResult) {
        if (matHang.getGiaKhuyenMai() != null && matHang.getGiaBanGoc() != null
                && (!Double.isFinite(matHang.getGiaKhuyenMai()) || matHang.getGiaKhuyenMai() <= 0
                || matHang.getGiaKhuyenMai() > matHang.getGiaBanGoc())) {
            bindingResult.rejectValue("giaKhuyenMai", "invalid", "Giá khuyến mãi phải lớn hơn 0 và không vượt giá gốc.");
        }
        if (matHang.getGiaBanGoc() != null && !Double.isFinite(matHang.getGiaBanGoc())) {
            bindingResult.rejectValue("giaBanGoc", "invalid", "Giá bán không hợp lệ.");
        }
        if (matHang.getThoiGianBatDauKM() != null && matHang.getThoiGianKetThucKM() != null
                && matHang.getThoiGianBatDauKM().isAfter(matHang.getThoiGianKetThucKM())) {
            bindingResult.rejectValue("thoiGianKetThucKM", "invalid", "Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
    }

    public record ProductSearchResult(Long id, String name, Double price, Integer quantityInStock) {
    }
}
