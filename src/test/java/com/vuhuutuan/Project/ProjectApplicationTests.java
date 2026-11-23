package com.vuhuutuan.Project;

import com.vuhuutuan.Project.model.AccountRole;
import com.vuhuutuan.Project.model.AppUser;
import com.vuhuutuan.Project.model.HoaDon;
import com.vuhuutuan.Project.model.KhachHang;
import com.vuhuutuan.Project.model.LoaiKhachHang;
import com.vuhuutuan.Project.model.LoaiMatHang;
import com.vuhuutuan.Project.model.MatHang;
import com.vuhuutuan.Project.model.PaymentMethod;
import com.vuhuutuan.Project.model.ProductBatch;
import com.vuhuutuan.Project.model.PurchaseOrder;
import com.vuhuutuan.Project.model.PurchaseOrderStatus;
import com.vuhuutuan.Project.model.Supplier;
import com.vuhuutuan.Project.model.SupplierProposal;
import com.vuhuutuan.Project.controller.InvoiceLineForm;
import com.vuhuutuan.Project.controller.PurchaseOrderLineForm;
import com.vuhuutuan.Project.config.InvoicePriceSnapshotMigration;
import com.vuhuutuan.Project.config.RetailSchemaMigration;
import com.vuhuutuan.Project.repository.AppUserRepository;
import com.vuhuutuan.Project.repository.HoaDonRepository;
import com.vuhuutuan.Project.repository.KhachHangRepository;
import com.vuhuutuan.Project.repository.MatHangRepository;
import com.vuhuutuan.Project.repository.ProductBatchRepository;
import com.vuhuutuan.Project.repository.PasswordResetTokenRepository;
import com.vuhuutuan.Project.repository.PurchaseOrderRepository;
import com.vuhuutuan.Project.repository.SupplierRepository;
import com.vuhuutuan.Project.repository.SupplierProposalRepository;
import com.vuhuutuan.Project.service.HoaDonService;
import com.vuhuutuan.Project.service.PurchasingService;
import com.vuhuutuan.Project.service.ReceiptPdfService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@AutoConfigureMockMvc
@SpringBootTest
class ProjectApplicationTests {
	private static final String TEST_PASSWORD = "Test-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private PasswordResetTokenRepository passwordResetTokenRepository;

	@MockitoBean
	private JavaMailSender mailSender;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private HoaDonService hoaDonService;

	@Autowired
	private com.vuhuutuan.Project.service.AccountService accountService;

	@Autowired
	private PurchasingService purchasingService;

	@Autowired
	private ReceiptPdfService receiptPdfService;

	@Autowired
	private MatHangRepository matHangRepository;

	@Autowired
	private KhachHangRepository khachHangRepository;

	@Autowired
	private SupplierRepository supplierRepository;

	@Autowired
	private SupplierProposalRepository supplierProposalRepository;

	@Autowired
	private ProductBatchRepository productBatchRepository;

	@Autowired
	private PurchaseOrderRepository purchaseOrderRepository;

	@Autowired
	private HoaDonRepository hoaDonRepository;

	@Autowired
	private InvoicePriceSnapshotMigration invoicePriceSnapshotMigration;

	@Autowired
	private RetailSchemaMigration retailSchemaMigration;

	@BeforeEach
	void migrateLegacyDatabaseSchema() throws Exception {
		DefaultApplicationArguments args = new DefaultApplicationArguments(new String[0]);
		invoicePriceSnapshotMigration.run(args);
		retailSchemaMigration.run(args);
	}

	@Test
	@Transactional
	void warehouseAndManagerHaveSeparateAccess() throws Exception {
		AppUser warehouse = createAccount(AccountRole.WAREHOUSE);
		MockHttpSession warehouseSession = login(warehouse.getUsername());
		mockMvc.perform(get("/mat-hang").session(warehouseSession))
				.andExpect(status().isOk());
		mockMvc.perform(get("/nhap-hang").session(warehouseSession))
				.andExpect(status().isOk());
		mockMvc.perform(get("/nhap-hang/tao").session(warehouseSession))
				.andExpect(status().is3xxRedirection());
		MvcResult supplierPage = mockMvc.perform(get("/nhap-hang/nha-cung-cap").session(warehouseSession))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Đề xuất nhà cung cấp mới")))
				.andExpect(content().string(org.hamcrest.Matchers.not(containsString("Lưu thay đổi"))))
				.andReturn();
		Matcher csrfMatcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
				.matcher(supplierPage.getResponse().getContentAsString());
		assertTrue(csrfMatcher.find(), "Supplier page must include a CSRF token");
		String csrfToken = csrfMatcher.group(1);
		Supplier officialSupplier = new Supplier();
		officialSupplier.setName("Nhà cung cấp quản trị " + UUID.randomUUID());
		officialSupplier = supplierRepository.save(officialSupplier);
		Long officialSupplierId = officialSupplier.getId();
		mockMvc.perform(get("/nhap-hang/nha-cung-cap/{id}/sua", officialSupplierId)
						.session(warehouseSession))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(post("/nhap-hang/nha-cung-cap")
						.session(warehouseSession)
						.param("id", officialSupplierId.toString())
						.param("name", "Tên bị thay đổi")
						.param("_csrf", csrfToken))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(post("/nhap-hang/nha-cung-cap/{id}/xoa", officialSupplierId)
						.session(warehouseSession)
						.param("_csrf", csrfToken))
				.andExpect(status().is3xxRedirection());
		assertTrue(supplierRepository.existsById(officialSupplierId),
				"Warehouse must not edit or delete official suppliers");
		String proposalName = "Nhà cung cấp đề xuất " + UUID.randomUUID();
		mockMvc.perform(post("/nhap-hang/nha-cung-cap")
						.session(warehouseSession)
						.param("name", proposalName)
						.param("_csrf", csrfToken))
				.andExpect(status().is3xxRedirection());
		assertTrue(supplierRepository.findAll().stream().noneMatch(supplier -> supplier.getName().equals(proposalName)),
				"Warehouse must not directly create official suppliers");
		mockMvc.perform(post("/nhap-hang/nha-cung-cap/de-xuat")
						.session(warehouseSession)
						.param("name", proposalName)
						.param("phone", "0981234567")
						.param("email", "proposal@example.com")
						.param("_csrf", csrfToken))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/nhap-hang/nha-cung-cap"));
		SupplierProposal proposal = supplierProposalRepository.findAll().stream()
				.filter(item -> item.getName().equals(proposalName))
				.findFirst()
				.orElseThrow();
		assertEquals("PENDING", proposal.getStatus().name());

		AppUser admin = createAccount(AccountRole.ADMIN);
		MockHttpSession adminSession = login(admin.getUsername());
		MvcResult adminSupplierPage = mockMvc.perform(get("/nhap-hang/nha-cung-cap").session(adminSession))
				.andExpect(status().isOk())
				.andReturn();
		Matcher adminCsrfMatcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
				.matcher(adminSupplierPage.getResponse().getContentAsString());
		assertTrue(adminCsrfMatcher.find(), "Admin supplier page must include a CSRF token");
		mockMvc.perform(post("/nhap-hang/nha-cung-cap/de-xuat/{id}/duyet", proposal.getId())
						.session(adminSession)
						.param("_csrf", adminCsrfMatcher.group(1)))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/nhap-hang/nha-cung-cap"));
		assertTrue(supplierRepository.findAll().stream().anyMatch(supplier -> supplier.getName().equals(proposalName)),
				"Approved proposals should become official suppliers");
		assertEquals("APPROVED",
				supplierProposalRepository.findById(proposal.getId()).orElseThrow().getStatus().name());
		mockMvc.perform(get("/hoa-don/bao-cao").session(warehouseSession))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/tai-khoan").session(warehouseSession))
				.andExpect(status().is3xxRedirection());

		AppUser manager = createAccount(AccountRole.MANAGER);
		MockHttpSession managerSession = login(manager.getUsername());
		mockMvc.perform(get("/hoa-don/bao-cao").session(managerSession))
				.andExpect(status().isOk());
		mockMvc.perform(get("/hoa-don").session(managerSession))
				.andExpect(status().isOk());
		mockMvc.perform(get("/nhap-hang").session(managerSession))
				.andExpect(status().isOk());
		mockMvc.perform(get("/tai-khoan").session(managerSession))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/hoa-don/lap").session(managerSession))
				.andExpect(status().is3xxRedirection());
		assertTrue(appUserRepository.findById(manager.getId()).orElseThrow().getLastLoginAt() != null);
	}

