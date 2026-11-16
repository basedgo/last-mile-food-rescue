import type { AppUser, Organization, PickupClaim } from "../types/api";
import type { Delivery, Listing } from "../types/view";
import { toLatLng } from "./listings";

export function mapClaimsToDeliveries(
  claims: PickupClaim[],
  listings: Listing[],
  organizations: Organization[],
  volunteers: AppUser[],
): Delivery[] {
  const listingsById = new Map(listings.map((listing) => [listing.id, listing]));
  const organizationsById = new Map(
    organizations.map((organization) => [organization.organizationId, organization]),
  );
  const volunteersById = new Map(volunteers.map((volunteer) => [volunteer.userId, volunteer]));

  return claims
    .filter((claim) => claim.status !== "CANCELLED")
    .map((claim) => {
      const listing = listingsById.get(claim.listingId);
      const shelter = organizationsById.get(claim.recipientOrganizationId);
      const volunteer = claim.volunteerUserId
        ? volunteersById.get(claim.volunteerUserId)
        : undefined;
      return {
        claimId: claim.claimId,
        listingTitle: listing?.title ?? "Unknown listing",
        portions: listing?.portions ?? "",
        restaurant: listing?.restaurant ?? "Unknown donor",
        shelter: shelter?.name ?? "Unknown shelter",
        status: claim.status,
        volunteerUserId: claim.volunteerUserId ?? null,
        volunteerName: volunteer?.fullName ?? "",
        restaurantLocation: listing?.location ?? null,
        shelterLocation: toLatLng(shelter?.latitude, shelter?.longitude),
      };
    });
}