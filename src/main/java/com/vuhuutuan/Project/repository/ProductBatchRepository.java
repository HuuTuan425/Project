package com.vuhuutuan.Project.repository;

import com.vuhuutuan.Project.model.ProductBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
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

    interface NearestExpiry {
        Long getProductId();
        LocalDate getExpiryDate();
    }

    @Query("""
            select b.product.maMatHang as productId, min(b.expiryDate) as expiryDate
            from ProductBatch b
            where b.product.maMatHang in :productIds and b.remainingQuantity > 0
              and b.expiryDate is not null
            group by b.product.maMatHang
            """)
    List<NearestExpiry> findNearestExpiryDates(@Param("productIds") Collection<Long> productIds);

    @Query("""
            select distinct b.product.maMatHang from ProductBatch b
            where b.remainingQuantity > 0 and b.expiryDate < :today
            order by b.product.maMatHang
            """)
    List<Long> findProductIdsWithExpiredStock(@Param("today") LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from ProductBatch b
            where b.product.maMatHang = :productId
              and b.remainingQuantity > 0 and b.expiryDate < :today
            order by b.id
            """)
    List<ProductBatch> findExpiredBatchesForUpdate(
            @Param("productId") Long productId,
            @Param("today") LocalDate today
    );
}
