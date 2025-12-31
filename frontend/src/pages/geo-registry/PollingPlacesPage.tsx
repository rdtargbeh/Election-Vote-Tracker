/**
 * GEOGRAPHY: POLLING PLACES
 * TABLE: polling_place
 * PURPOSE:
 * - Places belong under centers and allow granular reporting.
 */

import { useAuth } from "../../auth/useAuth";
import { Badge, Card, Note, PageShell, Table } from "./shared/geo-ui";

export default function PollingPlacesPage() {
  const { dashboardMode } = useAuth();
  const canEdit = dashboardMode === "NEC" || dashboardMode === "SYSTEM";

  const rows = [
    ["PC-021", "PP-021-A", "Room A", "polling_place"],
    ["PC-021", "PP-021-B", "Room B", "polling_place"],
    ["PC-019", "PP-019-A", "Main Hall", "polling_place"],
  ];

  return (
    <PageShell
      title="Geography • Polling Places"
      subtitle="Global places under centers. Allocation and submissions can be place-scoped."
      right={
        <Badge>
          {canEdit ? "Editable (NEC/SYSTEM)" : "Read-only (Tenant)"}
        </Badge>
      }
    >
      <Card
        title="Polling Places"
        right={
          <button className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold hover:bg-slate-50">
            + Add Place (later)
          </button>
        }
      >
        <Table
          columns={["Center Code", "Place Code", "Name", "Source"]}
          rows={rows}
        />
        <div className="mt-3 grid grid-cols-1 gap-3 lg:grid-cols-2">
          <Note
            title="Later behavior"
            bullets={[
              "Place creation requires selecting a polling center.",
              "If election has place allocations, submissions should align at place level for better integrity checks.",
            ]}
          />
          <Note
            title="Schema mapping"
            bullets={[
              "polling_place is global.",
              "polling_place_allocation ties place to election totals.",
              "vote_submission may reference polling_place_id (if used).",
            ]}
          />
        </div>
      </Card>
    </PageShell>
  );
}
