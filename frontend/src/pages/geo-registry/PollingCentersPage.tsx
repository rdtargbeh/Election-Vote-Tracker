/**
 * GEOGRAPHY: POLLING CENTERS
 * TABLE: polling_center
 * PURPOSE:
 * - Global master locations used by:
 *   - polling_center_allocation (election-scoped)
 *   - vote_submission (election-scoped)
 *   - official geo results views (mv_nec_result_geo)
 */

import { useAuth } from "../../auth/useAuth";
import { Badge, Card, Note, PageShell, Table } from "./shared/geo-ui";

export default function PollingCentersPage() {
  const { dashboardMode } = useAuth();
  const canEdit = dashboardMode === "NEC" || dashboardMode === "SYSTEM";

  const rows = [
    [
      "Montserrado",
      "District 1",
      "PC-021",
      "Paynesville City Hall",
      "polling_center",
    ],
    ["Montserrado", "District 2", "PC-034", "SKD Stadium", "polling_center"],
    ["Bong", "District 3", "PC-019", "Gbarnga Center", "polling_center"],
  ];

  return (
    <PageShell
      title="Geography • Polling Centers"
      subtitle="Global centers used by allocations, submissions, and official geo reporting."
      right={
        <Badge>
          {canEdit ? "Editable (NEC/SYSTEM)" : "Read-only (Tenant)"}
        </Badge>
      }
    >
      <Card
        title="Polling Centers"
        right={
          <button className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold hover:bg-slate-50">
            + Add Center (later)
          </button>
        }
      >
        <Table
          columns={["County", "District", "Code", "Name", "Source"]}
          rows={rows}
        />
        <div className="mt-3 grid grid-cols-1 gap-3 lg:grid-cols-2">
          <Note
            title="Later behavior"
            bullets={[
              "Centers can have geo coordinates (PostGIS) for mapping later.",
              "Used in allocation validation: votes must not exceed ballots issued.",
            ]}
          />
          <Note
            title="Schema mapping"
            bullets={[
              "polling_center is global.",
              "polling_center_allocation ties center to election totals.",
              "vote_submission references polling_center_id.",
            ]}
          />
        </div>
      </Card>
    </PageShell>
  );
}
