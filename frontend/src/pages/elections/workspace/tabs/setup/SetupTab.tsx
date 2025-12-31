/**
 * WORKSPACE: SETUP
 *
 * PURPOSE:
 * - Election-scoped official configuration:
 *   - Election Parties (election_party)
 *   - Election Candidates (election_candidate)
 *   - Contests & Options (contest, contest_option)
 *
 * PERMISSIONS:
 * - NEC/SYSTEM: create/edit
 * - ORG tenants: read-only
 *
 * DATA SOURCES (SQL):
 * - party, candidate (global masters)
 * - election_party, election_candidate (election assignments)
 * - contest, contest_option (ballot model)
 */

import { useState } from "react";
import { useAuth } from "../../../../../auth/useAuth";
import {
  Panel,
  ReadOnlyBanner,
  Badge,
  SimpleTable,
  PlaceholderNote,
} from "../../../shared/elections-ui";

type SubTab = "PARTIES" | "CANDIDATES" | "CONTESTS";

export default function SetupTab() {
  const { dashboardMode } = useAuth();
  const canEdit = dashboardMode === "NEC" || dashboardMode === "SYSTEM";

  const [tab, setTab] = useState<SubTab>("PARTIES");

  const electionParties = [
    ["Unity Party", "Qualified", "2029-10-01", "election_party"],
    ["CDC", "Qualified", "2029-10-01", "election_party"],
    ["LP", "Pending", "2029-10-02", "election_party"],
  ];

  const electionCandidates = [
    [
      "Candidate A",
      "Unity Party",
      "National",
      "Assigned",
      "election_candidate",
    ],
    ["Candidate B", "CDC", "National", "Assigned", "election_candidate"],
    ["Candidate C", "LP", "National", "Pending", "election_candidate"],
  ];

  const contests = [
    ["PRESIDENT", "Winner: highest votes", 2, "contest + contest_option"],
    ["REFERENDUM-01", "Yes/No question", 2, "contest + contest_option"],
  ];

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      {!canEdit && (
        <ReadOnlyBanner
          reason="Official ballot setup is managed by NEC/System Admin. You can view it read-only to ensure your submissions match the official contest/candidate structure."
          sources={[
            "election_party",
            "election_candidate",
            "contest",
            "contest_option",
          ]}
        />
      )}

      <Panel
        title="Setup"
        right={
          <div
            style={{
              display: "flex",
              gap: 8,
              alignItems: "center",
              flexWrap: "wrap",
            }}
          >
            <button
              type="button"
              onClick={() => setTab("PARTIES")}
              style={{
                padding: "8px 10px",
                borderRadius: 10,
                border: "1px solid #e5e7eb",
                background: "#fff",
                fontWeight: 700,
              }}
            >
              Parties
            </button>
            <button
              type="button"
              onClick={() => setTab("CANDIDATES")}
              style={{
                padding: "8px 10px",
                borderRadius: 10,
                border: "1px solid #e5e7eb",
                background: "#fff",
                fontWeight: 700,
              }}
            >
              Candidates
            </button>
            <button
              type="button"
              onClick={() => setTab("CONTESTS")}
              style={{
                padding: "8px 10px",
                borderRadius: 10,
                border: "1px solid #e5e7eb",
                background: "#fff",
                fontWeight: 700,
              }}
            >
              Contests
            </button>

            <Badge
              text={canEdit ? "Editable (NEC/SYSTEM)" : "Read-only (Tenant)"}
            />
          </div>
        }
      >
        {tab === "PARTIES" && (
          <>
            <SimpleTable
              columns={["Party", "Status", "Qualified Date", "Source"]}
              rows={electionParties}
            />
            <div style={{ marginTop: 12 }}>
              <PlaceholderNote
                title="Later actions (NEC/SYSTEM)"
                bullets={[
                  "Add party to election (select from global party master).",
                  "Mark qualified/withdrawn; audit log recorded.",
                ]}
              />
            </div>
          </>
        )}

        {tab === "CANDIDATES" && (
          <>
            <SimpleTable
              columns={["Candidate", "Party", "Scope", "Status", "Source"]}
              rows={electionCandidates}
            />
            <div style={{ marginTop: 12 }}>
              <PlaceholderNote
                title="Later actions (NEC/SYSTEM)"
                bullets={[
                  "Assign candidate to election (from global candidate master).",
                  "Optional assignment to polling_center_id (supported by schema).",
                  "Prevent assignment unless party is qualified for election.",
                ]}
              />
            </div>
          </>
        )}

        {tab === "CONTESTS" && (
          <>
            <SimpleTable
              columns={["Contest", "Rule", "#Options", "Source"]}
              rows={contests}
            />
            <div style={{ marginTop: 12 }}>
              <PlaceholderNote
                title="Later actions (NEC/SYSTEM)"
                bullets={[
                  "Create contest (e.g., PRESIDENT, SENATE, REFERENDUM).",
                  "Create options (candidate or yes/no options).",
                  "This enables future-proof ballots beyond candidates.",
                ]}
              />
            </div>
          </>
        )}
      </Panel>
    </div>
  );
}
