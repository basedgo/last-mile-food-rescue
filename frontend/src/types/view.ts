import type { ClaimStatus, ListingStatus } from './api'

export type LatLng = { lat: number; lng: number }

export type Listing = {
  id: string
  title: string
  restaurant: string
  donorOrganizationId: string | null
  category: string
  portions: string
  tags: string[]
  time: string
  timeTone: 'urgent' | 'soon' | 'calm'
  distance: string
  coordinates: { x: number; y: number }
  location: LatLng | null
  status: ListingStatus
  claimedByOrganizationId: string | null
  pickedUpBy: string
  datePickedUp: string
}

export type Delivery = {
  claimId: string
  listingTitle: string
  portions: string
  restaurant: string
  shelter: string
  status: ClaimStatus
  volunteerUserId: string | null
  volunteerName: string
  restaurantLocation: LatLng | null
  shelterLocation: LatLng | null
}