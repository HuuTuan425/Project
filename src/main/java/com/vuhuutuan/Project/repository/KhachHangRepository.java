package com.vuhuutuan.Project.repository;

import com.vuhuutuan.Project.model.KhachHang;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Long> {
    @Query(
            value = """
                    select k from KhachHang k
                    where (:search is null
                        or lower(k.hoTen) like :search
                        or lower(k.soCMND) like :search
                        or lower(k.soDienThoai) like :search)
                    """,
            countQuery = """
                    select count(k) from KhachHang k
                    where (:search is null
                        or lower(k.hoTen) like :search
                        or lower(k.soCMND) like :search
                        or lower(k.soDienThoai) like :search)
                    """
    )
    Page<KhachHang> searchCustomers(@Param("search") String search, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from KhachHang k where k.maKhachHang = :id")
    java.util.Optional<KhachHang> findByIdForUpdate(@Param("id") Long id);
}
