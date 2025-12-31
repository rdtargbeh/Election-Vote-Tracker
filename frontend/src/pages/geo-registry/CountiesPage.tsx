/**
 * GEOGRAPHY: COUNTIES
 * TABLE: county
 * PURPOSE:
 * - Global master data (NEC/SYSTEM can edit; tenants read-only).
 * - Drives geography drilldown everywhere (results, allocations, submissions).
 */

import { useAuth } from "../../auth/useAuth";
import { Badge, Card, Note, PageShell, Table } from "./shared/geo-ui";

export default function CountiesPage() {
  const { dashboardMode } = useAuth();
  const canEdit = dashboardMode === "NEC" || dashboardMode === "SYSTEM";

  const rows = [
    ["Montserrado", "MG-01", "Active", "county"],
    ["Bong", "BG-02", "Active", "county"],
    ["Nimba", "NB-03", "Active", "county"],
  ];

  return (
    <PageShell
      title="Geography • Counties"
      subtitle="Global master data used across allocations, submissions, and results drilldowns."
      right={
        <div className="flex items-center gap-2">
          <Badge>
            {canEdit ? "Editable (NEC/SYSTEM)" : "Read-only (Tenant)"}
          </Badge>
          <button className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold hover:bg-slate-50">
            + Add County (later)
          </button>
        </div>
      }
    >
      <Card title="Counties">
        <Table columns={["County", "Code", "Status", "Source"]} rows={rows} />
        <div className="mt-3 grid grid-cols-1 gap-3 lg:grid-cols-2">
          <Note
            title="Later behavior"
            bullets={[
              "Search + filter by status.",
              "Edit county metadata (NEC/SYSTEM only).",
              "Audit trail recorded (audit_log / audit_ledger).",
            ]}
          />
          <Note
            title="Schema mapping"
            bullets={[
              "county is global master data.",
              "Referenced by district, polling_center, polling_place, and geo result views.",
            ]}
          />
        </div>
      </Card>
    </PageShell>
  );
}
