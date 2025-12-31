/**
 * ELECTION WORKSPACE LAYOUT
 *
 * PURPOSE:
 * - Establish election context and show tab navigation.
 * - This is the “entered an election” experience.
 *
 * DATA SOURCES (SQL):
 * - election (header context)
 * - (later) also show official published/draft state
 *
 * IMPORTANT UX:
 * - Tenants see Setup/Allocation read-only but can submit votes in Submissions.
 * - Results mode toggles between Party (ORG) and Official (NEC) when published.
 */

import { Outlet, useParams } from "react-router-dom";
import { useAuth } from "../../../auth/useAuth";
import { Badge, TabsBar, WorkspaceHeader } from "../shared/elections-ui";

export default function ElectionWorkspaceLayout() {
  const { electionId } = useParams();
  const { dashboardMode } = useAuth();

  const isNecOrSystem = dashboardMode === "NEC" || dashboardMode === "SYSTEM";

  // Placeholder election header (later from `election` table)
  const election = {
    electionId: electionId ?? "unknown",
    name: "Presidential General 2029",
    year: 2029,
    type: "PRESIDENTIAL_GENERAL",
    status: "ACTIVE",
    officialPublished: false, // from official result publish flag
  };

  const tabs = [
    { to: "overview", label: "Overview" },
    { to: "setup", label: "Setup" },
    { to: "allocation", label: "Allocation" },
    { to: "submissions", label: "Submissions" },
    { to: "results", label: "Results" },
    { to: "integrity", label: "Integrity" },
    { to: "nec-workflow", label: "NEC Workflow", hidden: !isNecOrSystem },
  ];

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      <WorkspaceHeader
        electionName={election.name}
        meta={`Year: ${election.year} • Type: ${election.type} • Status: ${election.status} • ElectionId: ${election.electionId}`}
        right={
          <div
            style={{
              display: "flex",
              gap: 8,
              alignItems: "center",
              flexWrap: "wrap",
            }}
          >
            <Badge text={election.status} />
            <Badge
              text={
                election.officialPublished
                  ? "Official: Published"
                  : "Official: Draft"
              }
            />
            <button
              type="button"
              style={{
                padding: "8px 10px",
                borderRadius: 10,
                border: "1px solid #e5e7eb",
                background: "#fff",
              }}
            >
              Switch Election (later)
            </button>
          </div>
        }
      />

      <TabsBar tabs={tabs} />

      <div style={{ marginTop: 4 }}>
        <Outlet />
      </div>
    </div>
  );
}
