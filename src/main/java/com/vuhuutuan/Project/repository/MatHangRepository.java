package com.vuhuutuan.Project.repository;

import com.vuhuutuan.Project.model.MatHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;

@Repository
public interface MatHangRepository extends JpaRepository<MatHang, Long> {
    List<MatHang> findByDeletedAtIsNullOrderByTenMatHangAsc();

    List<MatHang> findByDeletedAtIsNullAndTenMatHangContainingIgnoreCaseOrderByTenMatHangAsc(String query);

    java.util.Optional<MatHang> findByMaMatHangAndDeletedAtIsNull(Long maMatHang);

    @Query("""
            select m from MatHang m
            where m.deletedAt is null
            and (:productId is null or m.maMatHang = :productId)
            and (:query is null
                or lower(m.tenMatHang) like :query
                or lower(coalesce(m.hangSanXuat, '')) like :query)
            order by m.maMatHang
            """)
    List<MatHang> searchProducts(
            @Param("productId") Long productId,
            @Param("query") String query
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MatHang m where m.maMatHang = :id")
    java.util.Optional<MatHang> findByIdForUpdate(@Param("id") Long id);
}