# API: Candidate county-level Party Stats

Endpoint
- GET /api/stats/party/candidates/counties

Description
- Returns paginated candidate-level aggregated statistics grouped by county from view `v_candidate_county_stats_party`.
- Aggregates candidate_votes across centers in a county and computes vote_share_pct = SUM(candidate_votes) / SUM(center_valid_votes).
- Tenant-scoped via orgId and PostgreSQL RLS (server sets transaction-local GUCs before queries).

Query Parameters
- orgId (UUID) — optional if auth principal includes organization; otherwise required.
- electionId (UUID) — required.
- countyId, candidateId, partyId — optional filters.
- Pagination/sorting: page (>=0), size (1..500), sort property[,asc|desc].

Response
- 200: Page<CandidateCountyStatsPartyDto>
- 400: missing/invalid params
- 403: org mismatch
- 404: election not found

Notes
- vote_share_pct is decimal in [0..1]. When SUM(center_valid_votes)=0 result is NULL.
- Use StatsCacheEvictService to evict caches from write-paths (vote_submission, allocations).