/**
 * OPERATIONS: SUBMISSION QUEUE
 *
 * PURPOSE:
 * - Queue-first operations for tenant vote submissions:
 *   - Draft → Submit → Pending Verification → Verified
 *   - Flag/Reject workflows (SUPERVISOR/COORDINATOR/AUDITOR)
 * - Evidence-first UX: tally_sheet/file_upload required before verification (policy)
 * - Freeze rule: VERIFIED submissions become read-only
 *
 * DATA SOURCES (SQL):
 * - vote_submission (orgId tenant-scoped)
 * - vote_submission_contest (contest votes)
 * - tally_sheet + file_upload (evidence)
 * - audit_log (workflow trail)
 *
 * PERMISSIONS:
 * - DATA_ENTRY/OBSERVER: create submissions, attach evidence
 * - SUPERVISOR/COORDINATOR: verify/flag/reject
 * - AUDITOR: discrepancy/anomaly triage (read + propose)
 */

import { useMemo, useState } from "react";
import { useAuth } from "../../auth/useAuth";
import {
  Badge,
  Card,
  Note,
  OpsPageShell,
  StatPill,
  Table,
} from "./shared/ops-ui";

type Queue = "DRAFT" | "PENDING" | "VERIFIED" | "FLAGGED" | "MISSING_EVIDENCE";

export default function SubmissionQueuePage() {
  const { user, tenant } = useAuth();
  const role = (user?.tenantRole ?? "DATA_ENTRY").toUpperCase();

  const [queue, setQueue] = useState<Queue>("PENDING");

  const canVerify = [
    "SUPERVISOR",
    "COORDINATOR",
    "PARTY_ADMIN",
    "ADMIN",
  ].includes(role);
  const canFlag = canVerify || ["AUDITOR"].includes(role);

  const stats = {
    draft: 6,
    pending: 14,
    verified: 48,
    flagged: 2,
    missingEvidence: 5,
  };

  const rowsAll = [
    // Realistic placeholder rows (vote_submission + tally_sheet state)
    {
      county: "Montserrado",
      center: "PC-021",
      place: "PP-021-A",
      status: "PENDING",
      tally: true,
      updated: "2:10 PM",
      risk: "—",
    },
    {
      county: "Montserrado",
      center: "PC-034",
      place: "—",
      status: "FLAGGED",
      tally: true,
      updated: "2:05 PM",
      risk: "Votes > ballots",
    },
    {
      county: "Bong",
      center: "PC-019",
      place: "—",
      status: "VERIFIED",
      tally: true,
      updated: "1:58 PM",
      risk: "—",
    },
    {
      county: "Nimba",
      center: "PC-051",
      place: "—",
      status: "DRAFT",
      tally: false,
      updated: "1:22 PM",
      risk: "Missing tally",
    },
  ];

  const rows = useMemo(() => {
    if (queue === "MISSING_EVIDENCE") return rowsAll.filter((r) => !r.tally);
    return rowsAll.filter((r) => r.status === queue);
  }, [queue]);

  return (
    <OpsPageShell
      title="Operations • Submissions"
      subtitle={`Tenant: ${tenant?.orgName ?? "—"} • Role: ${role}`}
      right={
        <div className="flex flex-wrap items-center gap-2">
          <Badge>Queue-first</Badge>
          <button className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold hover:bg-slate-50">
            + New Submission (later)
          </button>
        </div>
      }
    >
      <div className="grid grid-cols-1 gap-3 md:grid-cols-5">
        <StatPill
          label="Draft"
          value={`${stats.draft}`}
          hint="vote_submission status=DRAFT"
        />
        <StatPill
          label="Pending"
          value={`${stats.pending}`}
          hint="vote_submission status=PENDING"
        />
        <StatPill
          label="Verified"
          value={`${stats.verified}`}
          hint="vote_submission status=VERIFIED"
        />
        <StatPill
          label="Flagged"
          value={`${stats.flagged}`}
          hint="vote_submission status=FLAGGED"
        />
        <StatPill
          label="Missing Evidence"
          value={`${stats.missingEvidence}`}
          hint="tally_sheet/file_upload missing"
        />
      </div>

      <Card
        title="Queues"
        right={
          <div className="flex flex-wrap gap-2">
            {queueBtn("DRAFT", queue, setQueue)}
            {queueBtn("PENDING", queue, setQueue)}
            {queueBtn("VERIFIED", queue, setQueue)}
            {queueBtn("FLAGGED", queue, setQueue)}
            {queueBtn("MISSING_EVIDENCE", queue, setQueue)}
          </div>
        }
      >
        <Table
          columns={[
            "County",
            "Center",
            "Place",
            "Status",
            "Tally Sheet",
            "Last Update",
            "Risk Signal",
            "Actions",
          ]}
          rows={rows.map((r) => [
            r.county,
            r.center,
            r.place,
            <Badge key="s">{r.status}</Badge>,
            r.tally ? (
              <Badge key="t">Attached</Badge>
            ) : (
              <Badge key="nt">Missing</Badge>
            ),
            r.updated,
            r.risk,
            <div key="a" className="flex gap-2">
              <button className="rounded-xl border border-slate-200 bg-white px-2 py-1 text-xs font-bold hover:bg-slate-50">
                View (drawer later)
              </button>
              <button
                disabled={!canVerify}
                className={`rounded-xl border px-2 py-1 text-xs font-bold ${
                  canVerify
                    ? "border-slate-200 bg-white hover:bg-slate-50"
                    : "border-slate-100 bg-slate-50 text-slate-400"
                }`}
                title={
                  !canVerify
                    ? "Requires SUPERVISOR/COORDINATOR/ADMIN"
                    : "Verify submission"
                }
              >
                Verify
              </button>
              <button
                disabled={!canFlag}
                className={`rounded-xl border px-2 py-1 text-xs font-bold ${
                  canFlag
                    ? "border-slate-200 bg-white hover:bg-slate-50"
                    : "border-slate-100 bg-slate-50 text-slate-400"
                }`}
                title={
                  !canFlag
                    ? "Requires AUDITOR or supervisor roles"
                    : "Flag for review"
                }
              >
                Flag
              </button>
            </div>,
          ])}
        />

        <div className="mt-3 grid grid-cols-1 gap-3 lg:grid-cols-2">
          <Note
            title="What this page will do later (locked rules)"
            bullets={[
              "Evidence-first: tally_sheet/file_upload required before verification.",
              "Freeze rule: once VERIFIED, contest votes become read-only.",
              "Detail drawer shows contest votes: vote_submission_contest aligned with official contest model.",
              "Risk signals computed from allocation vs totals (votes > ballots), late centers, suspicious changes.",
            ]}
          />

          <Note
            title="Schema mapping"
            bullets={[
              "vote_submission: header row (orgId, electionId, pollingCenter/Place, status).",
              "vote_submission_contest: normalized votes per contest/option.",
              "tally_sheet + file_upload: evidence attachments and integrity support.",
              "audit_log: workflow actions (submit, verify, flag, reject).",
            ]}
          />
        </div>
      </Card>
    </OpsPageShell>
  );
}

function queueBtn(q: Queue, active: Queue, setQueue: (q: Queue) => void) {
  const on = q === active;
  return (
    <button
      key={q}
      onClick={() => setQueue(q)}
      className={`rounded-xl border px-3 py-2 text-sm font-semibold ${
        on
          ? "border-slate-200 bg-slate-100"
          : "border-slate-200 bg-white hover:bg-slate-50"
      }`}
    >
      {q.replace("_", " ")}
    </button>
  );
}
