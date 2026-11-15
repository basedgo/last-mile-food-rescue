package com.app.services;

import com.app.models.FoodListings;
import com.app.models.Organization;
import com.app.repositories.FoodListingsRepository;
import com.app.repositories.OrganizationRepository;
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

    public FoodListingsServices(FoodListingsRepository repository, OrganizationRepository organizationRepository) {
        this.repository = repository;
        this.organizationRepository = organizationRepository;
    }

    public List<FoodListings> findAll() {
        return repository.findAll();
    }

    public Optional<FoodListings> findById(UUID listingId) {
        return repository.findById(listingId);
    }

        public FoodListings create(FoodListings foodListings) {
            foodListings.setDonorOrganization(findDonor(foodListings.getDonorOrganizationId()));
            return repository.save(foodListings);
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
