# API: NEC Official Candidate Election Stats

Endpoint
- GET /api/stats/official/candidates/elections

Description
- Returns candidate-level aggregated statistics for an entire election from the view `v_candidate_election_stats_official`.
- Only published NEC results are returned (views source filters nec_result.is_published = TRUE).
- Intended for public consumption.

Query Parameters
- electionId (UUID) — required.
- candidateId (UUID) — optional.
- partyId (UUID) — optional.
- Pagination/sorting: page (>=0), size (1..500), sort property[,asc|desc].

Response
- 200 OK: Page<CandidateElectionStatsOfficialDto>
- 400 Bad Request: missing/invalid params
- 500 Internal Server Error: unexpected errors

Notes
- vote_share_pct = SUM(candidate_votes) / NULLIF(SUM(center_valid_votes), 0).
- Use StatsCacheEvictService.evictByElection(electionId) from NEC publish/unpublish workflow to invalidate caches for official endpoints.
- Ensure RLS policies permit published NEC rows to be visible to public callers (include is_published = true).