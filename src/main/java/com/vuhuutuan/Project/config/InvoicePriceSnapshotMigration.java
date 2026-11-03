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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InvoicePriceSnapshotMigration implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(InvoicePriceSnapshotMigration.class);
    private static final String TABLE_NAME = "chi_tiet_hoa_don";
    private static final String COLUMN_NAME = "gia_tai_thoi_diem";

    private final DataSource dataSource;

    public InvoicePriceSnapshotMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            if (!tableExists(metadata, connection.getCatalog())) {
                throw new IllegalStateException(
                        "Không tìm thấy bảng chi_tiet_hoa_don trong database. "
                                + "Hãy tạo các bảng hóa đơn theo cấu hình hiện có trước khi chạy ứng dụng."
                );
            }
            if (!columnExists(metadata, connection.getCatalog())) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(
                            "ALTER TABLE chi_tiet_hoa_don ADD COLUMN gia_tai_thoi_diem DOUBLE NULL"
                    );
                }
                logger.info("Added invoice line price snapshot column to chi_tiet_hoa_don.");
            }
        }
    }

    private boolean tableExists(DatabaseMetaData metadata, String catalog) throws SQLException {
        try (ResultSet tables = metadata.getTables(catalog, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                if (TABLE_NAME.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean columnExists(DatabaseMetaData metadata, String catalog) throws SQLException {
        try (ResultSet columns = metadata.getColumns(catalog, null, "%", "%")) {
            while (columns.next()) {
                if (TABLE_NAME.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                        && COLUMN_NAME.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
