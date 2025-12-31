/**
 * ELECTIONS HUB (LIST)
 *
 * PURPOSE:
 * - Show elections list (global official elections from NEC/SYSTEM)
 * - Tenants can read-only view elections.
 * - NEC/SYSTEM can create/edit/activate/deactivate later.
 *
 * DATA SOURCES (SQL):
 * - election
 *
 * ACTIONS (later):
 * - NEC/SYSTEM: Create Election, Edit, Activate/Deactivate
 * - All: Open Workspace (/elections/:electionId)
 */

import { useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/useAuth";
import {
  Panel,
  SimpleTable,
  PlaceholderNote,
  SectionTitle,
  Badge,
} from "./shared/elections-ui";

export default function ElectionsListPage() {
  const nav = useNavigate();
  const { dashboardMode } = useAuth();

  const canManage = dashboardMode === "SYSTEM" || dashboardMode === "NEC";

  // Realistic placeholders for "election" table
  const elections = [
    [
      "Presidential General 2029",
      2029,
      "PRESIDENTIAL_GENERAL",
      "ACTIVE",
      "Draft (not public)",
      "Open",
    ],
    [
      "Senatorial 2026",
      2026,
      "SENATORIAL",
      "INACTIVE",
      "Published (public)",
      "Open",
    ],
    [
      "Local By-Election 2025",
      2025,
      "BY_ELECTION",
      "INACTIVE",
      "Published (public)",
      "Open",
    ],
  ];

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
      <SectionTitle
        title="Elections"
        subtitle="Select an election workspace. Tenants can view official setup read-only and submit their own vote submissions."
      />

      <Panel
        title="Elections List"
        right={
          canManage ? (
            <button
              type="button"
              style={{
                padding: "8px 10px",
                borderRadius: 10,
                border: "1px solid #e5e7eb",
                background: "#fff",
              }}
            >
              + Create Election (NEC/SYSTEM later)
            </button>
          ) : (
            <Badge text="Read-only" />
          )
        }
      >
        <SimpleTable
          columns={[
            "Election",
            "Year",
            "Type",
            "Status",
            "Official Visibility",
            "Action",
          ]}
          rows={elections}
        />

        <div
          style={{ marginTop: 12, display: "flex", gap: 8, flexWrap: "wrap" }}
        >
          <button
            type="button"
            style={{
              padding: "8px 10px",
              borderRadius: 10,
              border: "1px solid #e5e7eb",
              background: "#fff",
            }}
            onClick={() =>
              nav("/elections/11111111-1111-1111-1111-111111111111/overview")
            }
          >
            Open Workspace (sample)
          </button>
        </div>

        <div style={{ marginTop: 12 }}>
          <PlaceholderNote
            title="What happens later"
            bullets={[
              "This list will support search, year/type filters, and status filters (active/inactive).",
              "NEC/SYSTEM will have row actions: Edit, Activate/Deactivate, Publish toggle (public results visibility).",
              "Open routes to workspace tabs: Overview, Setup, Allocation, Submissions, Results, Integrity, NEC Workflow.",
            ]}
          />
        </div>
      </Panel>
    </div>
  );
}
