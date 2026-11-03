package com.vuhuutuan.Project.repository;

import com.vuhuutuan.Project.model.ProductBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from ProductBatch b where b.id = :id")
    java.util.Optional<ProductBatch> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from ProductBatch b
            where b.product.maMatHang = :productId
              and b.remainingQuantity > 0
              and (b.expiryDate is null or b.expiryDate >= :today)
            order by case when b.expiryDate is null then 1 else 0 end, b.expiryDate, b.id
            """)
    List<ProductBatch> findSellableBatchesForUpdate(
            @Param("productId") Long productId,
            @Param("today") LocalDate today
    );

    @Query("""
            select count(b) > 0 from ProductBatch b
            where b.product.maMatHang = :productId
              and b.remainingQuantity > 0
              and (b.expiryDate is null or b.expiryDate >= :today)
            """)
    boolean hasSellableStock(@Param("productId") Long productId, @Param("today") LocalDate today);
}
