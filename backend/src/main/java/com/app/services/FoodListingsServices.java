package com.app.services;

import com.app.models.FoodListings;
import com.app.models.Organization;
import com.app.notifications.NotificationEvent;
import com.app.repositories.FoodListingsRepository;
import com.app.repositories.OrganizationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FoodListingsServices {

    private final FoodListingsRepository repository;
    private final OrganizationRepository organizationRepository;
    private final ApplicationEventPublisher events;

    public FoodListingsServices(FoodListingsRepository repository,
                                OrganizationRepository organizationRepository,
                                ApplicationEventPublisher events) {
        this.repository = repository;
        this.organizationRepository = organizationRepository;
        this.events = events;
    }

    public List<FoodListings> findAll() {
        return repository.findAll();
    }

    public Optional<FoodListings> findById(UUID listingId) {
        return repository.findById(listingId);
    }

        public FoodListings create(FoodListings foodListings) {
            Organization donor = findDonor(foodListings.getDonorOrganizationId());
            foodListings.setDonorOrganization(donor);
            FoodListings saved = repository.save(foodListings);

            events.publishEvent(new NotificationEvent(
                    "LISTING_POSTED",
                    "New food available: " + saved.getTitle(),
                    donor.getName() + " posted " + saved.getTitle() + " (" + saved.getQuantityLabel() + ")."
                            + " Claim it in Last Mile Food Rescue before the pickup window closes."));
            return saved;
        }

        private Organization findDonor(UUID donorOrganizationId) {
            if (donorOrganizationId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "donorOrganizationId is required");
            }
            Organization donor = organizationRepository.findById(donorOrganizationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown donor organization"));
            if (!"DONOR".equals(donor.getOrganizationType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization is not a donor");
            }
            return donor;
        }

    public FoodListings update(UUID listingId, FoodListings foodListings) {
        foodListings.setListingId(listingId);
        return repository.save(foodListings);
    }

    public void delete(UUID listingId) {
        repository.deleteById(listingId);
    }
}
