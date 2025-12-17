# Integrating SQL Views into Spring Boot (vote tracker)

Overview

- Map PostgreSQL views (v_center_stats_party, v_candidate_county_stats_party, v_nec_result_geo / mv_nec_result_geo, etc.) to read-only JPA entities.
- Provide Spring Data repositories and a service/controller for frontend consumption.
- Ensure Row-Level Security (RLS) using GUCs app.current_org and app.is_system_admin is applied per-request.

Key runtime requirements and notes

1. RLS GUCs

   - The DB policies use current_setting('app.current_org') and current_setting('app.is_system_admin').
   - The code below sets these per-request using JDBC statements. Two approaches:
     a) SET app.current_org = '...'; SET app.is_system_admin = 'true|false'
     - Persists on the physical connection, so you must RESET them at request end to avoid leaking tenant state to other requests that reuse the same pooled connection.
       b) SET LOCAL app.current_org = '...' executed within a transaction
     - Scoped to the current transaction only (safer). Requires the code that sets the GUC to execute after a transaction has begun (or execute inside same transaction).
   - The provided GucRequestFilter uses approach (a) but resets values in finally. If you prefer safer semantics, switch to a transaction-scoped approach (e.g., using TransactionSynchronization or custom AOP that executes SET LOCAL inside transactional methods).

2. JSONB mapping

   - Some views (v_nec_result_geo, candidate views) include JSONB columns (candidate_votes).
   - Use the Hibernate Types library to map json/jsonb to Java types (Map, POJO).
     Dependency (Maven):
     ```xml
     <dependency>
       <groupId>com.vladmihalcea</groupId>
       <artifactId>hibernate-types-52</artifactId>
       <version>2.21.1</version>
     </dependency>
     ```
   - In entity fields annotate with:
     @Type(type = "jsonb")
     @Column(columnDefinition = "jsonb")

3. Materialized view refresh

   - mv_nec_result_geo is a materialized view. If you need near-real-time values, schedule refreshes using the DB function refresh_mv_nec_result_geo() or call it from the application with appropriate privileges.
   - REFRESH MATERIALIZED VIEW CONCURRENTLY requires a unique index and enough DB resources.

4. Read-only entities

   - Mark view entities as immutable/ read-only to avoid accidental DML.
   - Use org.hibernate.annotations.Immutable and avoid saving them via repositories.

5. Security & Authorization

   - Ensure current authenticated user's org is set to app.current_org (or set the current org based on explicit selection).
   - System admins can bypass via app.is_system_admin = 'true'.

6. Pagination & filtering
   - Repositories below support Pageable and query methods (findByOrgIdAndElectionId etc.) for efficient pagination.

Next steps I can implement for you

- Generate JPA entities for all views (I included examples for center stats and NEC geo).
- Add controllers for the most-used frontend endpoints.
- Provide automated tests (testcontainers) validating RLS and view results.
- Convert the filter to transaction-scoped GUC setter (recommended for production).

If you'd like, tell me which of the following you'd like next:

- "Generate JPA entities for all views"
- "Implement endpoints for candidate county/candidate center views"
- "Switch GUC setter to transaction-scoped approach"
- "Add scheduled MV refresh endpoint"
