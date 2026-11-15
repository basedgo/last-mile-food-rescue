import type { ListingStatus } from './api'

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
  status: ListingStatus
  claimedByOrganizationId: string | null
  pickedUpBy: string
  datePickedUp: string
}