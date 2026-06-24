# Walk/Bike Support — Correct Implementation

## Context

Google Places Search Along Route only supports DRIVE mode. Walk/Bike was removed from the UI because it showed driving detour times — misleading. This plan adds them back correctly.

## Approach: Two-Phase Search

### Phase 1: Find POIs (existing, no change)
Use DRIVE-mode SAR to discover POIs along the corridor. Walking and driving corridors overlap significantly in urban areas — the same coffee shops are along both routes. This is already implemented in `backend/api/search.ts:341-348`.

### Phase 2: Recalculate detour times for walk/bike
For each POI returned by SAR, calculate the *actual* walking/biking detour using Routes API `computeRoutes` with the POI as an intermediate waypoint:

```
POST /directions/v2:computeRoutes
{
  origin: A,
  destination: B,
  intermediates: [{ location: POI }],
  travelMode: "WALK"  // or "BICYCLE"
}
```

Response gives `routes[0].duration` for the full trip through the POI. Subtract the baseline walk/bike route duration = actual detour.

### Cost
- Routes API: $5/1K requests
- 20 POIs × 1 call each = 20 calls per search = $0.10/search
- At 100 DAU, 3 searches/day = ~$9/month extra for walk/bike mode
- Only triggered when walk/bike is selected (most users will use drive)

## Implementation

### Backend (`backend/api/search.ts`)

1. After SAR returns POIs, if `travelMode` is WALK or BICYCLE:
   - Compute baseline walk/bike route duration (already done in `computeRoute()`)
   - For each POI, call `computeRoutes` with `intermediates: [poi]` and the correct `travelMode`
   - Detour = `routeViaPOI.duration - baselineRoute.duration`
   - Update `detourSeconds` and `detourFormatted` on each result
   - Run these in parallel (Promise.all) for speed

2. Add a new function `recalculateDetours(pois, origin, destination, baselineDuration, travelMode)` that handles the batch recalculation.

3. Cache key already includes travelMode, so walk/bike results cache separately.

### iOS (`RouteInputSheet.swift`, `RouteViewModel.swift`)
- Re-add the Drive/Walk/Bike picker (restore removed code)
- Remove the "Places shown along driving route" note (detour times will now be accurate)
- Bike mode: skip client-side MKDirections route (MapKit has no bike), use backend polyline decoded client-side instead

### Android (`RouteInputPanel.kt`, `RouteViewModel.kt`)
- Re-add the Drive/Walk/Bike FilterChips
- Pass travelMode to backend (already wired)

### Files to modify
- `backend/api/search.ts` — add `recalculateDetours()`, call it for WALK/BICYCLE
- `ios/Detour/Views/RouteInputSheet.swift` — re-add travel mode picker
- `ios/Detour/ViewModels/RouteViewModel.swift` — re-add TravelMode enum usage
- `android/.../ui/component/RouteInputPanel.kt` — re-add FilterChips
- No changes needed to `android/.../viewmodel/RouteViewModel.kt` (travelMode state still exists)

## Verification
- Backend: `curl` with `travelMode: "WALK"` → results show walking detour times (should be larger than driving)
- iOS: Walk mode selected → picker visible, results show "+X min" for walking
- Android: same
- Compare: same route with DRIVE vs WALK should show different detour times (walk detours should be longer)
