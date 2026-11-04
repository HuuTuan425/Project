CREATE TABLE IF NOT EXISTS tai_khoan (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ten_dang_nhap VARCHAR(50) NOT NULL,
    mat_khau VARCHAR(100) NOT NULL,
    vai_tro VARCHAR(20) NOT NULL,
    dang_hoat_dong BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_tai_khoan_ten_dang_nhap UNIQUE (ten_dang_nhap)
);

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
);
