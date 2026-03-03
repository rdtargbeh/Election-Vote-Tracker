# API: NEC Official Center Stats

Endpoint
- GET /api/stats/official/centers

Description
- Returns paginated NEC official (published) results aggregated per center from `nec_result` via view `v_center_stats_official`.
- Only published results are shown (view filters nr.is_published = TRUE).
- This endpoint is intended for public / non-tenant consumption of NEC official results.

Query parameters
- electionId (UUID) — required.
- countyId (UUID) — optional.
- districtId (UUID) — optional.
- centerId (UUID) — optional.
- Pagination: page (>=0), size (1..500), sort (Spring Data style)

Response codes
- 200 OK: Page<CenterStatsOfficialDto>
- 400 Bad Request: missing/invalid params
- 500 Internal Server Error: unexpected errors

Security & RLS notes
- Database-level Row-Level Security must be written so that published NEC results are accessible to non-NEC orgs (e.g., policy: is_published = true OR current_setting('app.is_nec_admin', true) = 'true').
- The application sets transaction-local GUCs (TenantGucService) before queries; ensure RLS policies are compatible with published access.
- For NEC internal (unpublished) preview access, implement a separate admin-only endpoint that queries nec_result without the is_published filter (or a separate view) and protect it by role checks (NEC admin/system admin).