	@Test
	@Transactional
	void administratorsCanCreateEditAndSafelyDeleteSuppliers() throws Exception {
		AppUser admin = createAccount(AccountRole.ADMIN);
		MockHttpSession session = login(admin.getUsername());

		MvcResult supplierPage = mockMvc.perform(get("/nhap-hang/nha-cung-cap").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("nhaphang/nhacungcap"))
				.andExpect(content().string(containsString("supplier-form-grid")))
				.andExpect(content().string(containsString("Nhập tên nhà cung cấp...")))
				.andExpect(content().string(containsString("active")))
				.andExpect(content().string(containsString("Thao tác")))
				.andExpect(content().string(containsString("supplier-list-panel")))
				.andExpect(content().string(containsString("table-bordered align-middle")))
				.andExpect(content().string(containsString("btn btn-outline-primary")))
				.andExpect(content().string(containsString("btn btn-outline-danger")))
				.andReturn();
		Matcher csrfMatcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
				.matcher(supplierPage.getResponse().getContentAsString());
		assertTrue(csrfMatcher.find(), "Supplier forms must include a CSRF token");
		String csrfToken = csrfMatcher.group(1);

		String supplierName = "Nhà cung cấp kiểm thử " + UUID.randomUUID();
		mockMvc.perform(post("/nhap-hang/nha-cung-cap")
						.session(session)
						.param("name", supplierName)
						.param("phone", "0981234567")
						.param("email", "supplier@example.com")
						.param("address", "Hà Nội")
						.param("_csrf", csrfToken))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/nhap-hang/nha-cung-cap"));

		Supplier supplier = supplierRepository.findAll().stream()
				.filter(item -> item.getName().equals(supplierName))
				.findFirst()
				.orElseThrow();
		mockMvc.perform(get("/nhap-hang/nha-cung-cap/{id}/sua", supplier.getId()).session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(supplierName)));

		String updatedSupplierName = supplierName + " đã sửa";
		mockMvc.perform(post("/nhap-hang/nha-cung-cap")
						.session(session)
						.param("id", supplier.getId().toString())
						.param("name", updatedSupplierName)
						.param("phone", "0900000000")
						.param("email", "updated@example.com")
						.param("address", "Đà Nẵng")
						.param("_csrf", csrfToken))
				.andExpect(status().is3xxRedirection());
		assertEquals(updatedSupplierName,
				supplierRepository.findById(supplier.getId()).orElseThrow().getName());

