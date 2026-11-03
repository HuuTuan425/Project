CREATE TABLE IF NOT EXISTS tai_khoan (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ten_dang_nhap VARCHAR(50) NOT NULL,
    mat_khau VARCHAR(100) NOT NULL,
    vai_tro VARCHAR(20) NOT NULL,
    dang_hoat_dong BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_tai_khoan_ten_dang_nhap UNIQUE (ten_dang_nhap)
);
