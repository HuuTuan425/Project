package com.vuhuutuan.Project.repository;

import com.vuhuutuan.Project.model.AppUser;
import com.vuhuutuan.Project.model.AccountRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsernameIgnoreCase(String username);

    List<AppUser> findAllByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    List<AppUser> findAllByDeletedAtIsNullOrderByUsernameAsc();

    @Query(
            value = """
                    select u from AppUser u
                    where (:search is null
                        or lower(u.username) like :search
                        or lower(coalesce(u.fullName, '')) like :search
                        or lower(coalesce(u.phone, '')) like :search
                        or lower(coalesce(u.email, '')) like :search)
                    and (:role is null or u.role = :role)
                    and (:enabled is null or u.enabled = :enabled)
                    and ((:deleted = true and u.deletedAt is not null)
                        or (:deleted = false and u.deletedAt is null))
                    """,
            countQuery = """
                    select count(u) from AppUser u
                    where (:search is null
                        or lower(u.username) like :search
                        or lower(coalesce(u.fullName, '')) like :search
                        or lower(coalesce(u.phone, '')) like :search
                        or lower(coalesce(u.email, '')) like :search)
                    and (:role is null or u.role = :role)
                    and (:enabled is null or u.enabled = :enabled)
                    and ((:deleted = true and u.deletedAt is not null)
                        or (:deleted = false and u.deletedAt is null))
                    """
    )
    Page<AppUser> searchAccounts(
            @Param("search") String search,
            @Param("role") AccountRole role,
            @Param("enabled") Boolean enabled,
            @Param("deleted") boolean deleted,
            Pageable pageable
    );

    long countByRoleAndEnabledTrue(AccountRole role);
}
