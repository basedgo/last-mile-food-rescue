import {
  appUsersApi,
  claimListing,
  foodListingsApi,
  listingTagsApi,
  organizationsApi,
  pickupClaimsApi,
} from "./endpoints";

import type {
  AppUser,
  FoodListingPayload,
  Organization,
  PickupClaim,
} from "../types/api";

import type { Listing } from "../types/view";
import {
  groupTagsByListingId,
  latestClaimByListingId,
  mapFoodListingToViewListing,
} from "../mappers/listings";

export async function getListingsForUi(): Promise<{
  listings: Listing[];
  organizations: Organization[];
  claims: PickupClaim[];
  volunteers: AppUser[];
}> {
  const [foodListings, organizations, listingTags, pickupClaims, appUsers] =
    await Promise.all([
      foodListingsApi.list(),
      organizationsApi.list(),
      listingTagsApi.list().catch((error: unknown) => {
        console.warn("Unable to load listing tags; continuing without tags.", error);
        return [];
      }),
      pickupClaimsApi.list(),
      appUsersApi.list(),
    ]);

  const tagsByListingId = groupTagsByListingId(listingTags);
  const claimsByListingId = latestClaimByListingId(pickupClaims);
  const listings = foodListings.map((listing, index) =>
    mapFoodListingToViewListing(
      listing,
      index,
      organizations,
      tagsByListingId,
      claimsByListingId,
    ),
  );

  const volunteers = appUsers.filter((user) => user.role === "VOLUNTEER");

  return { listings, organizations, claims: pickupClaims, volunteers };
}

export async function createFoodListingFromUi(
  title: string,
  donor: Organization,
): Promise<Listing> {
  const now = new Date();
  const endsAt = new Date(now.getTime() + 75 * 60 * 1000);
  const payload: FoodListingPayload = {
    donorOrganizationId: donor.organizationId,
    title: title || "Fresh surplus pickup",
    category: "Prepared meals",
    description: "Posted from the frontend surplus food form.",
    quantityLabel: "24 portions",
    servingCount: 24,
    pickupInstructions: "Use loading dock B and ask for the shift lead.",
    pickupWindowStartsAt: now.toISOString(),
    pickupWindowEndsAt: endsAt.toISOString(),
    status: "AVAILABLE",
    timeTone: "SOON",
    distanceLabel: "0.2 mi",
    mapPositionX: 34,
    mapPositionY: 42,
  };

  const createdListing = await foodListingsApi.create(payload);
  return mapFoodListingToViewListing(createdListing, 0, [donor], new Map(), new Map());
}

export async function claimListingFromUi(
  listing: Listing,
  recipient: Organization,
): Promise<{ listing: Listing; claim: PickupClaim }> {
  const claim = await claimListing(listing.id, recipient.organizationId);
  // Return an updated copy of the listing instead of changing the original (React needs new objects to re-render).
  const claimedListing: Listing = {
    ...listing,
    status: "CLAIMED",
    claimedByOrganizationId: claim.recipientOrganizationId,
    pickedUpBy: recipient.name,
    datePickedUp: formatClaimDate(claim.claimedAt),
  };
  return { listing: claimedListing, claim };
}

function formatClaimDate(value: string) {
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  }).format(new Date(value));
}