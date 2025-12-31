# API: NEC Official Candidate Center Stats

Endpoint
- GET /api/stats/official/candidates/centers

Description
- Returns paginated candidate-level aggregated statistics per center from the view `v_candidate_center_stats_official`.
- Only published NEC results are returned; the view filters `nec_result.is_published = TRUE`.
- Intended for public consumption of NEC official candidate results by center.

Query Parameters
- electionId (UUID) — required.
- countyId, districtId, centerId, candidateId, partyId — optional filters.
- Pagination/sorting: page (>=0), size (1..500), sort property[,asc|desc].

Response
- 200 OK: Page<CandidateCenterStatsOfficialDto>
- 400 Bad Request: missing/invalid params
- 500 Internal Server Error: unexpected errors

Notes
- vote_share_pct is candidate_votes / center_valid_votes (NULL when center_valid_votes = 0).
- Ensure DB RLS for nec_result allows published rows to be visible to non-NEC tenants (e.g., policy includes is_published = true).
- When NEC publishes/unpublishes or modifies nec_result rows, call StatsCacheEvictService.evictAllStatsCaches() or targeted eviction helpers to invalidate caches.