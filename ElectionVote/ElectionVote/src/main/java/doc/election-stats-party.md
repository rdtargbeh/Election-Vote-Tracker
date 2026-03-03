
# API: Election-level Party Stats

Endpoint
- GET /api/stats/party/elections

Description
- Returns paginated election-level aggregated statistics computed from v_center_stats_party grouped by election.
- Enforces tenant scoping (org) and uses DB RLS policies; server sets transaction-local GUCs before repository queries.

Query Parameters
- orgId (UUID) — optional if authenticated principal includes an organization; otherwise required.
- electionId (UUID) — required.
- Pagination: page (>=0), size (1..500), sort (Spring Data style). Usually a single row per org+election but pageable is supported for consistency.

Response
- 200 OK: Page<ElectionStatsPartyDto>
- 400 Bad Request: missing/invalid params
- 403 Forbidden: orgId mismatch
- 404 Not Found: electionId not found

ElectionStatsPartyDto
- orgId, electionId (UUIDs)
- registeredVoters, ballotsCast, validVotes, invalidTotal (integers)
- turnoutPct, invalidPct (decimals in [0..1]; multiply by 100 for percent)