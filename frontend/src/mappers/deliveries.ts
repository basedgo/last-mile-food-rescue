import type { Organization, PickupClaim } from "../types/api";
import type { Delivery, Listing } from "../types/view";

export function mapClaimsToDeliveries(
  claims: PickupClaim[],
  listings: Listing[],
  organizations: Organization[],
): Delivery[] {
  const listingsById = new Map(listings.map((listing) => [listing.id, listing]));
  const organizationsById = new Map(
    organizations.map((organization) => [organization.organizationId, organization]),
  );

  return claims
    .filter((claim) => claim.status !== "CANCELLED")
    .map((claim) => {
      const listing = listingsById.get(claim.listingId);
      return {
        claimId: claim.claimId,
        listingTitle: listing?.title ?? "Unknown listing",
        portions: listing?.portions ?? "",
        restaurant: listing?.restaurant ?? "Unknown donor",
        shelter:
          organizationsById.get(claim.recipientOrganizationId)?.name ?? "Unknown shelter",
        status: claim.status,
        volunteerUserId: claim.volunteerUserId ?? null,
      };
    });
}