package com.app.controllers;

import com.app.dto.ClaimListingRequest;
import com.app.models.FoodListings;
import com.app.models.PickupClaims;
import com.app.services.FoodListingsServices;
import com.app.services.PickupClaimsServices;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/food-listings")
public class FoodListingsController {

    private final FoodListingsServices services;
    private final PickupClaimsServices pickupClaimsServices;

    public FoodListingsController(FoodListingsServices services, PickupClaimsServices pickupClaimsServices) {
        this.services = services;
        this.pickupClaimsServices = pickupClaimsServices;
    }

    @PostMapping("/{listingId}/claims")
    public ResponseEntity<PickupClaims> claim(@PathVariable UUID listingId, @RequestBody ClaimListingRequest request) {
        PickupClaims claim = pickupClaimsServices.claimListing(listingId, request.recipientOrganizationId());
        return ResponseEntity.created(URI.create("/api/pickup-claims/" + claim.getClaimId())).body(claim);
    }

    @GetMapping
    public List<FoodListings> list() {
        return services.findAll();
    }

    @GetMapping("/{listingId}")
    public ResponseEntity<FoodListings> get(@PathVariable UUID listingId) {
        return services.findById(listingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FoodListings> create(@RequestBody FoodListings foodListings) {
        FoodListings saved = services.create(foodListings);
        return ResponseEntity.created(URI.create("/api/food-listings/" + saved.getListingId())).body(saved);
    }

    @PutMapping("/{listingId}")
    public ResponseEntity<FoodListings> update(@PathVariable UUID listingId, @RequestBody FoodListings foodListings) {
        if (!services.findById(listingId).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        FoodListings saved = services.update(listingId, foodListings);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{listingId}")
    public ResponseEntity<Void> delete(@PathVariable UUID listingId) {
        services.delete(listingId);
        return ResponseEntity.noContent().build();
    }
}
