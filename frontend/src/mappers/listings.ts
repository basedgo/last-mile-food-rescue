import type {
  FoodListing,
  ListingTag,
  Organization,
  PickupClaim,
} from "../types/api";
import type { Listing } from "../types/view";

function toNumber(value: number | string | null | undefined, fallback: number) {
  if (typeof value === "number") return value;
  if (typeof value === "string") {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return fallback;
}

function formatPickupTime(endTime: string) {
  const endsAt = new Date(endTime).getTime();
  const diffMs = endsAt - Date.now();
  const absoluteMinutes = Math.max(0, Math.round(diffMs / 60000));

  if (absoluteMinutes < 60) return `${absoluteMinutes} min`;

  const hours = Math.floor(absoluteMinutes / 60);
  const minutes = absoluteMinutes % 60;
  return minutes === 0 ? `${hours}h` : `${hours}h ${minutes}m`;
}

function formatDate(value: string | null | undefined) {
  if (!value) return "Not claimed yet";
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  }).format(new Date(value));
}

export function mapFoodListingToViewListing(
  listing: FoodListing,
  index: number,
  organizations: Organization[],
  tagsByListingId: Map<string, string[]>,
  claimsByListingId: Map<string, PickupClaim>,
): Listing {
  // Look organizations up by their real IDs from the API.
  const organizationsById = new Map(
    organizations.map((organization) => [organization.organizationId, organization]),
  );
  const donor = listing.donorOrganizationId
    ? organizationsById.get(listing.donorOrganizationId)
    : undefined;
  const claim = claimsByListingId.get(listing.listingId);
  const recipient = claim
    ? organizationsById.get(claim.recipientOrganizationId)
    : undefined;

  return {
    id: listing.listingId,
    title: listing.title,
    restaurant: donor?.name ?? "Unknown donor",
    donorOrganizationId: listing.donorOrganizationId ?? null,
    category: listing.category,
    portions: listing.quantityLabel,
    tags: tagsByListingId.get(listing.listingId) ?? [],
    time: formatPickupTime(listing.pickupWindowEndsAt),
    timeTone: listing.timeTone.toLowerCase() as Listing["timeTone"],
    distance: listing.distanceLabel ?? "Nearby",
    coordinates: {
      x: toNumber(listing.mapPositionX, 20 + index * 16),
      y: toNumber(listing.mapPositionY, 34 + index * 7),
    },
    status: listing.status,
    claimedByOrganizationId: claim?.recipientOrganizationId ?? null,
    pickedUpBy: recipient?.name ?? "",
    datePickedUp: formatDate(claim?.claimedAt),
  };
}

// A listing can have several claims over time (e.g. one cancelled); keep the newest.
export function latestClaimByListingId(claims: PickupClaim[]) {
  const latest = new Map<string, PickupClaim>();
  for (const claim of claims) {
    if (claim.status === "CANCELLED") continue;
    const existing = latest.get(claim.listingId);
    if (!existing || claim.claimedAt > existing.claimedAt) {
      latest.set(claim.listingId, claim);
    }
  }
  return latest;
}

export function groupTagsByListingId(tags: ListingTag[]) {
  return tags.reduce((grouped, tag) => {
    const listingId = tag.listingId ?? tag.listing?.listingId;
    if (!listingId) return grouped;

    const existing = grouped.get(listingId) ?? [];
    grouped.set(listingId, [...existing, tag.tag]);
    return grouped;
  }, new Map<string, string[]>());
}