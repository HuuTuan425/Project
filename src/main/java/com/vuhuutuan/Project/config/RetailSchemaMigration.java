package com.vuhuutuan.Project.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RetailSchemaMigration implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(RetailSchemaMigration.class);
    private final DataSource dataSource;

    public RetailSchemaMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String catalog = connection.getCatalog();
            requireTable(metadata, catalog, "mat_hang");
            requireTable(metadata, catalog, "hoa_don");
            requireTable(metadata, catalog, "chi_tiet_hoa_don");
            requireTable(metadata, catalog, "tai_khoan");

            ensureColumn(statement, metadata, catalog, "khach_hang", "da_xoa_luc",
                    "ALTER TABLE khach_hang ADD COLUMN da_xoa_luc DATETIME NULL");
            ensureColumn(statement, metadata, catalog, "mat_hang", "da_xoa_luc",
                    "ALTER TABLE mat_hang ADD COLUMN da_xoa_luc DATETIME NULL");

            ensureColumn(statement, metadata, catalog, "tai_khoan", "ho_ten",
                    "ALTER TABLE tai_khoan ADD COLUMN ho_ten VARCHAR(120) NULL");
            ensureColumn(statement, metadata, catalog, "tai_khoan", "so_dien_thoai",
                    "ALTER TABLE tai_khoan ADD COLUMN so_dien_thoai VARCHAR(30) NULL");
            ensureColumn(statement, metadata, catalog, "tai_khoan", "email",
                    "ALTER TABLE tai_khoan ADD COLUMN email VARCHAR(150) NULL");
            ensureColumn(statement, metadata, catalog, "tai_khoan", "ngay_tao",
                    "ALTER TABLE tai_khoan ADD COLUMN ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
            ensureColumn(statement, metadata, catalog, "tai_khoan", "dang_nhap_lan_cuoi",
                    "ALTER TABLE tai_khoan ADD COLUMN dang_nhap_lan_cuoi DATETIME NULL");
            ensureColumn(statement, metadata, catalog, "tai_khoan", "da_xoa_luc",
                    "ALTER TABLE tai_khoan ADD COLUMN da_xoa_luc DATETIME NULL");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS password_reset_token (
                        account_id BIGINT NOT NULL,
                        token_hash CHAR(64) NOT NULL,
                        expires_at DATETIME(6) NOT NULL,
                        PRIMARY KEY (account_id),
                        UNIQUE KEY uk_password_reset_token_hash (token_hash),
                        CONSTRAINT fk_password_reset_token_account
                            FOREIGN KEY (account_id) REFERENCES tai_khoan(id) ON DELETE CASCADE
                    )
                    """);
            ensureResetTokenExpiryPrecision(statement, metadata, catalog);
            ensureColumn(statement, metadata, catalog, "hoa_don", "da_huy",
                    "ALTER TABLE hoa_don ADD COLUMN da_huy BOOLEAN NOT NULL DEFAULT FALSE");
            ensureColumn(statement, metadata, catalog, "hoa_don", "ngay_huy",
                    "ALTER TABLE hoa_don ADD COLUMN ngay_huy DATETIME NULL");
            ensureColumn(statement, metadata, catalog, "hoa_don", "nguoi_huy",
                    "ALTER TABLE hoa_don ADD COLUMN nguoi_huy BIGINT NULL");
            ensureColumn(statement, metadata, catalog, "hoa_don", "ly_do_huy",
                    "ALTER TABLE hoa_don ADD COLUMN ly_do_huy VARCHAR(500) NULL");
            ensureForeignKey(statement, metadata, catalog, "hoa_don", "fk_hoa_don_nguoi_huy",
                    "ALTER TABLE hoa_don ADD CONSTRAINT fk_hoa_don_nguoi_huy "
                            + "FOREIGN KEY (nguoi_huy) REFERENCES tai_khoan(id)");

            if (!columnExists(metadata, catalog, "mat_hang", "quantity_in_stock")) {
                statement.executeUpdate(
                        "ALTER TABLE mat_hang ADD COLUMN quantity_in_stock INT NOT NULL DEFAULT 0"
                );
                logger.info("Added inventory quantity; existing products start at zero and require stocktaking.");
            }
            if (columnExists(metadata, catalog, "mat_hang", "trang_thai")) {
                statement.executeUpdate("ALTER TABLE mat_hang DROP COLUMN trang_thai");
            }

            ensureColumn(statement, metadata, catalog, "chi_tiet_hoa_don", "gia_tai_thoi_diem",
                    "ALTER TABLE chi_tiet_hoa_don ADD COLUMN gia_tai_thoi_diem DOUBLE NULL");
            ensureColumn(statement, metadata, catalog, "chi_tiet_hoa_don", "gia_von_tai_thoi_diem",
                    "ALTER TABLE chi_tiet_hoa_don ADD COLUMN gia_von_tai_thoi_diem DOUBLE NULL");
            ensureColumn(statement, metadata, catalog, "chi_tiet_hoa_don", "ma_lo_hang",
                    "ALTER TABLE chi_tiet_hoa_don ADD COLUMN ma_lo_hang BIGINT NULL");
            ensureColumn(statement, metadata, catalog, "hoa_don", "diem_thuong_dung",
                    "ALTER TABLE hoa_don ADD COLUMN diem_thuong_dung INT NOT NULL DEFAULT 0");
            ensureColumn(statement, metadata, catalog, "hoa_don", "phuong_thuc_thanh_toan",
                    "ALTER TABLE hoa_don ADD COLUMN phuong_thuc_thanh_toan VARCHAR(20) NOT NULL DEFAULT 'TIEN_MAT'");
            ensureColumn(statement, metadata, catalog, "hoa_don", "tien_khach_tra",
                    "ALTER TABLE hoa_don ADD COLUMN tien_khach_tra DOUBLE NULL");
            ensureColumn(statement, metadata, catalog, "hoa_don", "tien_thua",
                    "ALTER TABLE hoa_don ADD COLUMN tien_thua DOUBLE NULL");
            ensureColumn(statement, metadata, catalog, "hoa_don", "ma_thu_ngan",
                    "ALTER TABLE hoa_don ADD COLUMN ma_thu_ngan BIGINT NULL");
            ensureIndex(statement, metadata, catalog, "chi_tiet_hoa_don", "idx_cthd_invoice",
                    "CREATE INDEX idx_cthd_invoice ON chi_tiet_hoa_don (ma_hoa_don)");
            dropUniqueIndex(statement, metadata, catalog, "chi_tiet_hoa_don", "uk_hoa_don_mat_hang");

            createPurchasingTables(statement);
            ensureColumn(statement, metadata, catalog, "nha_cung_cap", "da_xoa_luc",
                    "ALTER TABLE nha_cung_cap ADD COLUMN da_xoa_luc DATETIME NULL");
            ensureColumn(statement, metadata, catalog, "phieu_nhap", "ngay_giao_du_kien",
                    "ALTER TABLE phieu_nhap ADD COLUMN ngay_giao_du_kien DATE NULL");
            ensureColumn(statement, metadata, catalog, "phieu_nhap", "dieu_khoan_thanh_toan",
                    "ALTER TABLE phieu_nhap ADD COLUMN dieu_khoan_thanh_toan VARCHAR(500) NULL");
            ensureColumn(statement, metadata, catalog, "chi_tiet_phieu_nhap", "so_luong_da_nhan",
                    "ALTER TABLE chi_tiet_phieu_nhap ADD COLUMN so_luong_da_nhan INT NOT NULL DEFAULT 0");
            ensureIndex(statement, metadata, catalog, "lo_hang", "idx_lo_hang_fefo",
                    "CREATE INDEX idx_lo_hang_fefo ON lo_hang (ma_mat_hang, han_su_dung, ma_lo_hang)");
            ensureIndex(statement, metadata, catalog, "hoa_don", "idx_hoa_don_ngay",
                    "CREATE INDEX idx_hoa_don_ngay ON hoa_don (thoi_gian_thanh_toan)");

            statement.executeUpdate(
                    "UPDATE hoa_don SET diem_thuong_dung = 0 WHERE diem_thuong_dung IS NULL"
            );
            statement.executeUpdate(
                    "UPDATE hoa_don SET phuong_thuc_thanh_toan = 'TIEN_MAT' "
                            + "WHERE phuong_thuc_thanh_toan IS NULL OR phuong_thuc_thanh_toan = ''"
            );
            statement.executeUpdate(
                    "UPDATE tai_khoan SET vai_tro = 'CASHIER' WHERE vai_tro = 'NHAN_VIEN'"
            );
            statement.executeUpdate(
                    "UPDATE tai_khoan SET ho_ten = ten_dang_nhap WHERE ho_ten IS NULL OR ho_ten = ''"
            );
            statement.executeUpdate(
                    "UPDATE khach_hang SET loai_khach_hang = 'KHACH_HANG_THONG_THUONG' "
                            + "WHERE loai_khach_hang = 'MUA_LE'"
            );
            statement.executeUpdate(
                    "UPDATE khach_hang SET loai_khach_hang = 'KHACH_HANG_THANH_VIEN' "
                            + "WHERE loai_khach_hang = 'MUA_BUON'"
            );
        }
    }

    private void createPurchasingTables(Statement statement) throws SQLException {
        List<String> ddl = List.of(
                """
                CREATE TABLE IF NOT EXISTS nha_cung_cap (
                    ma_nha_cung_cap BIGINT NOT NULL AUTO_INCREMENT,
                    ten_nha_cung_cap VARCHAR(150) NOT NULL,
                    so_dien_thoai VARCHAR(30),
                    email VARCHAR(150),
                    dia_chi VARCHAR(255),
                    PRIMARY KEY (ma_nha_cung_cap)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS de_xuat_nha_cung_cap (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    ten_nha_cung_cap VARCHAR(150) NOT NULL,
                    so_dien_thoai VARCHAR(30),
                    email VARCHAR(150),
                    dia_chi VARCHAR(255),
                    nguoi_de_xuat VARCHAR(50) NOT NULL,
                    thoi_gian_de_xuat DATETIME NOT NULL,
                    trang_thai VARCHAR(20) NOT NULL,
                    nguoi_xu_ly VARCHAR(50),
                    thoi_gian_xu_ly DATETIME,
                    PRIMARY KEY (id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS phieu_nhap (
                    ma_phieu_nhap BIGINT NOT NULL AUTO_INCREMENT,
                    ma_nha_cung_cap BIGINT NOT NULL,
                    ma_nguoi_tao BIGINT NOT NULL,
                    ngay_tao DATETIME NOT NULL,
                    ngay_nhan DATETIME NULL,
                    trang_thai VARCHAR(30) NOT NULL,
                    tong_tien_nhap DECIMAL(18,2) NOT NULL,
                    ngay_giao_du_kien DATE NULL,
                    dieu_khoan_thanh_toan VARCHAR(500) NULL,
                    PRIMARY KEY (ma_phieu_nhap),
                    CONSTRAINT fk_phieu_nhap_ncc FOREIGN KEY (ma_nha_cung_cap)
                        REFERENCES nha_cung_cap(ma_nha_cung_cap),
                    CONSTRAINT fk_phieu_nhap_user FOREIGN KEY (ma_nguoi_tao)
                        REFERENCES tai_khoan(id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS chi_tiet_phieu_nhap (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    ma_phieu_nhap BIGINT NOT NULL,
                    ma_mat_hang BIGINT NOT NULL,
                    so_luong INT NOT NULL,
                    so_luong_da_nhan INT NOT NULL DEFAULT 0,
                    gia_nhap DECIMAL(18,2) NOT NULL,
                    ngay_san_xuat DATE NULL,
                    han_su_dung DATE NULL,
                    PRIMARY KEY (id),
                    CONSTRAINT fk_ctpn_phieu FOREIGN KEY (ma_phieu_nhap)
                        REFERENCES phieu_nhap(ma_phieu_nhap),
                    CONSTRAINT fk_ctpn_product FOREIGN KEY (ma_mat_hang)
                        REFERENCES mat_hang(ma_mat_hang)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS lo_hang (
                    ma_lo_hang BIGINT NOT NULL AUTO_INCREMENT,
                    ma_mat_hang BIGINT NOT NULL,
                    ma_phieu_nhap BIGINT NOT NULL,
                    ngay_san_xuat DATE NULL,
                    han_su_dung DATE NULL,
                    gia_nhap DECIMAL(18,2) NOT NULL,
                    so_luong_ban_dau INT NOT NULL,
                    so_luong_con_lai INT NOT NULL,
                    PRIMARY KEY (ma_lo_hang),
                    CONSTRAINT fk_lo_hang_product FOREIGN KEY (ma_mat_hang)
                        REFERENCES mat_hang(ma_mat_hang),
                    CONSTRAINT fk_lo_hang_receipt FOREIGN KEY (ma_phieu_nhap)
                        REFERENCES phieu_nhap(ma_phieu_nhap),
                    CONSTRAINT ck_lo_hang_quantity CHECK (
                        so_luong_ban_dau >= 0 AND so_luong_con_lai >= 0
                    )
                )
                """
        );
        for (String createTable : ddl) {
            statement.executeUpdate(createTable);
        }
    }

    private void ensureResetTokenExpiryPrecision(
            Statement statement, DatabaseMetaData metadata, String catalog
    ) throws SQLException {
        try (ResultSet columns = metadata.getColumns(catalog, null, "password_reset_token", "expires_at")) {
            if (!columns.next()) {
                throw new IllegalStateException("Password reset expiry column does not exist.");
            }
            if (columns.getInt("DECIMAL_DIGITS") < 6) {
                statement.executeUpdate(
                        "ALTER TABLE password_reset_token MODIFY COLUMN expires_at DATETIME(6) NOT NULL"
                );
            }
        }
    }

    private void ensureColumn(
            Statement statement,
            DatabaseMetaData metadata,
            String catalog,
            String table,
            String column,
            String ddl
    ) throws SQLException {
        if (!columnExists(metadata, catalog, table, column)) {
            statement.executeUpdate(ddl);
        }
    }

    private void ensureIndex(
            Statement statement,
            DatabaseMetaData metadata,
            String catalog,
            String table,
            String index,
            String ddl
    ) throws SQLException {
        try (ResultSet indexes = metadata.getIndexInfo(catalog, null, table, false, false)) {
            while (indexes.next()) {
                if (index.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return;
                }
            }
        }
        statement.executeUpdate(ddl);
    }

    private void ensureForeignKey(
            Statement statement,
            DatabaseMetaData metadata,
            String catalog,
            String table,
            String foreignKey,
            String ddl
    ) throws SQLException {
        try (ResultSet keys = metadata.getImportedKeys(catalog, null, table)) {
            while (keys.next()) {
                if (foreignKey.equalsIgnoreCase(keys.getString("FK_NAME"))) {
                    return;
                }
            }
        }
        statement.executeUpdate(ddl);
    }

    private void dropUniqueIndex(
            Statement statement,
            DatabaseMetaData metadata,
            String catalog,
            String table,
            String index
    ) throws SQLException {
        try (ResultSet indexes = metadata.getIndexInfo(catalog, null, table, false, false)) {
            while (indexes.next()) {
                if (index.equalsIgnoreCase(indexes.getString("INDEX_NAME"))
                        && !indexes.getBoolean("NON_UNIQUE")) {
                    statement.executeUpdate("ALTER TABLE " + table + " DROP INDEX " + index);
                    return;
                }
            }
        }
    }

    private void requireTable(DatabaseMetaData metadata, String catalog, String table) throws SQLException {
        try (ResultSet tables = metadata.getTables(catalog, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                if (table.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return;
                }
            }
        }
        throw new IllegalStateException(
                "Required table '" + table + "' does not exist in the configured database."
        );
    }

    private boolean columnExists(
            DatabaseMetaData metadata,
            String catalog,
            String table,
            String column
    ) throws SQLException {
        try (ResultSet columns = metadata.getColumns(catalog, null, "%", "%")) {
            while (columns.next()) {
                if (table.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                        && column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