		PurchaseOrder purchaseOrder = new PurchaseOrder();
		purchaseOrder.setSupplier(supplier);
		purchaseOrder.setCreatedBy(admin);
		purchaseOrder.setCreatedAt(LocalDateTime.now());
		purchaseOrder.setStatus(PurchaseOrderStatus.DRAFT);
		purchaseOrder.setTotalCost(0.0);
		purchaseOrder = purchaseOrderRepository.save(purchaseOrder);
		mockMvc.perform(get("/nhap-hang/{id}", purchaseOrder.getId()).session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("nhaphang/chitiet"))
				.andExpect(content().string(containsString("confirmation-dialog")))
				.andExpect(content().string(containsString("Đúng, đã nhận hàng")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						containsString("confirm('Xác nhận hàng đã về kho?"))));
		mockMvc.perform(get("/nhap-hang").session(session)
						.param("q", purchaseOrder.getId().toString())
						.param("tuNgay", LocalDate.now().toString())
						.param("denNgay", LocalDate.now().toString())
						.param("trangThai", "DRAFT"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("purchase-order-filter")))
				.andExpect(content().string(containsString("invoice-badge is-pending")))
				.andExpect(content().string(containsString("Trang 1/1")))
				.andExpect(content().string(containsString(purchaseOrder.getId().toString())));

		mockMvc.perform(post("/nhap-hang/nha-cung-cap/{id}/xoa", supplier.getId())
						.session(session)
						.param("_csrf", csrfToken))
				.andExpect(status().is3xxRedirection());
		assertTrue(supplierRepository.existsById(supplier.getId()),
				"Suppliers with purchase-order history must not be deleted");

		Supplier unusedSupplier = new Supplier();
		unusedSupplier.setName("Nhà cung cấp có thể xóa " + UUID.randomUUID());
		unusedSupplier = supplierRepository.save(unusedSupplier);
		Long unusedSupplierId = unusedSupplier.getId();
		mockMvc.perform(post("/nhap-hang/nha-cung-cap/{id}/xoa", unusedSupplierId)
						.session(session)
						.param("_csrf", csrfToken))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/nhap-hang/nha-cung-cap"));
		assertTrue(!supplierRepository.existsById(unusedSupplierId));
	}

	@Test
	@Transactional
	void purchasingStaffCanPlaceOrdersAndWarehouseCanReceivePartialDeliveries() throws Exception {
		AppUser purchasingStaff = createAccount(AccountRole.PURCHASING);
		MockHttpSession purchasingSession = login(purchasingStaff.getUsername());
		Supplier supplier = new Supplier();
		supplier.setName("Nhà cung cấp đặt hàng " + UUID.randomUUID());
		supplier = supplierRepository.save(supplier);

		MatHang product = new MatHang();
		product.setTenMatHang("PO-" + UUID.randomUUID().toString().substring(0, 8));
		product.setLoaiMatHang(LoaiMatHang.THUC_PHAM);
		product.setGiaBanGoc(20_000.0);
		product.setQuantityInStock(0);
		product = matHangRepository.save(product);

		mockMvc.perform(get("/nhap-hang/nha-cung-cap").session(purchasingSession))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Thêm nhà cung cấp")))
				.andExpect(content().string(containsString("Đề xuất nhà cung cấp")));
		mockMvc.perform(get("/mat-hang").session(purchasingSession))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Chỉ xem")));

		MvcResult orderForm = mockMvc.perform(get("/nhap-hang/tao").session(purchasingSession))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Ngày giao dự kiến")))
				.andExpect(content().string(containsString("Điều khoản đã thỏa thuận")))
				.andReturn();
		Matcher csrfMatcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
				.matcher(orderForm.getResponse().getContentAsString());
		assertTrue(csrfMatcher.find(), "Purchase order form must include a CSRF token");

		LocalDate expectedDeliveryDate = LocalDate.now().plusDays(3);
		mockMvc.perform(post("/nhap-hang")
						.session(purchasingSession)
						.param("supplierId", supplier.getId().toString())
						.param("expectedDeliveryDate", expectedDeliveryDate.toString())
						.param("paymentTerms", "Thanh toán trong 30 ngày")
						.param("lines[0].productId", product.getMaMatHang().toString())
						.param("lines[0].quantity", "5")
						.param("lines[0].unitCost", "12000")
						.param("_csrf", csrfMatcher.group(1)))
				.andExpect(status().is3xxRedirection());
		PurchaseOrder purchaseOrder = purchaseOrderRepository.findAll().stream()
				.filter(order -> order.getCreatedBy().getId().equals(purchasingStaff.getId()))
				.findFirst()
				.orElseThrow();
		assertEquals(expectedDeliveryDate, purchaseOrder.getExpectedDeliveryDate());
		assertEquals("Thanh toán trong 30 ngày", purchaseOrder.getPaymentTerms());

		mockMvc.perform(get("/nhap-hang/{id}", purchaseOrder.getId()).session(purchasingSession))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Thanh toán trong 30 ngày")))
				.andExpect(content().string(org.hamcrest.Matchers.not(containsString("Xác nhận đã nhận hàng"))));
		mockMvc.perform(post("/nhap-hang/{id}/nhan", purchaseOrder.getId())
						.session(purchasingSession)
						.param("receivedQuantities", "5")
						.param("_csrf", csrfMatcher.group(1)))
				.andExpect(status().is3xxRedirection());
		assertEquals(0, matHangRepository.findById(product.getMaMatHang()).orElseThrow().getQuantityInStock());

		AppUser warehouse = createAccount(AccountRole.WAREHOUSE);
		MockHttpSession warehouseSession = login(warehouse.getUsername());
		MvcResult warehouseOrderPage = mockMvc.perform(get("/nhap-hang/{id}", purchaseOrder.getId())
						.session(warehouseSession))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("số lượng thực nhận")))
				.andReturn();
		Matcher warehouseCsrfMatcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
				.matcher(warehouseOrderPage.getResponse().getContentAsString());
		assertTrue(warehouseCsrfMatcher.find(), "Receiving form must include a CSRF token");
		mockMvc.perform(post("/nhap-hang/{id}/nhan", purchaseOrder.getId())
						.session(warehouseSession)
						.param("receivedQuantities", "2")
						.param("_csrf", warehouseCsrfMatcher.group(1)))
				.andExpect(status().is3xxRedirection());
		PurchaseOrder partlyReceived = purchaseOrderRepository.findById(purchaseOrder.getId()).orElseThrow();
		assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, partlyReceived.getStatus());
		assertEquals(2, matHangRepository.findById(product.getMaMatHang()).orElseThrow().getQuantityInStock());
		assertEquals(2, partlyReceived.getLines().getFirst().getReceivedQuantity());

		mockMvc.perform(post("/nhap-hang/{id}/nhan", purchaseOrder.getId())
						.session(warehouseSession)
						.param("receivedQuantities", "3")
						.param("_csrf", warehouseCsrfMatcher.group(1)))
				.andExpect(status().is3xxRedirection());
		PurchaseOrder fullyReceived = purchaseOrderRepository.findById(purchaseOrder.getId()).orElseThrow();
		assertEquals(PurchaseOrderStatus.RECEIVED, fullyReceived.getStatus());
		assertEquals(5, matHangRepository.findById(product.getMaMatHang()).orElseThrow().getQuantityInStock());
	}

	@Test
	void administratorsCanLockAccountsWithoutRemovingTheirHistory() {
		AppUser account = createAccount(AccountRole.CASHIER);
		Long accountId = account.getId();
		accountService.setEnabled(accountId, false, "admin");
		AppUser locked = appUserRepository.findById(accountId).orElseThrow();
		assertTrue(!locked.isEnabled());
		assertEquals(AccountRole.CASHIER, locked.getRole());
	}

	@Test
	void legacyInvoicesWithoutCashierRemainAfterAccountSoftDeletion() {
		AppUser account = createAccount(AccountRole.CASHIER);
		KhachHang customer = new KhachHang();
		customer.setHoTen("Khách hóa đơn cũ");
		customer.setSoCMND(Long.toString(Math.abs(UUID.randomUUID().getLeastSignificantBits()
				% 1_000_000_000_000L)));
		customer.setLoaiKhachHang(LoaiKhachHang.MUA_LE);
		customer.setDiemThuong(0);
		khachHangRepository.save(customer);
		HoaDon legacyInvoice = new HoaDon();
		legacyInvoice.setKhachHang(customer);
		legacyInvoice.setThoiGianThanhToan(LocalDateTime.now());
		legacyInvoice.setTongTien(1.0);
		legacyInvoice.setDiemThuongCong(0);
		legacyInvoice.setDiemThuongDung(0);
		legacyInvoice.setPhuongThucThanhToan(PaymentMethod.TIEN_MAT);
		hoaDonRepository.saveAndFlush(legacyInvoice);

		try {
			accountService.deleteAccount(account.getId(), "admin");
			AppUser softDeleted = appUserRepository.findById(account.getId()).orElseThrow();
			assertTrue(softDeleted.isDeleted());
			assertTrue(!softDeleted.isEnabled());
			assertTrue(hoaDonRepository.existsById(legacyInvoice.getMaHoaDon()));
			assertEquals(customer.getMaKhachHang(),
					hoaDonRepository.findById(legacyInvoice.getMaHoaDon()).orElseThrow()
							.getKhachHang().getMaKhachHang());
		} finally {
			hoaDonRepository.delete(legacyInvoice);
			khachHangRepository.delete(customer);
		}
	}

	@Test
	void administratorsCanSoftDeleteAccountsWithoutRemovingTheirHistoryOrDeletingThemselves() throws Exception {
		AppUser admin = createAccount(AccountRole.ADMIN);
		MockHttpSession session = login(admin.getUsername());
		MvcResult accountPage = mockMvc.perform(get("/tai-khoan").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("/tai-khoan/")))
				.andExpect(content().string(containsString("account-delete-trigger")))
				.andExpect(content().string(containsString("account-delete-confirmation")))
				.andExpect(content().string(containsString("Xác nhận xóa tài khoản?")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						containsString("return confirm('Bạn có chắc muốn xóa tài khoản"))))
				.andReturn();
		Matcher csrfMatcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
				.matcher(accountPage.getResponse().getContentAsString());
		assertTrue(csrfMatcher.find(), "Account forms must include a CSRF token");
		String csrfToken = csrfMatcher.group(1);

		mockMvc.perform(post("/tai-khoan/{id}/xoa", admin.getId())
						.session(session)
						.param("_csrf", csrfToken))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/tai-khoan"))
				.andExpect(flash().attributeExists("errorMessage"));
		assertTrue(appUserRepository.existsById(admin.getId()), "The active account must not delete itself");

		AppUser employee = createAccount(AccountRole.CASHIER);
		Supplier supplier = new Supplier();
		supplier.setName("Supplier-" + UUID.randomUUID().toString().substring(0, 8));
		supplier = supplierRepository.save(supplier);
		PurchaseOrder purchaseOrder = new PurchaseOrder();
		purchaseOrder.setSupplier(supplier);
		purchaseOrder.setCreatedBy(employee);
		purchaseOrder.setCreatedAt(LocalDateTime.now());
		purchaseOrder.setStatus(PurchaseOrderStatus.DRAFT);
		purchaseOrder.setTotalCost(0.0);
		purchaseOrder = purchaseOrderRepository.save(purchaseOrder);
		Long supplierId = supplier.getId();
		Long purchaseOrderId = purchaseOrder.getId();

		mockMvc.perform(post("/tai-khoan/{id}/xoa", employee.getId())
						.session(session)
						.param("_csrf", csrfToken))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/tai-khoan"))
				.andExpect(flash().attribute("successMessage",
						"Đã chuyển tài khoản sang trạng thái đã xóa; lịch sử vẫn được giữ."));
		AppUser softDeletedEmployee = appUserRepository.findById(employee.getId()).orElseThrow();
		assertTrue(softDeletedEmployee.isDeleted());
		assertTrue(!softDeletedEmployee.isEnabled());
		assertTrue(purchaseOrderRepository.existsById(purchaseOrderId),
				"Purchase-order history must remain after account soft deletion");

		mockMvc.perform(get("/tai-khoan")
						.session(session)
						.param("q", employee.getUsername()))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.not(
						containsString("<td>" + employee.getUsername() + "</td>"))));
		mockMvc.perform(get("/tai-khoan")
						.session(session)
						.param("deleted", "true")
						.param("q", employee.getUsername()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("<td>" + employee.getUsername() + "</td>")))
				.andExpect(content().string(containsString("Đã xóa")));

		purchaseOrderRepository.deleteById(purchaseOrderId);
		supplierRepository.deleteById(supplierId);
	}

	@Test
	void softDeletingAnAccountImmediatelyEndsItsExistingSession() throws Exception {
		AppUser employee = createAccount(AccountRole.CASHIER);
		MockHttpSession session = login(employee.getUsername());
		accountService.deleteAccount(employee.getId(), "admin");

		mockMvc.perform(get("/hoa-don/lap").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dang-nhap?deleted"));
	}

	@Test
	void lockingAnAccountImmediatelyEndsItsExistingSession() throws Exception {
		AppUser employee = createAccount(AccountRole.CASHIER);
		MockHttpSession session = login(employee.getUsername());
		accountService.setEnabled(employee.getId(), false, "admin");

		mockMvc.perform(get("/hoa-don/lap").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dang-nhap?locked"));
	}

	private final List<AppUser> testAccounts = new ArrayList<>();

	@AfterEach
	void removeTestAccounts() {
		appUserRepository.deleteAll(testAccounts);
		testAccounts.clear();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void administratorCanOpenAccountManagementAfterLogin() throws Exception {
		AppUser admin = createAccount(AccountRole.ADMIN);
		MockHttpSession session = login(admin.getUsername());

		MvcResult accountPage = mockMvc.perform(get("/tai-khoan").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("taikhoan/danhsach"))
				.andExpect(content().string(containsString("Nhân viên bán hàng")))
				.andExpect(content().string(containsString("account-create-actions")))
				.andExpect(content().string(containsString("Xem danh sách tài khoản")))
				.andExpect(content().string(containsString("account-list-panel")))
				.andExpect(content().string(containsString("account-actions")))
				.andExpect(content().string(containsString("Đang đăng nhập")))
				.andExpect(content().string(containsString("aria-label=\"Điều hướng chính\"")))
				.andExpect(content().string(containsString("aria-current=\"page\"")))
				.andExpect(result -> {
					String html = result.getResponse().getContentAsString();
					assertTrue(html.indexOf("class=\"menu\"") < html.indexOf("class=\"app-main\""));
					assertTrue(html.indexOf("class=\"app-main\"") < html.indexOf("class=\"container\""));
					assertTrue(html.indexOf("class=\"container\"") < html.indexOf("account-create-actions"));
				})
				.andReturn();
		Matcher csrfMatcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
				.matcher(accountPage.getResponse().getContentAsString());
		assertTrue(csrfMatcher.find(), "Account form must include a CSRF token");

		String employeeUsername = "test-employee-" + UUID.randomUUID();
		mockMvc.perform(post("/tai-khoan")
						.session(session)
						.param("username", employeeUsername)
						.param("fullName", "Nhân viên kiểm thử")
						.param("password", TEST_PASSWORD)
						.param("role", AccountRole.CASHIER.name())
						.param("_csrf", csrfMatcher.group(1)))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/tai-khoan"));

		AppUser createdEmployee = appUserRepository.findByUsernameIgnoreCase(employeeUsername)
				.orElseThrow();
		testAccounts.add(createdEmployee);
		assertEquals(AccountRole.CASHIER, createdEmployee.getRole());
		assertEquals("Nhân viên kiểm thử", createdEmployee.getFullName());
		assertTrue(passwordEncoder.matches(TEST_PASSWORD, createdEmployee.getPasswordHash()));
		assertTrue(createdEmployee.isEnabled());
		assertTrue(createdEmployee.getCreatedAt() != null);

		MvcResult editPage = mockMvc.perform(get("/tai-khoan/{id}/sua", createdEmployee.getId()).session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("taikhoan/sua"))
				.andReturn();
		Matcher editCsrfMatcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
				.matcher(editPage.getResponse().getContentAsString());
		assertTrue(editCsrfMatcher.find(), "Profile form must include a CSRF token");
		mockMvc.perform(post("/tai-khoan/{id}/sua", createdEmployee.getId())
						.session(session)
						.param("fullName", "Nguyễn Văn A")
						.param("phone", "0900000000")
						.param("email", "a@example.com")
						.param("role", AccountRole.WAREHOUSE.name())
						.param("_csrf", editCsrfMatcher.group(1)))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/tai-khoan"));
		AppUser updatedEmployee = appUserRepository.findById(createdEmployee.getId()).orElseThrow();
		assertEquals("Nguyễn Văn A", updatedEmployee.getFullName());
		assertEquals("0900000000", updatedEmployee.getPhone());
		assertEquals("a@example.com", updatedEmployee.getEmail());
		assertEquals(AccountRole.WAREHOUSE, updatedEmployee.getRole());
	}

	@Test
	void administratorsCanFilterAndPaginateAccounts() throws Exception {
		AppUser admin = createAccount(AccountRole.ADMIN);
		MockHttpSession session = login(admin.getUsername());
		String token = UUID.randomUUID().toString().substring(0, 8);

		for (int index = 0; index < 11; index++) {
			String username = "staff-" + token + "-" + String.format("%02d", index);
			AppUser employee = new AppUser(username, "not-used", index % 2 == 0
					? AccountRole.WAREHOUSE : AccountRole.CASHIER);
			employee.setFullName("Nhân viên " + token + " " + index);
			testAccounts.add(appUserRepository.save(employee));
		}

		mockMvc.perform(get("/tai-khoan").session(session).param("q", token))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("account-filter")))
				.andExpect(content().string(containsString("account-pagination")))
				.andExpect(content().string(containsString("account-list-panel\" open")))
				.andExpect(content().string(containsString("11 tài khoản")))
				.andExpect(content().string(containsString("Nhập tên nhân viên")))
				.andExpect(content().string(containsString("<table>")))
				.andExpect(content().string(containsString("staff-" + token + "-00")))
				.andExpect(content().string(containsString("staff-" + token + "-04")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						containsString("staff-" + token + "-05"))))
				.andExpect(content().string(containsString("Trang 1/3")));

		mockMvc.perform(get("/tai-khoan")
						.session(session)
						.param("q", token)
						.param("role", AccountRole.WAREHOUSE.name()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("staff-" + token + "-00")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						containsString("staff-" + token + "-10"))))
				.andExpect(content().string(containsString("Trang 1/2")));

		mockMvc.perform(get("/tai-khoan")
						.session(session)
						.param("q", token)
						.param("role", AccountRole.WAREHOUSE.name())
						.param("page", "1"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("staff-" + token + "-10")))
				.andExpect(content().string(containsString("Trang 2/2")));

		mockMvc.perform(get("/tai-khoan")
						.session(session)
						.param("q", token)
						.param("enabled", "false"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Không có tài khoản nào khớp với")))
				.andExpect(content().string(org.hamcrest.Matchers.not(containsString("<table>"))));
	}

	@Test
	void administratorCanOpenStockPurchasingAndReportingPages() throws Exception {
		AppUser admin = createAccount(AccountRole.ADMIN);
		MockHttpSession session = login(admin.getUsername());

		mockMvc.perform(get("/mat-hang").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("mathang/danhsach"))
				.andExpect(content().string(containsString("Tìm kiếm mặt hàng")))
				.andExpect(content().string(containsString("Nhập kho")))
				.andExpect(content().string(containsString("aria-current=\"page\"")));
		mockMvc.perform(get("/khach-hang").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("aria-current=\"page\"")));
		mockMvc.perform(get("/mat-hang/them").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("mathang/form"));
		mockMvc.perform(get("/nhap-hang").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("nhaphang/danhsach"))
				.andExpect(content().string(containsString("aria-current=\"page\"")))
				.andExpect(content().string(containsString("purchase-order-filter")))
				.andExpect(content().string(containsString("invoice-history-table align-middle purchase-order-table")))
				.andExpect(content().string(containsString("Hiển thị ")));
		mockMvc.perform(get("/nhap-hang/tao").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("nhaphang/form"))
				.andExpect(content().string(containsString("purchase-page-heading")))
				.andExpect(content().string(containsString("purchase-form-actions")))
				.andExpect(content().string(containsString("name=\"lines[0].productId\"")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						containsString("name=\"lines[1].productId\""))));
		mockMvc.perform(get("/nhap-hang/nha-cung-cap").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("nhaphang/nhacungcap"));
		mockMvc.perform(get("/hoa-don").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("aria-current=\"page\"")))
				.andExpect(content().string(containsString("invoice-filter")))
				.andExpect(content().string(containsString("invoice-history-table align-middle")))
				.andExpect(content().string(containsString("Tất cả trạng thái")));
		String unmatchedSearch = "khong-co-hoa-don-" + UUID.randomUUID();
		mockMvc.perform(get("/hoa-don").session(session).param("q", unmatchedSearch))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("invoice-no-results")))
				.andExpect(content().string(containsString("Không tìm thấy hóa đơn phù hợp với từ khóa")))
				.andExpect(content().string(containsString(unmatchedSearch)))
				.andExpect(content().string(containsString("Không có dữ liệu để hiển thị.")));
		mockMvc.perform(get("/hoa-don/bao-cao").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("hoadon/baocao"))
				.andExpect(content().string(containsString("aria-current=\"page\"")))
				.andExpect(content().string(containsString("report-stat-grid")))
				.andExpect(content().string(containsString("Tổng doanh thu")))
				.andExpect(content().string(containsString("Số hóa đơn")))
				.andExpect(content().string(containsString("Tổng giá vốn")))
				.andExpect(content().string(containsString("Lợi nhuận gộp")))
				.andExpect(content().string(containsString("report-table align-middle")))
				.andExpect(content().string(containsString("table-light")))
				.andExpect(content().string(containsString("btn btn-outline-primary")));
	}

	@Test
	void productListSearchAndLocalizedTypeAreAvailableToAdminAndWarehouse() throws Exception {
		MatHang product = new MatHang();
		product.setTenMatHang("Product-" + UUID.randomUUID().toString().substring(0, 8));
		product.setLoaiMatHang(LoaiMatHang.HANG_THOI_TRANG);
		product.setGiaBanGoc(10_000.0);
		product.setHangSanXuat("Brand-" + UUID.randomUUID());
		product.setQuantityInStock(8);
		matHangRepository.save(product);

		try {
			String search = product.getTenMatHang();
			AppUser admin = createAccount(AccountRole.ADMIN);
			MockHttpSession adminSession = login(admin.getUsername());
			mockMvc.perform(get("/mat-hang").session(adminSession))
					.andExpect(status().isOk())
					.andExpect(content().string(containsString("＋ Thêm mặt hàng")));
			mockMvc.perform(get("/mat-hang").param("q", search).session(adminSession))
					.andExpect(status().isOk())
					.andExpect(content().string(containsString(search)))
					.andExpect(content().string(containsString("Hàng thời trang")))
					.andExpect(content().string(containsString("Còn hàng")))
					.andExpect(content().string(containsString("btn-outline-primary")));
			String missingSearch = "missing-" + UUID.randomUUID().toString().substring(0, 8);
			mockMvc.perform(get("/mat-hang").param("q", missingSearch).session(adminSession))
					.andExpect(status().isOk())
					.andExpect(content().string(containsString("Không tìm thấy mặt hàng phù hợp")))
					.andExpect(content().string(containsString(missingSearch)));

			AppUser warehouse = createAccount(AccountRole.WAREHOUSE);
			MockHttpSession warehouseSession = login(warehouse.getUsername());
			mockMvc.perform(get("/mat-hang").session(warehouseSession))
					.andExpect(status().isOk())
					.andExpect(content().string(containsString("Tìm kiếm mặt hàng")));
			mockMvc.perform(get("/mat-hang").param("q", search).session(warehouseSession))
					.andExpect(status().isOk())
					.andExpect(content().string(containsString(search)))
					.andExpect(content().string(containsString("Chỉ xem")))
					.andExpect(content().string(containsString("Tìm kiếm mặt hàng")));
		} finally {
			matHangRepository.delete(product);
		}
	}

	@Test
	@Transactional
	void administratorsCanSearchAndPaginateCustomers() throws Exception {
		AppUser admin = createAccount(AccountRole.ADMIN);
		MockHttpSession session = login(admin.getUsername());
		String searchToken = "KH" + UUID.randomUUID().toString().substring(0, 8);
		String firstCustomerName = null;
		String firstCustomerIdNumber = null;
		for (int index = 0; index < 11; index++) {
			KhachHang customer = new KhachHang();
			customer.setHoTen("Khách " + searchToken + " " + index);
			customer.setSoCMND(Long.toString(100_000_000_000L
					+ Math.abs(UUID.randomUUID().getLeastSignificantBits() % 900_000_000_000L)));
			customer.setLoaiKhachHang(LoaiKhachHang.MUA_LE);
			khachHangRepository.save(customer);
			if (index == 0) {
				firstCustomerName = customer.getHoTen();
				firstCustomerIdNumber = customer.getSoCMND();
			}
		}

		mockMvc.perform(get("/khach-hang").session(session).param("q", searchToken))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("customer-filter")))
				.andExpect(content().string(containsString("customer-table align-middle")))
				.andExpect(content().string(containsString("Tìm theo họ tên, SĐT, CCCD...")))
				.andExpect(content().string(containsString(firstCustomerName)))
				.andExpect(content().string(containsString(">0 điểm</span>")))
				.andExpect(content().string(containsString("btn btn-sm btn-outline-primary")))
				.andExpect(content().string(containsString("btn btn-sm btn-outline-danger")))
				.andExpect(content().string(containsString("Trang 1/2")))
				.andExpect(content().string(containsString("class=\"text-center\">-</td>")));
		mockMvc.perform(get("/khach-hang").session(session).param("q", searchToken).param("page", "1"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Trang 2/2")))
				.andExpect(content().string(containsString(searchToken + " 10")));
		mockMvc.perform(get("/khach-hang").session(session).param("q", firstCustomerIdNumber))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(firstCustomerName)));

		String missingName = "khong-co-khach-hang-" + UUID.randomUUID();
		mockMvc.perform(get("/khach-hang").session(session).param("q", missingName))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Không tìm thấy khách hàng phù hợp")))
				.andExpect(content().string(containsString(missingName)));
	}

	@Test
	void employeeIsDeniedAccessToAccountManagement() throws Exception {
		AppUser employee = createAccount(AccountRole.CASHIER);
		MockHttpSession session = login(employee.getUsername());

		mockMvc.perform(get("/khach-hang").session(session))
				.andExpect(status().is3xxRedirection());

		mockMvc.perform(get("/tai-khoan").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/khong-co-quyen"));

		mockMvc.perform(get("/khong-co-quyen").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("khong-co-quyen"));
		mockMvc.perform(get("/hoa-don/lap").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("app-topbar")))
				.andExpect(content().string(containsString("page-heading")))
				.andExpect(content().string(containsString("cashier-sale-notice")))
				.andExpect(content().string(containsString("cashier-customer-action")))
				.andExpect(content().string(containsString(employee.getUsername())))
				.andExpect(content().string(containsString("Nhân viên bán hàng")))
				.andExpect(content().string(containsString("Đăng xuất")));
	}

	@Test
	void employeeCanOpenProductInvoiceHistoryAndReportPages() throws Exception {
		AppUser employee = createAccount(AccountRole.CASHIER);
		MockHttpSession session = login(employee.getUsername());

		mockMvc.perform(get("/mat-hang").session(session))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/mat-hang/tim-kiem").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string("[]"));
		mockMvc.perform(get("/khach-hang/them").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("khachhang/form"));
		mockMvc.perform(get("/hoa-don/lap").session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("hoadon/form"))
				.andExpect(content().string(containsString("page-heading")))
				.andExpect(content().string(containsString("cashier-sale-notice")))
				.andExpect(content().string(containsString("＋ Thêm khách hàng mới")))
				.andExpect(content().string(containsString("Bán hàng")))
				.andExpect(content().string(containsString("aria-current=\"page\"")))
				.andExpect(content().string(containsString("Đăng xuất")))
				.andExpect(content().string(containsString("app-topbar")))
				.andExpect(content().string(containsString(employee.getUsername())))
				.andExpect(content().string(containsString("Nhân viên bán hàng")));
		mockMvc.perform(get("/hoa-don").session(session))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/hoa-don/bao-cao").session(session))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	void employeeCanResetPasswordWithSingleUseEmailLink() throws Exception {
		AppUser employee = createAccount(AccountRole.CASHIER);
		employee.setEmail("reset-" + UUID.randomUUID() + "@example.com");
		appUserRepository.save(employee);
		MockHttpSession existingLoginSession = login(employee.getUsername());

		MvcResult requestPage = mockMvc.perform(get("/quen-mat-khau"))
				.andExpect(status().isOk())
				.andExpect(view().name("quenmatkhau"))
				.andReturn();
		Matcher requestCsrf = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
				.matcher(requestPage.getResponse().getContentAsString());
		assertTrue(requestCsrf.find(), "Password reset request form must include a CSRF token");
		MockHttpSession session = (MockHttpSession) requestPage.getRequest().getSession(false);

		mockMvc.perform(post("/quen-mat-khau")
						.session(session)
						.param("email", employee.getEmail())
						.param("_csrf", requestCsrf.group(1)))
				.andExpect(status().isOk())
				.andExpect(model().attribute("requestSubmitted", true));

		var messageCaptor = forClass(SimpleMailMessage.class);
		verify(mailSender).send(messageCaptor.capture());
		assertEquals(employee.getEmail(), messageCaptor.getValue().getTo()[0]);
		Matcher tokenMatcher = Pattern.compile("token=([A-Za-z0-9_-]+)")
				.matcher(messageCaptor.getValue().getText());
		assertTrue(tokenMatcher.find(), "Reset email must contain a reset token");
		String token = tokenMatcher.group(1);

		MvcResult resetPage = mockMvc.perform(get("/dat-lai-mat-khau")
						.session(session)
						.param("token", token))
				.andExpect(status().isOk())
				.andExpect(view().name("datlaimatkhau"))
				.andReturn();
		Matcher resetCsrf = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
				.matcher(resetPage.getResponse().getContentAsString());
		assertTrue(resetCsrf.find(), "Password reset form must include a CSRF token");

		mockMvc.perform(post("/dat-lai-mat-khau")
						.session(session)
						.param("token", token)
						.param("password", "New-strong-password-123")
						.param("confirmPassword", "New-strong-password-123")
						.param("_csrf", resetCsrf.group(1)))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dang-nhap"));
		AppUser resetAccount = appUserRepository.findById(employee.getId()).orElseThrow();
		assertTrue(passwordEncoder.matches("New-strong-password-123", resetAccount.getPasswordHash()));
		assertTrue(passwordResetTokenRepository.findById(employee.getId()).isEmpty());
		mockMvc.perform(get("/hoa-don/lap").session(existingLoginSession))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dang-nhap?passwordChanged"));

		mockMvc.perform(post("/dat-lai-mat-khau")
						.session(session)
						.param("token", token)
						.param("password", "Another-strong-password-123")
						.param("confirmPassword", "Another-strong-password-123")
						.param("_csrf", resetCsrf.group(1)))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("resetError"));
		assertTrue(passwordEncoder.matches("New-strong-password-123",
				appUserRepository.findById(employee.getId()).orElseThrow().getPasswordHash()));
	}

	@Test
	void administratorCanSendResetLinkForAnAccount() throws Exception {
		AppUser admin = createAccount(AccountRole.ADMIN);
		AppUser employee = createAccount(AccountRole.CASHIER);
		employee.setEmail("admin-reset-" + UUID.randomUUID() + "@example.com");
		appUserRepository.save(employee);
		MockHttpSession session = login(admin.getUsername());

		MvcResult accountPage = mockMvc.perform(get("/tai-khoan").session(session))
				.andExpect(status().isOk())
				.andReturn();
		Matcher csrfMatcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
				.matcher(accountPage.getResponse().getContentAsString());
		assertTrue(csrfMatcher.find(), "Account management form must include a CSRF token");

		mockMvc.perform(post("/tai-khoan/{id}/dat-lai-mat-khau", employee.getId())
						.session(session)
						.param("_csrf", csrfMatcher.group(1)))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("successMessage",
						"Đã gửi liên kết đặt lại mật khẩu đến email của tài khoản."));
		var messageCaptor = forClass(SimpleMailMessage.class);
		verify(mailSender).send(messageCaptor.capture());
		assertEquals(employee.getEmail(), messageCaptor.getValue().getTo()[0]);
	}

	@Test
	void passwordResetRequestDoesNotShowServerErrorWhenMailIsUnavailable() throws Exception {
		AppUser employee = createAccount(AccountRole.CASHIER);
		employee.setEmail("unavailable-mail-" + UUID.randomUUID() + "@example.com");
		appUserRepository.save(employee);
		doThrow(new IllegalStateException("SMTP host is not configured"))
				.when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

		MvcResult requestPage = mockMvc.perform(get("/quen-mat-khau"))
				.andExpect(status().isOk())
				.andReturn();
		Matcher csrfMatcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
				.matcher(requestPage.getResponse().getContentAsString());
		assertTrue(csrfMatcher.find(), "Password reset request form must include a CSRF token");

		mockMvc.perform(post("/quen-mat-khau")
						.session((MockHttpSession) requestPage.getRequest().getSession(false))
						.param("email", employee.getEmail())
						.param("_csrf", csrfMatcher.group(1)))
				.andExpect(status().isOk())
				.andExpect(view().name("quenmatkhau"))
				.andExpect(model().attribute("requestSubmitted", true));
		assertTrue(passwordResetTokenRepository.findById(employee.getId()).isEmpty());
	}

	@Test
	void passwordResetRequestDoesNotFailWhenEmailBelongsToMultipleAccounts() throws Exception {
		String sharedEmail = "shared-reset-" + UUID.randomUUID() + "@example.com";
		AppUser firstAccount = createAccount(AccountRole.CASHIER);
		firstAccount.setEmail(sharedEmail);
		appUserRepository.save(firstAccount);
		AppUser secondAccount = createAccount(AccountRole.WAREHOUSE);
		secondAccount.setEmail(sharedEmail);
		appUserRepository.save(secondAccount);

		MvcResult requestPage = mockMvc.perform(get("/quen-mat-khau"))
				.andExpect(status().isOk())
				.andReturn();
		Matcher csrfMatcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
				.matcher(requestPage.getResponse().getContentAsString());
		assertTrue(csrfMatcher.find(), "Password reset request form must include a CSRF token");

		mockMvc.perform(post("/quen-mat-khau")
						.session((MockHttpSession) requestPage.getRequest().getSession(false))
						.param("email", sharedEmail)
						.param("_csrf", csrfMatcher.group(1)))
				.andExpect(status().isOk())
				.andExpect(view().name("quenmatkhau"))
				.andExpect(model().attribute("requestSubmitted", true));
		verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
		assertTrue(passwordResetTokenRepository.findById(firstAccount.getId()).isEmpty());
		assertTrue(passwordResetTokenRepository.findById(secondAccount.getId()).isEmpty());
	}

	@Test
	@Transactional
	void confirmedPurchaseFeedsFefoSaleAndLoyaltyAndPdf() throws Exception {
		String cashierUsername = "test-cashier-" + UUID.randomUUID();
		AppUser cashier = new AppUser(
				cashierUsername,
				passwordEncoder.encode(TEST_PASSWORD),
				AccountRole.CASHIER
		);
		cashier.setFullName("Vũ Hữu Tuấn");
		appUserRepository.save(cashier);

		KhachHang customer = new KhachHang();
		customer.setHoTen("Khách kiểm thử");
		customer.setSoCMND(Long.toString(Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1_000_000_000_000L)));
		customer.setLoaiKhachHang(LoaiKhachHang.MUA_LE);
		customer.setDiemThuong(0);
		khachHangRepository.save(customer);

		MatHang product = new MatHang();
		product.setTenMatHang("FEFO-" + UUID.randomUUID().toString().substring(0, 8));
		product.setLoaiMatHang(LoaiMatHang.THUC_PHAM);
		product.setGiaBanGoc(20_000.0);
		product.setQuantityInStock(0);
		matHangRepository.save(product);

		Supplier supplier = new Supplier();
		supplier.setName("Nhà cung cấp kiểm thử " + UUID.randomUUID());
		supplierRepository.save(supplier);

		PurchaseOrderLineForm earlyExpiry = purchaseLine(product, 2, 10_000, LocalDate.now().plusDays(5));
		PurchaseOrderLineForm lateExpiry = purchaseLine(product, 3, 12_000, LocalDate.now().plusDays(20));
		var purchaseOrder = purchasingService.createDraft(
				supplier.getId(), cashier.getId(), List.of(earlyExpiry, lateExpiry));
		assertEquals(0, product.getQuantityInStock());
		purchasingService.receive(purchaseOrder.getId());
		assertEquals(5, matHangRepository.findById(product.getMaMatHang()).orElseThrow().getQuantityInStock());
		mockMvc.perform(get("/hoa-don/lap").session(login(cashierUsername)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(product.getTenMatHang())))
				.andExpect(content().string(containsString("20,000 đ")))
				.andExpect(content().string(containsString("id=\"product-0\"")))
				.andExpect(content().string(org.hamcrest.Matchers.not(containsString("id=\"product-1\""))));

		List<ProductBatch> batches = productBatchRepository.findAll().stream()
				.filter(batch -> batch.getProduct().getMaMatHang().equals(product.getMaMatHang()))
				.sorted(Comparator.comparing(ProductBatch::getExpiryDate))
				.toList();
		assertEquals(2, batches.size());
		assertEquals(2, batches.getFirst().getRemainingQuantity());
		assertEquals(3, batches.getLast().getRemainingQuantity());

		assertThrows(IllegalArgumentException.class, () -> hoaDonService.createInvoice(
				customer.getMaKhachHang(),
				List.of(invoiceLine(product, 6)),
				0,
				PaymentMethod.TIEN_MAT,
				120_000.0,
				false,
				cashier.getUsername()
		));
		assertEquals(5, matHangRepository.findById(product.getMaMatHang()).orElseThrow().getQuantityInStock());

		HoaDon firstInvoice = hoaDonService.createInvoice(
				customer.getMaKhachHang(),
				List.of(invoiceLine(product, 3)),
				0,
				PaymentMethod.TIEN_MAT,
				70_000.0,
				false,
				cashier.getUsername()
		);
		assertEquals(60_000.0, firstInvoice.getTongTien());
		assertEquals(10_000.0, firstInvoice.getTienThua());
		assertEquals(6, firstInvoice.getDiemThuongCong());
		assertEquals(0, batches.getFirst().getRemainingQuantity());
		assertEquals(2, batches.getLast().getRemainingQuantity());
		assertEquals(2, matHangRepository.findById(product.getMaMatHang()).orElseThrow().getQuantityInStock());
		assertTrue(hoaDonService.summarize(LocalDate.now(), LocalDate.now()).cashierSales().stream()
				.anyMatch(sale -> sale.cashier().equals("Vũ Hữu Tuấn")));

		assertThrows(IllegalArgumentException.class, () -> hoaDonService.createInvoice(
				customer.getMaKhachHang(),
				List.of(invoiceLine(product, 1)),
				1,
				PaymentMethod.VIET_QR,
				null,
				false,
				cashier.getUsername()
		));
		assertEquals(2, matHangRepository.findById(product.getMaMatHang()).orElseThrow().getQuantityInStock());

		HoaDon secondInvoice = hoaDonService.createInvoice(
				customer.getMaKhachHang(),
				List.of(invoiceLine(product, 1)),
				1,
				PaymentMethod.VIET_QR,
				null,
				true,
				cashier.getUsername()
		);
		assertEquals(19_000.0, secondInvoice.getTongTien());
		assertEquals(1, secondInvoice.getDiemThuongCong());
		assertEquals(6, khachHangRepository.findById(customer.getMaKhachHang()).orElseThrow().getDiemThuong());
		assertEquals(1, matHangRepository.findById(product.getMaMatHang()).orElseThrow().getQuantityInStock());
		assertEquals(1, batches.getLast().getRemainingQuantity());

		AppUser manager = appUserRepository.save(new AppUser("test-manager-" + UUID.randomUUID(),
				passwordEncoder.encode(TEST_PASSWORD), AccountRole.MANAGER));
		testAccounts.add(manager);
		mockMvc.perform(get("/hoa-don/{id}", firstInvoice.getMaHoaDon())
						.session(login(manager.getUsername())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("cancel-invoice-confirmation")))
				.andExpect(content().string(containsString("Xác nhận hủy")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						containsString("confirm('Hủy hóa đơn này"))));
		hoaDonService.cancelInvoice(secondInvoice.getMaHoaDon(), "Kiểm thử hủy hóa đơn", manager.getUsername());
		assertTrue(hoaDonRepository.findById(secondInvoice.getMaHoaDon()).orElseThrow().isCancelled());
		assertEquals(2, matHangRepository.findById(product.getMaMatHang()).orElseThrow().getQuantityInStock());
		assertEquals(2, batches.getLast().getRemainingQuantity());
		assertEquals(6, khachHangRepository.findById(customer.getMaKhachHang()).orElseThrow().getDiemThuong());
		var beforeFirstCancellation = hoaDonService.summarize(LocalDate.now(), LocalDate.now());
		hoaDonService.cancelInvoice(firstInvoice.getMaHoaDon(), "Kiểm thử hoàn điểm", manager.getUsername());
		assertEquals(5, matHangRepository.findById(product.getMaMatHang()).orElseThrow().getQuantityInStock());
		assertEquals(0, khachHangRepository.findById(customer.getMaKhachHang()).orElseThrow().getDiemThuong());
		var afterFirstCancellation = hoaDonService.summarize(LocalDate.now(), LocalDate.now());
		assertEquals(beforeFirstCancellation.invoiceCount() - 1, afterFirstCancellation.invoiceCount());
		assertEquals(0, beforeFirstCancellation.revenue()
				.subtract(afterFirstCancellation.revenue()).compareTo(java.math.BigDecimal.valueOf(60_000)));
		mockMvc.perform(get("/hoa-don")
						.session(login(manager.getUsername()))
						.param("q", firstInvoice.getMaHoaDon().toString())
						.param("tuNgay", LocalDate.now().toString())
						.param("denNgay", LocalDate.now().toString())
						.param("trangThai", "CANCELLED")
						.param("page", "0"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(firstInvoice.getMaHoaDon().toString())))
				.andExpect(content().string(containsString("invoice-badge is-cancelled")))
				.andExpect(content().string(containsString("Trang 1/1")));
		assertThrows(IllegalStateException.class, () -> hoaDonService.cancelInvoice(
				firstInvoice.getMaHoaDon(), "Hủy lần nữa", manager.getUsername()));

		byte[] receipt = receiptPdfService.create(hoaDonRepository.findById(firstInvoice.getMaHoaDon()).orElseThrow());
		assertTrue(new String(receipt, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF"));
		try (var pdfDocument = org.apache.pdfbox.Loader.loadPDF(receipt)) {
			String receiptText = new org.apache.pdfbox.text.PDFTextStripper().getText(pdfDocument);
			assertTrue(receiptText.contains("HÓA ĐƠN BÁN HÀNG"));
			assertTrue(receiptText.contains("Khách hàng:"));
			assertTrue(receiptText.contains("HÓA ĐƠN ĐÃ HỦY"));
			assertTrue(receiptText.contains("Hẹn gặp lại quý khách!"));
		}

		MockHttpSession cashierSession = login(cashierUsername);
		mockMvc.perform(get("/hoa-don/{id}", firstInvoice.getMaHoaDon()).session(cashierSession))
				.andExpect(status().isOk())
				.andExpect(view().name("hoadon/chitiet"))
				.andExpect(content().string(containsString("invoice-detail")))
				.andExpect(content().string(containsString("Sản phẩm đã mua")))
				.andExpect(content().string(containsString("Tổng thanh toán")));
		mockMvc.perform(get("/hoa-don/{id}/pdf", firstInvoice.getMaHoaDon()).session(cashierSession))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/pdf"));
	}

	private AppUser createAccount(AccountRole role) {
		String username = "test-" + UUID.randomUUID();
		AppUser account = appUserRepository.save(
				new AppUser(username, passwordEncoder.encode(TEST_PASSWORD), role)
		);
		testAccounts.add(account);
		return account;
	}

	private PurchaseOrderLineForm purchaseLine(MatHang product, int quantity, double cost, LocalDate expiry) {
		PurchaseOrderLineForm line = new PurchaseOrderLineForm();
		line.setProductId(product.getMaMatHang());
		line.setQuantity(quantity);
		line.setUnitCost(cost);
		line.setManufactureDate(LocalDate.now().minusDays(2));
		line.setExpiryDate(expiry);
		return line;
	}

	private InvoiceLineForm invoiceLine(MatHang product, int quantity) {
		InvoiceLineForm line = new InvoiceLineForm();
		line.setProductId(product.getMaMatHang());
		line.setQuantity(quantity);
		return line;
	}

	private MockHttpSession login(String username) throws Exception {
		AccountRole role = appUserRepository.findByUsernameIgnoreCase(username).orElseThrow().getRole();
		String destination = switch (role) {
			case WAREHOUSE -> "/nhap-hang";
			case PURCHASING -> "/nhap-hang";
			case MANAGER -> "/hoa-don/bao-cao";
			default -> "/hoa-don/lap";
		};
		MvcResult loginPage = mockMvc.perform(get("/dang-nhap"))
				.andExpect(status().isOk())
				.andReturn();
		Matcher csrfMatcher = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
				.matcher(loginPage.getResponse().getContentAsString());
		assertTrue(csrfMatcher.find(), "Login form must include a CSRF token");

		MvcResult loginResult = mockMvc.perform(post("/dang-nhap")
						.session((MockHttpSession) loginPage.getRequest().getSession(false))
						.param("username", username)
						.param("password", TEST_PASSWORD)
						.param("_csrf", csrfMatcher.group(1)))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(destination))
				.andReturn();

		return (MockHttpSession) loginResult.getRequest().getSession(false);
	}
}
