package com.app.repositories;

import com.app.models.FoodListings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FoodListingsRepository extends JpaRepository<FoodListings, UUID> {

    // Locks the listing row (SELECT ... FOR UPDATE) until the transaction ends, so two
    // shelters clicking "Pickup" at the same moment can't both claim the same food.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from FoodListings l where l.listingId = :listingId")
    Optional<FoodListings> findByIdForUpdate(@Param("listingId") UUID listingId);
}