# Architecture & Design Notes

Goals

- Strong tenant isolation.
- High availability during election day.
- Secure, auditable, tamper-evident tallies.
- Resilient to intermittent connectivity at polling stations.

High-level components

1. Frontend (Web + Mobile)

   - Auth, station check-in, tally submission, photo attachments, incident reports, live dashboard for party agents.
   - Realtime websocket for instantaneous updates to dashboards.

2. API Backend

   - REST/GraphQL endpoints for CRUD, auth, and tallies.
   - Enforces tenant isolation at middleware + DB level.
   - Audit log writer for every state change.

3. Database (Postgres)

   - Tenant-aware schema: recommended Row-Level Security (RLS) with tenant_id column.
   - Consider schema-per-tenant or DB-per-tenant for the highest isolation (tradeoffs: operational complexity vs simplicity).

4. Messaging & Sync

   - Message queue (e.g., Redis streams / RabbitMQ) for background tasks (media processing, notifications).
   - Offline-first clients keep local queue; retries + conflict resolution on reconnect.

5. Realtime

   - WebSocket server for pushing live tally updates to subscribed agents.
   - Auth + channel permissioning per tenant.

6. Monitoring & Auditing
   - Prometheus/Grafana for metrics, Grafana alerting.
   - Immutable audit logs; optionally write signed snapshots of tallies (see "tamper-evidence").

Security & Integrity

- All traffic TLS.
- Content signing for tallies: backend stores tallies + a SHA256 hash of (tally + timestamp + server nonce) in audit_log.
- Strict RBAC (roles per tenant).
- Strong user authentication (SSO/2FA for officials).
- Backup & offsite encrypted backups.

Offline sync strategy

- Client stores local operations and attachments, assigns local operation IDs.
- On reconnect, the client POSTs operation bundle; server validates and assigns canonical IDs.
- Use vector clocks or last-writer-wins with audit trail for conflict resolution (tallies are append-only with corrections).
