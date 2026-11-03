package com.vuhuutuan.Project.repository;

import com.vuhuutuan.Project.model.PurchaseOrder;
import com.vuhuutuan.Project.model.PurchaseOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

    @Query(
            value = """
                    select p from PurchaseOrder p left join fetch p.supplier s
                    where ((:purchaseOrderId is null and :supplierName is null)
                        or (:purchaseOrderId is not null and p.id = :purchaseOrderId)
                        or (:supplierName is not null and lower(s.name) like :supplierName))
                    and (:fromTime is null or p.createdAt >= :fromTime)
                    and (:untilTime is null or p.createdAt < :untilTime)
                    and (:status is null or p.status = :status)
                    """,
            countQuery = """
                    select count(p) from PurchaseOrder p left join p.supplier s
                    where ((:purchaseOrderId is null and :supplierName is null)
                        or (:purchaseOrderId is not null and p.id = :purchaseOrderId)
                        or (:supplierName is not null and lower(s.name) like :supplierName))
                    and (:fromTime is null or p.createdAt >= :fromTime)
                    and (:untilTime is null or p.createdAt < :untilTime)
                    and (:status is null or p.status = :status)
                    """
    )
    Page<PurchaseOrder> searchHistory(
            @Param("purchaseOrderId") Long purchaseOrderId,
            @Param("supplierName") String supplierName,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("untilTime") LocalDateTime untilTime,
            @Param("status") PurchaseOrderStatus status,
            Pageable pageable
    );

    boolean existsBySupplier_Id(Long supplierId);

    boolean existsByCreatedBy_Id(Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PurchaseOrder p where p.id = :id")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("id") Long id);
}
