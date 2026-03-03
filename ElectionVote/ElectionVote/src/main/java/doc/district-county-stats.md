# API: NEC Official District & County Stats

Endpoints
- GET /api/stats/official/districts
- GET /api/stats/official/counties

Description
- Returns paginated NEC official (published) results aggregated by district or county. Source view: `v_district_stats_official` and `v_county_stats_official` built on top of `v_center_stats_official`.
- Only published results are returned (views source v_center_stats_official filters nr.is_published = TRUE).
- Intended for public / non-tenant consumption.

Query parameters (common)
- electionId (UUID) — required.
- countyId (UUID) — optional filter.
- districtId (UUID) — optional (district endpoint).
- Pagination: page (>=0), size (1..500), sort (Spring Data style)

Response codes
- 200 OK: Page<DistrictStatsOfficialDto> / Page<CountyStatsOfficialDto>
- 400 Bad Request: missing/invalid params
- 500 Internal Server Error: unexpected errors

Security & RLS notes
- DB RLS policies must allow published NEC rows to be visible to non-NEC orgs (e.g., policy: is_published = true OR current_setting('app.is_nec_admin', true) = 'true').
- Keep fn_pct and other helper functions defined with SECURITY INVOKER or otherwise ensure they don't bypass RLS.
- When NEC toggles publish status or updates nec_result rows, call StatsCacheEvictService to invalidate cached responses (evictOrgElectionCaches or evictAllStatsCaches).