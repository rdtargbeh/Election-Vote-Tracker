/**
 * OPERATIONS: OBSERVER REPORTS
 *
 * PURPOSE:
 * - Capture and triage observer_report entries (field issues, irregularities).
 * - Link reports to election/county/center/place and optionally to submissions.
 *
 * DATA SOURCES (SQL):
 * - observer_report (tenant-scoped if designed that way; or global with tenant visibility)
 * - file_upload (evidence photos/documents)
 * - notification (alerts)
 * - audit_log (triage actions)
 */

import { Badge, Card, Note, OpsPageShell, Table } from "./shared/ops-ui";
import { useAuth } from "../../auth/useAuth";

export default function ObserverReportsPage() {
  const { tenant } = useAuth();

  const rows = [
    // Realistic placeholders: location + severity + summary + evidence
    [
      "HIGH",
      "Montserrado",
      "PC-034",
      "Intimidation reported",
      "2 attachments",
      "Open",
      "observer_report",
    ],
    [
      "MEDIUM",
      "Bong",
      "PC-019",
      "Late opening",
      "0 attachments",
      "Investigating",
      "observer_report",
    ],
    [
      "LOW",
      "Nimba",
      "PC-051",
      "Queue management issue",
      "1 attachment",
      "Closed",
      "observer_report",
    ],
  ];

  return (
    <OpsPageShell
      title="Operations • Observer Reports"
      subtitle={`Tenant: ${
        tenant?.orgName ?? "—"
      } • Field incident reporting & triage`}
      right={<Badge>Evidence-first</Badge>}
    >
      <Card
        title="Reports"
        right={
          <div className="flex flex-wrap gap-2">
            <button className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold hover:bg-slate-50">
              + New Report (later)
            </button>
            <button className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold hover:bg-slate-50">
              Export (later)
            </button>
          </div>
        }
      >
        <Table
          columns={[
            "Severity",
            "County",
            "Center",
            "Summary",
            "Evidence",
            "Status",
            "Source",
          ]}
          rows={rows.map((r) => [
            <Badge key="sev">{String(r[0])}</Badge>,
            r[1] as string,
            r[2] as string,
            r[3] as string,
            r[4] as string,
            <Badge key="st">{String(r[5])}</Badge>,
            r[6] as string,
          ])}
        />

        <div className="mt-3 grid grid-cols-1 gap-3 lg:grid-cols-2">
          <Note
            title="Later behavior"
            bullets={[
              "Create report with location selectors (county/district/center/place).",
              "Attach evidence (file_upload) and optionally link to a vote_submission.",
              "Supervisor triage: assign → investigate → close with audit trail.",
            ]}
          />
          <Note
            title="Schema mapping"
            bullets={[
              "observer_report stores the incident and metadata.",
              "file_upload stores attachments (photos/docs).",
              "audit_log records triage actions and access.",
            ]}
          />
        </div>
      </Card>
    </OpsPageShell>
  );
}
