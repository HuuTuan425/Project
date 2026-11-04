package com.vuhuutuan.Project.repository;

import com.vuhuutuan.Project.model.SupplierProposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierProposalRepository extends JpaRepository<SupplierProposal, Long> {
    List<SupplierProposal> findAllByOrderByRequestedAtDesc();

    List<SupplierProposal> findByRequestedByOrderByRequestedAtDesc(String requestedBy);
}
