
# API: County-level Party Stats

Endpoint
- GET /api/stats/party/counties

Description
- Returns paginated county-level aggregated statistics computed from v_center_stats_party grouped by county.
- Enforces tenant scoping (org) and uses DB RLS policies (server sets transaction-local GUCs before queries).

Query Parameters
- orgId (UUID) — optional if authenticated principal includes an organization; otherwise required.
- electionId (UUID) — required.
- countyId (UUID) — optional filter for a single county.
- Pagination: page (>=0), size (1..500), sort (Spring Data style).

Response
- 200 OK: Page<CountyStatsPartyDto>
- 400 Bad Request: missing/invalid params
- 403 Forbidden: orgId mismatch
- 404 Not Found: electionId not found

CountyStatsPartyDto
- orgId, electionId, countyId (UUIDs)
- countyName (string)
- registeredVoters, ballotsCast, validVotes, invalidTotal (integers)
- turnoutPct, invalidPct (decimals in [0..1]; multiply by 100 for percent)