package com.app.services;

import com.app.models.AppUsers;
import com.app.models.FoodListings;
import com.app.models.Organization;
import com.app.models.PickupClaims;
import com.app.notifications.NotificationEvent;
import com.app.repositories.AppUsersRepository;
import com.app.repositories.FoodListingsRepository;
import com.app.repositories.OrganizationRepository;
import com.app.repositories.PickupClaimsRepository;
import org.springframework.context.ApplicationEventPublisher;
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
    private final AppUsersRepository appUsersRepository;
    private final ApplicationEventPublisher events;

    public PickupClaimsServices(PickupClaimsRepository repository,
            FoodListingsRepository foodListingsRepository,
            OrganizationRepository organizationRepository,
            AppUsersRepository appUsersRepository,
            ApplicationEventPublisher events) {
        this.repository = repository;
        this.foodListingsRepository = foodListingsRepository;
        this.organizationRepository = organizationRepository;
        this.appUsersRepository = appUsersRepository;
        this.events = events;
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
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown recipient organization"));
        if (!"RECIPIENT".equals(recipient.getOrganizationType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only recipient organizations can claim food");
        }

        PickupClaims claim = new PickupClaims();
        claim.setListing(listing);
        claim.setRecipientOrganization(recipient);
        listing.setStatus("CLAIMED");
        PickupClaims saved = repository.save(claim);

        events.publishEvent(new NotificationEvent(
                "LISTING_CLAIMED",
                "Food claimed: " + listing.getTitle(),
                recipient.getName() + " claimed " + listing.getTitle() + " from "
                        + listing.getDonorOrganization().getName() + ". A volunteer can now accept the delivery."));
        return saved;
    }

    @Transactional
    public PickupClaims acceptDelivery(UUID claimId, UUID volunteerUserId) {
        PickupClaims claim = lockClaim(claimId);
        AppUsers volunteer = findVolunteer(volunteerUserId);

        if (!"CLAIMED".equals(claim.getStatus()) || claim.getVolunteerUser() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This delivery has already been taken");
        }

        claim.setVolunteerUser(volunteer);

        events.publishEvent(deliveryEvent("DELIVERY_ACCEPTED", "Volunteer assigned", claim,
                volunteer.getFullName() + " will deliver"));
        return claim;
    }

    @Transactional
    public PickupClaims markPickedUp(UUID claimId, UUID volunteerUserId) {
        PickupClaims claim = lockClaim(claimId);
        requireAssignedVolunteer(claim, volunteerUserId);

        if (!"CLAIMED".equals(claim.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only claimed deliveries can be picked up");
        }

        claim.setStatus("IN_TRANSIT");
        claim.setPickedUpAt(OffsetDateTime.now());
        claim.getListing().setStatus("PICKED_UP");

        events.publishEvent(deliveryEvent("DELIVERY_PICKED_UP", "Food on the way", claim,
                claim.getVolunteerUser().getFullName() + " picked up"));
        return claim;
    }

    @Transactional
    public PickupClaims markDelivered(UUID claimId, UUID volunteerUserId) {
        PickupClaims claim = lockClaim(claimId);
        requireAssignedVolunteer(claim, volunteerUserId);

        if (!"IN_TRANSIT".equals(claim.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Food must be picked up before it can be delivered");
        }

        claim.setStatus("DELIVERED");
        claim.setDeliveredAt(OffsetDateTime.now());

        events.publishEvent(deliveryEvent("DELIVERY_COMPLETED", "Food delivered", claim,
                claim.getVolunteerUser().getFullName() + " delivered"));
        return claim;
    }

    // e.g. "Priya Patel picked up Pasta (48 portions) from Green Harvest Co. for Mission House Shelter."
    private NotificationEvent deliveryEvent(String eventType, String subjectPrefix, PickupClaims claim, String action) {
        FoodListings listing = claim.getListing();
        String message = action + " " + listing.getTitle() + " (" + listing.getQuantityLabel() + ") from "
                + listing.getDonorOrganization().getName() + " for " + claim.getRecipientOrganization().getName() + ".";
        return new NotificationEvent(eventType, subjectPrefix + ": " + listing.getTitle(), message);
    }

    private PickupClaims lockClaim(UUID claimId) {
        return repository.findByIdForUpdate(claimId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));
    }

    private AppUsers findVolunteer(UUID volunteerUserId) {
        if (volunteerUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "volunteerUserId is required");
        }
        AppUsers volunteer = appUsersRepository.findById(volunteerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown volunteer"));
        if (!"VOLUNTEER".equals(volunteer.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only volunteers can deliver food");
        }
        return volunteer;
    }

    private void requireAssignedVolunteer(PickupClaims claim, UUID volunteerUserId) {
        AppUsers assigned = claim.getVolunteerUser();
        if (assigned == null || !assigned.getUserId().equals(volunteerUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the assigned volunteer can update this delivery");
        }
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
