/**
 * GEOGRAPHY: DISTRICTS
 * TABLE: district
 */

import { useAuth } from "../../auth/useAuth";
import { Badge, Card, Note, PageShell, Table } from "./shared/geo-ui";

export default function DistrictsPage() {
  const { dashboardMode } = useAuth();
  const canEdit = dashboardMode === "NEC" || dashboardMode === "SYSTEM";

  const rows = [
    ["Montserrado", "District 1", "MD-01", "district"],
    ["Montserrado", "District 2", "MD-02", "district"],
    ["Bong", "District 3", "BD-03", "district"],
  ];

  return (
    <PageShell
      title="Geography • Districts"
      subtitle="Districts roll up under counties and support election reporting and operations."
      right={
        <Badge>
          {canEdit ? "Editable (NEC/SYSTEM)" : "Read-only (Tenant)"}
        </Badge>
      }
    >
      <Card
        title="Districts"
        right={
          <button className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold hover:bg-slate-50">
            + Add District (later)
          </button>
        }
      >
        <Table columns={["County", "District", "Code", "Source"]} rows={rows} />
        <div className="mt-3 grid grid-cols-1 gap-3 lg:grid-cols-2">
          <Note
            title="Later behavior"
            bullets={[
              "District creation requires selecting a county.",
              "Bulk import supported (optional) for national datasets.",
            ]}
          />
          <Note
            title="Schema mapping"
            bullets={[
              "district is global; referenced by polling_center and geo reporting views.",
            ]}
          />
        </div>
      </Card>
    </PageShell>
  );
}
