package com.app.services;

import com.app.models.FoodListings;
import com.app.models.Organization;
import com.app.models.PickupClaims;
import com.app.repositories.FoodListingsRepository;
import com.app.repositories.OrganizationRepository;
import com.app.repositories.PickupClaimsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PickupClaimsServices {

    private final PickupClaimsRepository repository;
    private final FoodListingsRepository foodListingsRepository;
    private final OrganizationRepository organizationRepository;

    public PickupClaimsServices(PickupClaimsRepository repository,
                                FoodListingsRepository foodListingsRepository,
                                OrganizationRepository organizationRepository) {
        this.repository = repository;
        this.foodListingsRepository = foodListingsRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public PickupClaims claimListing(UUID listingId, UUID recipientOrganizationId) {
        FoodListings listing = foodListingsRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));

        if (!"AVAILABLE".equals(listing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This listing has already been claimed");
        }
        if (listing.getPickupWindowEndsAt().isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The pickup window for this listing has closed");
        }
        if (recipientOrganizationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipientOrganizationId is required");
        }

        Organization recipient = organizationRepository.findById(recipientOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown recipient organization"));
        if (!"RECIPIENT".equals(recipient.getOrganizationType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only recipient organizations can claim food");
        }
        
        PickupClaims claim = new PickupClaims();
        claim.setListing(listing);
        claim.setRecipientOrganization(recipient);
        listing.setStatus("CLAIMED");

        return repository.save(claim);
    }

    public List<PickupClaims> findAll() {
        return repository.findAll();
    }

    public Optional<PickupClaims> findById(UUID claimId) {
        return repository.findById(claimId);
    }

    public PickupClaims create(PickupClaims pickupClaims) {
        return repository.save(pickupClaims);
    }

    public PickupClaims update(UUID claimId, PickupClaims pickupClaims) {
        pickupClaims.setClaimId(claimId);
        return repository.save(pickupClaims);
    }

    public void delete(UUID claimId) {
        repository.deleteById(claimId);
    }
}
