# API: District-level Party Stats

Endpoint
- GET /api/stats/party/districts

Description
- Returns paginated district-level aggregated statistics computed from v_center_stats_party grouped by district.
- Enforces tenant scoping (org), uses DB RLS policies (app.current_org/app.is_system_admin/app.is_nec_admin) — server sets GUCs in transaction.

Query Parameters
- orgId (UUID) — optional if authenticated principal includes an organization; otherwise required.
- electionId (UUID) — required.
- countyId (UUID) — optional filter by county.
- districtId (UUID) — optional filter for a single district.
- Pagination: page (>=0), size (1..500), sort (Spring Data style, e.g., sort=ballotsCast,desc).

Response
- 200 OK: Page<DistrictStatsPartyDto>
- 400 Bad Request: missing/invalid params
- 403 Forbidden: orgId mismatch
- 404 Not Found: electionId not found

DistrictStatsPartyDto fields
- orgId, electionId, districtId (UUIDs)
- districtName (string)
- countyId, countyName
- registeredVoters, ballotsCast, validVotes, invalidTotal (integers)
- turnoutPct, invalidPct (decimals in [0..1]; multiply by 100 for percent)