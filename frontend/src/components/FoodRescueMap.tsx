import { useEffect, useMemo, useRef } from "react";
import Map, {
  Layer,
  Marker,
  NavigationControl,
  Source,
  type LayerProps,
  type MapRef,
} from "react-map-gl/mapbox";
import "mapbox-gl/dist/mapbox-gl.css";
import type { Organization } from "../types/api";
import type { Delivery, LatLng, Listing } from "../types/view";
import { toLatLng } from "../mappers/listings";

// Public token from frontend/.env.local (VITE_MAPBOX_TOKEN=pk....). Never commit it.
const MAPBOX_TOKEN = import.meta.env.VITE_MAPBOX_TOKEN as string | undefined;

// Downtown Seattle, where the seed data lives.
const DEFAULT_CENTER: LatLng = { lat: 47.6097, lng: -122.3331 };

// Line style for volunteer routes. Dashed while assigned, solid once the food is on the way.
const routeLayer: LayerProps = {
  id: "volunteer-routes",
  type: "line",
  paint: {
    "line-color": ["case", ["==", ["get", "status"], "IN_TRANSIT"], "#2563eb", "#7c3aed"],
    "line-width": ["case", ["get", "highlighted"], 5, 3],
    "line-dasharray": ["case", ["==", ["get", "status"], "IN_TRANSIT"], ["literal", [1, 0]], ["literal", [2, 1.5]]],
  },
  layout: { "line-cap": "round", "line-join": "round" },
};

export function FoodRescueMap({
  listings,
  shelters,
  deliveries,
  selectedListingId = null,
  highlightVolunteerId = null,
  onSelectListing,
}: {
  listings: Listing[];
  shelters: Organization[];
  deliveries: Delivery[];
  selectedListingId?: string | null;
  highlightVolunteerId?: string | null;
  onSelectListing?: (listing: Listing) => void;
}) {
  const mapRef = useRef<MapRef>(null);

  // Only active, assigned deliveries are drawn as routes: restaurant -> shelter.
  const routes = useMemo(
    () => ({
      type: "FeatureCollection" as const,
      features: deliveries
        .filter(
          (delivery) =>
            delivery.volunteerUserId &&
            (delivery.status === "CLAIMED" || delivery.status === "IN_TRANSIT") &&
            delivery.restaurantLocation &&
            delivery.shelterLocation,
        )
        .map((delivery) => ({
          type: "Feature" as const,
          properties: {
            status: delivery.status,
            highlighted: delivery.volunteerUserId === highlightVolunteerId,
          },
          geometry: {
            type: "LineString" as const,
            coordinates: [
              [delivery.restaurantLocation!.lng, delivery.restaurantLocation!.lat],
              [delivery.shelterLocation!.lng, delivery.shelterLocation!.lat],
            ],
          },
        })),
    }),
    [deliveries, highlightVolunteerId],
  );

  // Fly to the selected listing when it changes.
  const selectedLocation = listings.find((listing) => listing.id === selectedListingId)?.location;
  useEffect(() => {
    if (selectedLocation) {
      mapRef.current?.flyTo({
        center: [selectedLocation.lng, selectedLocation.lat],
        zoom: 14,
        duration: 800,
      });
    }
  }, [selectedLocation]);

  if (!MAPBOX_TOKEN) {
    return (
      <div className="map-panel map-missing-token">
        Add VITE_MAPBOX_TOKEN to frontend/.env.local to show the map.
      </div>
    );
  }

  return (
    <div className="map-panel">
      <Map
        ref={mapRef}
        mapboxAccessToken={MAPBOX_TOKEN}
        initialViewState={{
          longitude: DEFAULT_CENTER.lng,
          latitude: DEFAULT_CENTER.lat,
          zoom: 13,
        }}
        mapStyle="mapbox://styles/mapbox/streets-v12"
        style={{ position: "absolute", inset: 0 }}
      >
        <NavigationControl position="top-right" />

        <Source id="routes" type="geojson" data={routes}>
          <Layer {...routeLayer} />
        </Source>

        {shelters.map((shelter) => {
          const location = toLatLng(shelter.latitude, shelter.longitude);
          if (!location) return null;
          return (
            <Marker
              key={shelter.organizationId}
              longitude={location.lng}
              latitude={location.lat}
              anchor="center"
            >
              <div className="map-marker shelter" title={shelter.name}>
                {shelter.displayInitials ?? "S"}
              </div>
            </Marker>
          );
        })}

        {listings
          .filter((listing) => listing.location && listing.status === "AVAILABLE")
          .map((listing) => (
            <Marker
              key={listing.id}
              longitude={listing.location!.lng}
              latitude={listing.location!.lat}
              anchor="bottom"
              onClick={(event) => {
                event.originalEvent.stopPropagation();
                onSelectListing?.(listing);
              }}
            >
              <div
                className={`map-marker donation ${listing.id === selectedListingId ? "active" : ""}`}
                title={`${listing.title} — ${listing.restaurant}`}
              >
                {listing.title.slice(0, 1)}
              </div>
            </Marker>
          ))}
      </Map>

      <div className="map-legend">
        <span><i className="legend-dot donation" /> Available food</span>
        <span><i className="legend-dot shelter" /> Shelter</span>
        <span><i className="legend-line" /> Volunteer route</span>
      </div>
    </div>
  );
}