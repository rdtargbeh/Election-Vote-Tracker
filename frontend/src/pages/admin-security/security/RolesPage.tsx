/**
 * SECURITY: ROLES
 * TABLE: user_role
 * PURPOSE:
 * - System-level roles (SYSTEM_ADMIN, NEC_ADMIN, PARTY_ADMIN, etc.)
 */

import { AdminShell, Badge, Card, Note, Table } from "../shared/admin-ui";

export default function RolesPage() {
  const rows = [
    ["SYSTEM_ADMIN", "System owner access", "user_role"],
    ["NEC_ADMIN", "NEC tenant admin privileges", "user_role"],
    ["PARTY_ADMIN", "Tenant admin privileges", "user_role"],
  ];

  return (
    <AdminShell
      title="Security • Roles"
      subtitle="System-level roles and permissions model."
      right={<Badge>RBAC</Badge>}
    >
      <Card title="Roles">
        <Table columns={["Role", "Description", "Source"]} rows={rows} />
        <div className="mt-3 grid grid-cols-1 gap-3 lg:grid-cols-2">
          <Note
            title="Later behavior"
            bullets={[
              "Roles map to route visibility and API permissions.",
              "Tenant roles (OBSERVER/SUPERVISOR/COORDINATOR/DATA_ENTRY/AUDITOR) live in org_membership.",
            ]}
          />
          <Note
            title="Schema mapping"
            bullets={["user_role defines system roles."]}
          />
        </div>
      </Card>
    </AdminShell>
  );
}
