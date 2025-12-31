# API: Candidate center-level Party Stats

Endpoint
- GET /api/stats/party/candidates/centers

Description
- Returns paginated candidate-level aggregated statistics per center from the view `v_candidate_center_stats_party`.
- Enforces tenant scoping (org) and DB-level RLS via GUCs.

Query Parameters
- orgId (UUID) — optional if auth principal has org context; otherwise required.
- electionId (UUID) — required.
- countyId, districtId, centerId, candidateId, partyId — optional filters.
- Pagination/sorting: page (>=0), size (1..500), sort property[,asc|desc].

Response
- 200: Page<CandidateCenterStatsPartyDto>
- 400: missing/invalid params
- 403: org mismatch
- 404: election not found

Notes
- vote_share_pct is decimal in [0..1], representing candidate_votes / center_valid_votes.
- Cache eviction must be invoked by write operations (vote_submission / polling_center_allocation) using StatsCacheEvictService.evictAllStatsCaches() to keep aggregates fresh.