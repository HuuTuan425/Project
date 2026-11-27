package com.vuhuutuan.Project.controller;

import com.vuhuutuan.Project.model.HoaDon;
import com.vuhuutuan.Project.service.HoaDonService;
import com.vuhuutuan.Project.service.ReceiptPdfService;
import com.vuhuutuan.Project.model.SessionUser;
import com.vuhuutuan.Project.config.SessionAuthInterceptor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class ReceiptController {
    private final HoaDonService hoaDonService;
    private final ReceiptPdfService receiptPdfService;

    public ReceiptController(HoaDonService hoaDonService, ReceiptPdfService receiptPdfService) {
        this.hoaDonService = hoaDonService;
        this.receiptPdfService = receiptPdfService;
    }

    @GetMapping("/hoa-don/{id}/pdf")
    public ResponseEntity<byte[]> pdf(
            @PathVariable Long id,
            HttpSession session
    ) throws IOException {
        HoaDon invoice = hoaDonService.findById(id);
        SessionUser user = (SessionUser) session.getAttribute(SessionAuthInterceptor.SESSION_USER_ATTRIBUTE);
        boolean isAdmin = user != null && user.isAdmin();
        boolean isManager = user != null && user.isManager();
        if (!isAdmin && !isManager && (invoice.getThuNgan() == null
                || !invoice.getThuNgan().getUsername().equalsIgnoreCase(user == null ? "" : user.username()))) {
            return ResponseEntity.status(403).build();
        }

        byte[] receipt = receiptPdfService.create(invoice);
        String filename = "hoa-don-" + invoice.getMaHoaDon() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(receipt);
    }
}
