package com.vuhuutuan.Project.service;

import com.vuhuutuan.Project.model.ChiTietHoaDon;
import com.vuhuutuan.Project.model.HoaDon;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ReceiptPdfService {
    private static final float PAGE_WIDTH = 226.77f;
    private static final float MARGIN = 18f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");

    public byte[] create(HoaDon invoice) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ReceiptFonts fonts = loadFonts(document);
            ReceiptCanvas canvas = new ReceiptCanvas(document, fonts);

            canvas.center("HÓA ĐƠN BÁN HÀNG", 14, true, 5);
            canvas.center("Cảm ơn quý khách đã mua hàng", 8, false, 12);
            canvas.line("Mã hóa đơn: " + invoice.getMaHoaDon(), 9, true, 3);
            canvas.line("Ngày lập: " + (invoice.getThoiGianThanhToan() == null
                    ? "-" : invoice.getThoiGianThanhToan().format(DATE_FORMAT)), 8, false, 3);
            canvas.line("Khách hàng: " + (invoice.getKhachHang() == null
                    ? "Khách lẻ" : invoice.getKhachHang().getHoTen()), 8, false, 3);
            canvas.line("Thu ngân: " + (invoice.getThuNgan() == null
                    ? "-" : invoice.getThuNgan().getUsername()), 8, false, 3);
            canvas.line("Thanh toán: " + (invoice.getPhuongThucThanhToan() == null
                    ? "Tiền mặt" : invoice.getPhuongThucThanhToan().getDisplayName()), 8, false, 8);

            if (invoice.isCancelled()) {
                canvas.divider(6);
                canvas.line("HÓA ĐƠN ĐÃ HỦY", 10, true, 3);
                canvas.line("Lý do hủy: " + (invoice.getCancellationReason() == null
                        ? "-" : invoice.getCancellationReason()), 8, false, 6);
            }

            canvas.divider(8);
            for (ChiTietHoaDon item : invoice.getChiTiet()) {
                canvas.line(item.getMatHang().getTenMatHang(), 9, true, 2);
                canvas.line(item.getSoLuong() + " x " + money(item.getGiaTaiThoiDiem()),
                        8, false, 1);
                canvas.right(money(item.getThanhTien()), 8, true, 7);
            }

            canvas.divider(7);
            canvas.line("Điểm đã dùng: " + number(invoice.getDiemThuongDung()), 8, false, 3);
            canvas.line("Tiền khách đưa: " + money(invoice.getTienKhachTra()), 8, false, 3);
            canvas.line("Tiền thừa: " + money(invoice.getTienThua()), 8, false, 3);
            canvas.line("Điểm được cộng: " + number(invoice.getDiemThuongCong()), 8, false, 8);
            canvas.total("TỔNG THANH TOÁN", money(invoice.getTongTien()));
            canvas.center("Hẹn gặp lại quý khách!", 9, true, 0);

            canvas.finish();
            document.save(output);
            return output.toByteArray();
        }
    }

    private ReceiptFonts loadFonts(PDDocument document) throws IOException {
        List<FontPair> candidates = List.of(
                new FontPair(Path.of("C:\\Windows\\Fonts\\arial.ttf"),
                        Path.of("C:\\Windows\\Fonts\\arialbd.ttf")),
                new FontPair(Path.of("C:\\Windows\\Fonts\\segoeui.ttf"),
                        Path.of("C:\\Windows\\Fonts\\segoeuib.ttf")),
                new FontPair(Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
                        Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf")),
                new FontPair(Path.of("/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"),
                        Path.of("/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf")),
                new FontPair(Path.of("/Library/Fonts/Arial Unicode.ttf"),
                        Path.of("/Library/Fonts/Arial Unicode Bold.ttf"))
        );
        for (FontPair candidate : candidates) {
            if (Files.isRegularFile(candidate.regular()) && Files.isRegularFile(candidate.bold())) {
                return new ReceiptFonts(
                        PDType0Font.load(document, candidate.regular().toFile()),
                        PDType0Font.load(document, candidate.bold().toFile())
                );
            }
        }
        throw new IOException(
                "Không tìm thấy font Unicode để xuất hóa đơn tiếng Việt. Cài Arial hoặc Noto Sans trên máy chạy ứng dụng."
        );
    }

    private String money(Double value) {
        NumberFormat format = NumberFormat.getNumberInstance(VIETNAM);
        format.setMaximumFractionDigits(0);
        return format.format(value == null ? 0 : value) + " đ";
    }

    private String number(Integer value) {
        return NumberFormat.getIntegerInstance(VIETNAM).format(value == null ? 0 : value);
    }

    private record FontPair(Path regular, Path bold) {
    }

    private record ReceiptFonts(PDFont regular, PDFont bold) {
    }

    private static final class ReceiptCanvas {
        private final PDDocument document;
        private final ReceiptFonts fonts;
        private final List<ReceiptRow> rows = new ArrayList<>();

        private ReceiptCanvas(PDDocument document, ReceiptFonts fonts) {
            this.document = document;
            this.fonts = fonts;
        }

        private void line(String text, int size, boolean bold, float gapAfter) {
            rows.add(new ReceiptRow(text, size, bold, Alignment.LEFT, gapAfter, false, false));
        }

        private void center(String text, int size, boolean bold, float gapAfter) {
            rows.add(new ReceiptRow(text, size, bold, Alignment.CENTER, gapAfter, false, false));
        }

        private void right(String text, int size, boolean bold, float gapAfter) {
            rows.add(new ReceiptRow(text, size, bold, Alignment.RIGHT, gapAfter, false, false));
        }

        private void divider(float gapAfter) {
            rows.add(new ReceiptRow("", 1, false, Alignment.LEFT, gapAfter, true, false));
        }

        private void total(String label, String value) {
            rows.add(new ReceiptRow(label, 10, true, Alignment.LEFT, 2, false, true));
            rows.add(new ReceiptRow(value, 13, true, Alignment.RIGHT, 10, false, true));
        }

        private void finish() throws IOException {
            List<List<String>> wrappedRows = new ArrayList<>();
            float contentHeight = MARGIN * 2;
            for (ReceiptRow row : rows) {
                List<String> wrapped = row.divider()
                        ? List.of() : wrap(row.text(), row);
                if (!row.divider()) {
                    contentHeight += wrapped.size() * (row.size() + 3);
                } else {
                    contentHeight += 1;
                }
                contentHeight += row.gapAfter();
                wrappedRows.add(wrapped);
            }

            float pageHeight = Math.max(250, contentHeight);
            PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, pageHeight));
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = pageHeight - MARGIN;
                for (int i = 0; i < rows.size(); i++) {
                    ReceiptRow row = rows.get(i);
                    List<String> wrapped = wrappedRows.get(i);
                    if (row.highlighted()) {
                        content.setNonStrokingColor(new Color(239, 246, 255));
                        content.addRect(MARGIN - 6, y - row.size() - 3,
                                CONTENT_WIDTH + 12, row.size() + 9);
                        content.fill();
                        content.setNonStrokingColor(Color.BLACK);
                    }
                    if (row.divider()) {
                        content.setStrokingColor(new Color(190, 198, 208));
                        content.setLineDashPattern(new float[]{2, 2}, 0);
                        content.moveTo(MARGIN, y - 2);
                        content.lineTo(PAGE_WIDTH - MARGIN, y - 2);
                        content.stroke();
                        content.setLineDashPattern(new float[0], 0);
                        y -= 5;
                    } else {
                        for (String text : wrapped) {
                            drawText(content, text, row, y);
                            y -= row.size() + 3;
                        }
                    }
                    y -= row.gapAfter();
                }
            }
        }

        private List<String> wrap(String text, ReceiptRow row) throws IOException {
            PDFont font = font(row);
            List<String> result = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : text.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (font.getStringWidth(candidate) * row.size() / 1000 <= CONTENT_WIDTH) {
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }
                if (!line.isEmpty()) {
                    result.add(line.toString());
                    line.setLength(0);
                }
                line.append(word);
            }
            if (!line.isEmpty()) {
                result.add(line.toString());
            }
            return result;
        }

        private void drawText(PDPageContentStream content, String text, ReceiptRow row, float y)
                throws IOException {
            PDFont font = font(row);
            float textWidth = font.getStringWidth(text) * row.size() / 1000;
            float x = switch (row.alignment()) {
                case LEFT -> MARGIN;
                case CENTER -> (PAGE_WIDTH - textWidth) / 2;
                case RIGHT -> PAGE_WIDTH - MARGIN - textWidth;
            };
            content.beginText();
            content.setFont(font, row.size());
            content.newLineAtOffset(x, y);
            content.showText(text);
            content.endText();
        }

        private PDFont font(ReceiptRow row) {
            return row.bold() ? fonts.bold() : fonts.regular();
        }

        private enum Alignment {
            LEFT,
            CENTER,
            RIGHT
        }

        private record ReceiptRow(
                String text,
                int size,
                boolean bold,
                Alignment alignment,
                float gapAfter,
                boolean divider,
                boolean highlighted
        ) {
        }
    }
}
