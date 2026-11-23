package com.vuhuutuan.Project.repository;

import com.vuhuutuan.Project.model.HoaDon;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Long> {
    java.util.List<HoaDon> findAllByOrderByThoiGianThanhToanDesc();

    @Query(
            value = """
                    select h from HoaDon h left join h.khachHang k
                    where ((:invoiceId is null and :customerName is null)
                        or (:invoiceId is not null and h.maHoaDon = :invoiceId)
                        or (:customerName is not null and lower(k.hoTen) like :customerName))
                    and (:fromTime is null or h.thoiGianThanhToan >= :fromTime)
                    and (:untilTime is null or h.thoiGianThanhToan < :untilTime)
                    and (:cancelled is null or h.cancelled = :cancelled)
                    """,
            countQuery = """
                    select count(h) from HoaDon h left join h.khachHang k
                    where ((:invoiceId is null and :customerName is null)
                        or (:invoiceId is not null and h.maHoaDon = :invoiceId)
                        or (:customerName is not null and lower(k.hoTen) like :customerName))
                    and (:fromTime is null or h.thoiGianThanhToan >= :fromTime)
                    and (:untilTime is null or h.thoiGianThanhToan < :untilTime)
                    and (:cancelled is null or h.cancelled = :cancelled)
                    """
    )
    Page<HoaDon> searchHistory(
            @Param("invoiceId") Long invoiceId,
            @Param("customerName") String customerName,
            @Param("fromTime") java.time.LocalDateTime fromTime,
            @Param("untilTime") java.time.LocalDateTime untilTime,
            @Param("cancelled") Boolean cancelled,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HoaDon h where h.maHoaDon = :id")
    java.util.Optional<HoaDon> findByIdForUpdate(@Param("id") Long id);
}