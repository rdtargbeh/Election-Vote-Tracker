/**
 * WORKSPACE: OVERVIEW
 *
 * PURPOSE:
 * - “Command center” for this election:
 *   - Readiness checklist (NEC/SYSTEM)
 *   - Role queues (ORG operational)
 *   - Integrity summary (discrepancy/anomaly)
 *
 * DATA SOURCES (SQL):
 * - election_party, election_candidate, contest, contest_option
 * - polling_center_allocation, polling_place_allocation
 * - vote_submission (per tenant)
 * - discrepancy, anomaly_event
 * - nec_result_staging / nec_result_public (NEC)
 */

import { useAuth } from "../../../../../auth/useAuth";
import {
  Panel,
  PlaceholderNote,
  ReadOnlyBanner,
  SimpleTable,
} from "../../../shared/elections-ui";

export default function OverviewTab() {
  const { dashboardMode } = useAuth();
  const isNecOrSystem = dashboardMode === "NEC" || dashboardMode === "SYSTEM";

  const readinessRows = [
    ["Election parties qualified", "✅", "election_party"],
    ["Candidates assigned", "✅", "election_candidate"],
    ["Contests & options ready", "✅", "contest, contest_option"],
    ["Center allocation", "❌", "polling_center_allocation"],
    ["Place allocation", "❌", "polling_place_allocation"],
    ["Submissions enabled", "❌", "org_setting / election policy"],
    ["Official staging imported", "⚠️", "nec_result_staging"],
    ["Official published", "❌", "nec_result_public"],
  ];

  const orgQueues = [
    ["Draft submissions", 6, "vote_submission"],
    ["Pending verification", 14, "vote_submission"],
    ["Missing tally sheet", 5, "tally_sheet / file_upload"],
    ["Flagged submissions", 2, "vote_submission"],
  ];

  const integritySummary = [
    ["Open discrepancies", 8, "discrepancy"],
    ["Anomalies", 2, "anomaly_event"],
  ];

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      {!isNecOrSystem && (
        <ReadOnlyBanner
          reason="Official election configuration is managed by NEC/System Admin. You can submit your organization’s vote submissions and view public official data when published."
          sources={[
            "election",
            "election_party",
            "election_candidate",
            "contest",
            "contest_option",
          ]}
        />
      )}

      <Panel title="Election Readiness Checklist">
        <SimpleTable
          columns={["Step", "Status", "Source"]}
          rows={readinessRows}
        />
        <div style={{ marginTop: 12 }}>
          <PlaceholderNote
            title="Later behavior"
            bullets={[
              "NEC/SYSTEM sees action buttons that deep-link to Setup/Allocation/NEC Workflow tabs.",
              "Tenants see this checklist read-only to understand what’s ready.",
            ]}
          />
        </div>
      </Panel>

      <Panel title="Organization Operational Queues (Election-scoped)">
        <SimpleTable columns={["Queue", "Count", "Source"]} rows={orgQueues} />
        <div style={{ marginTop: 12 }}>
          <PlaceholderNote
            title="Later behavior"
            bullets={[
              "Each row becomes a clickable queue chip that opens Submissions with URL filters.",
              "Verification will enforce evidence policy: tally_sheet must exist before verify.",
              "Freeze rule: VERIFIED submissions become read-only.",
            ]}
          />
        </div>
      </Panel>

      <Panel title="Integrity Summary">
        <SimpleTable
          columns={["Indicator", "Count", "Source"]}
          rows={integritySummary}
        />
        <div style={{ marginTop: 12 }}>
          <PlaceholderNote
            title="Later behavior"
            bullets={[
              "Click indicator opens Integrity tab filtered to those items.",
              "Compare mode (Results) will create discrepancies from official vs party deltas.",
            ]}
          />
        </div>
      </Panel>
    </div>
  );
}
