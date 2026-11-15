package com.app.repositories;

import com.app.models.PickupClaims;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PickupClaimsRepository extends JpaRepository<PickupClaims, UUID> {

    // Locks the claim row so two volunteers can't accept the same delivery at once.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PickupClaims c where c.claimId = :claimId")
    Optional<PickupClaims> findByIdForUpdate(@Param("claimId") UUID claimId);
}