# API: NEC Official Candidate County Stats

Endpoint
- GET /api/stats/official/candidates/counties

Description
- Returns paginated candidate-level aggregated statistics grouped by county from the view `v_candidate_county_stats_official`.
- Only published NEC results are returned (view source filters nec_result.is_published = TRUE).
- Intended for public consumption.

Query Parameters
- electionId (UUID) — required.
- countyId (UUID) — optional.
- candidateId (UUID) — optional.
- partyId (UUID) — optional.
- Pagination/sorting: page (>=0), size (1..500), sort property[,asc|desc].

Response
- 200 OK: Page<CandidateCountyStatsOfficialDto>
- 400 Bad Request: missing/invalid params
- 500 Internal Server Error: unexpected errors

Notes
- vote_share_pct = SUM(candidate_votes) / NULLIF(SUM(center_valid_votes), 0).
- Ensure RLS policies allow published NEC rows to be visible to public callers (policy should include is_published = true).
- When NEC publishes/unpublishes or modifies nec_result rows, call StatsCacheEvictService.evictByElection(electionId) or evictAllStatsCaches() to refresh caches.