# API: Center-level Party Stats

Endpoint
- GET /api/stats/party/centers

Description
- Returns paginated center-level aggregated statistics for "party" (organization-submitted) vote submissions.
- Data is sourced from the database view `v_center_stats_party`.
- The endpoint enforces tenant scoping: `orgId` is derived from the authenticated principal when available (or must be provided).
- Server-side RLS is applied via PostgreSQL GUCs; the backend ensures the transaction-local GUC is set before querying.

Query Parameters
- orgId (UUID) — optional if authenticated principal includes an organization; otherwise required.
- electionId (UUID) — required.
- countyId (UUID) — optional, filter by county.
- districtId (UUID) — optional, filter by district.
- centerId (UUID) — optional, filter for a single center.
- pageable parameters supported: `page`, `size`, `sort` (Spring Data style).
    - Default size: controlled by client; server caps size at 500.
    - Sorting: supported on fields such as `centerName`, `ballotsCast`, `validVotes`, `turnoutPct`. (Sort keys use entity property names.)

Response
- 200 OK with a Page of CenterStatsPartyDto objects (Spring Data page structure).
- 400 Bad Request if required params are missing.
- 403 Forbidden if the requested orgId does not match the authenticated principal.
- 404 Not Found if electionId does not exist.

CenterStatsPartyDto (response fields)
- orgId (UUID)
- electionId (UUID)
- centerId (UUID)
- centerCode (string)
- centerName (string)
- districtId (UUID)
- districtName (string)
- countyId (UUID)
- countyName (string)
- registeredVoters (integer|null)
- ballotsCast (integer|null)
- validVotes (integer|null)
- invalidTotal (integer|null)
- turnoutPct (decimal|null) — numeric decimal (e.g., 0.5432 means 54.32%). Format on client as desired.
- invalidPct (decimal|null)
- centerLatitude (decimal|null) — optional enrichment from polling_center
- centerLongitude (decimal|null)

Sample Request
GET /api/stats/party/centers?electionId=3fa85f64-5717-4562-b3fc-2c963f66afa6&page=0&size=20&sort=ballotsCast,desc

Sample Response (200)
```json
{
  "content": [
    {
      "orgId": "11111111-1111-1111-1111-111111111111",
      "electionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "centerId": "22222222-2222-2222-2222-222222222222",
      "centerCode": "CTR-001",
      "centerName": "Central High School",
      "districtId": "33333333-3333-3333-3333-333333333333",
      "districtName": "North District",
      "countyId": "44444444-4444-4444-4444-444444444444",
      "countyName": "Example County",
      "registeredVoters": 1200,
      "ballotsCast": 850,
      "validVotes": 830,
      "invalidTotal": 20,
      "turnoutPct": 0.708333,
      "invalidPct": 0.023529,
      "centerLatitude": 34.123456,
      "centerLongitude": -118.123456
    }
  ],
  "pageable": {
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "pageNumber": 0,
    "pageSize": 20,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 5,
  "totalElements": 100,
  "last": false,
  "size": 20,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "first": true,
  "numberOfElements": 1
}
```

Notes / Integration tips
- Percent fields are decimals in the response. The frontend should multiply by 100 and format as percent if desired.
- If you require the polling center geometry (GeoJSON) rather than simple lat/lon, add an enrichment step to fetch geometry from `polling_center` or a separate geospatial endpoint.
- For map clients, prefer requesting by bounding box or tile (if available) to limit returned rows.
- The server applies a tenant GUC in the transaction so DB RLS policies will filter rows; confirm your auth layer supplies the org context claim used by `SecurityUtils`.