/**
 * WORKSPACE: SUBMISSIONS
 *
 * PURPOSE:
 * - Tenant-owned vote submissions for this election.
 * - Queue-first UX: Draft, Pending, Verified, Flagged, Missing Attachments
 * - Later includes detail drawer for a submission.
 *
 * PERMISSIONS:
 * - ORG tenants: create/edit/submit (own orgId)
 * - Supervisors: verify/flag/reject (policy)
 * - NEC: may view aggregated stats; not edit tenant submissions
 *
 * DATA SOURCES (SQL):
 * - vote_submission
 * - vote_submission_contest (normalized contest votes)
 * - vote_submission_ranking (optional)
 * - tally_sheet, file_upload (attachments)
 * - audit_log (workflow audit)
 */

import { useState } from "react";
import { useAuth } from "../../../../../auth/useAuth";
import {
  Panel,
  SimpleTable,
  PlaceholderNote,
  Badge,
} from "../../../shared/elections-ui";

type Queue = "DRAFT" | "PENDING" | "VERIFIED" | "FLAGGED" | "MISSING_EVIDENCE";

export default function SubmissionsTab() {
  const { user } = useAuth();
  const role = (user?.tenantRole ?? "DATA_ENTRY").toUpperCase();

  const [queue, setQueue] = useState<Queue>("PENDING");

  // Realistic placeholder rows (vote_submission + attachment status)
  const rows = [
    [
      "Montserrado",
      "PC-021",
      "PP-021-A",
      "PENDING",
      "Yes",
      "2:10 PM",
      "vote_submission",
    ],
    [
      "Montserrado",
      "PC-034",
      "—",
      "FLAGGED",
      "Yes",
      "2:05 PM",
      "vote_submission",
    ],
    ["Bong", "PC-019", "—", "VERIFIED", "Yes", "1:58 PM", "vote_submission"],
    ["Nimba", "PC-051", "—", "DRAFT", "No", "1:22 PM", "vote_submission"],
  ].filter((r) => {
    if (queue === "MISSING_EVIDENCE") return r[4] === "No";
    return r[3] === queue;
  });

  const canVerify = [
    "SUPERVISOR",
    "COORDINATOR",
    "PARTY_ADMIN",
    "ADMIN",
  ].includes(role);

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      <Panel
        title="Vote Submissions (Election-scoped)"
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
              onClick={() => setQueue("DRAFT")}
              style={btn(queue === "DRAFT")}
            >
              Draft
            </button>
            <button
              type="button"
              onClick={() => setQueue("PENDING")}
              style={btn(queue === "PENDING")}
            >
              Pending
            </button>
            <button
              type="button"
              onClick={() => setQueue("VERIFIED")}
              style={btn(queue === "VERIFIED")}
            >
              Verified
            </button>
            <button
              type="button"
              onClick={() => setQueue("FLAGGED")}
              style={btn(queue === "FLAGGED")}
            >
              Flagged
            </button>
            <button
              type="button"
              onClick={() => setQueue("MISSING_EVIDENCE")}
              style={btn(queue === "MISSING_EVIDENCE")}
            >
              Missing Evidence
            </button>

            <Badge text={canVerify ? "Verifier Role" : "Submitter Role"} />
            <button
              type="button"
              style={{
                padding: "8px 10px",
                borderRadius: 10,
                border: "1px solid #e5e7eb",
                background: "#fff",
              }}
            >
              + New Submission (later)
            </button>
          </div>
        }
      >
        <SimpleTable
          columns={[
            "County",
            "Center",
            "Place",
            "Status",
            "Tally Sheet",
            "Last Update",
            "Source",
          ]}
          rows={rows as any}
        />

        <div style={{ marginTop: 12 }}>
          <PlaceholderNote
            title="Later behavior (critical rules)"
            bullets={[
              "Evidence-first: tally_sheet/file_upload required before verification (policy-driven).",
              "Freeze rule: once VERIFIED, votes become read-only; only dispute flow can reopen.",
              "Detail drawer will show contest votes (vote_submission_contest) aligned with official contests.",
              "Actions depend on role: DATA_ENTRY creates; SUPERVISOR verifies/flags/rejects.",
            ]}
          />
        </div>
      </Panel>
    </div>
  );
}

function btn(active: boolean) {
  return {
    padding: "8px 10px",
    borderRadius: 10,
    border: "1px solid #e5e7eb",
    background: active ? "#f3f4f6" : "#fff",
    fontWeight: 700,
  } as const;
}
