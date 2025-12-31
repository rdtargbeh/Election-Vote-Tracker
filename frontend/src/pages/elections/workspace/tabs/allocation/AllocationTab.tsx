/**
 * WORKSPACE: ALLOCATION
 *
 * PURPOSE:
 * - Election-scoped allocations:
 *   - Polling center allocation (polling_center_allocation)
 *   - Polling place allocation (polling_place_allocation)
 *
 * PERMISSIONS:
 * - NEC/SYSTEM: create/edit/bulk import
 * - ORG tenants: read-only
 *
 * DATA SOURCES (SQL):
 * - polling_center_allocation, polling_place_allocation
 * - polling_center, polling_place (global master)
 */

import { useState } from "react";
import { useAuth } from "../../../../../auth/useAuth";
import {
  Panel,
  ReadOnlyBanner,
  SimpleTable,
  Badge,
  PlaceholderNote,
} from "../../../shared/elections-ui";

type SubTab = "CENTERS" | "PLACES";

export default function AllocationTab() {
  const { dashboardMode } = useAuth();
  const canEdit = dashboardMode === "NEC" || dashboardMode === "SYSTEM";

  const [tab, setTab] = useState<SubTab>("CENTERS");

  const centerAllocations = [
    ["Montserrado", "PC-021", 1200, 1200, "polling_center_allocation"],
    ["Bong", "PC-019", 780, 780, "polling_center_allocation"],
    ["Nimba", "PC-051", 950, 950, "polling_center_allocation"],
  ];

  const placeAllocations = [
    ["Montserrado", "PC-021", "PP-021-A", 450, 450, "polling_place_allocation"],
    ["Montserrado", "PC-021", "PP-021-B", 750, 750, "polling_place_allocation"],
    ["Bong", "PC-019", "PP-019-A", 780, 780, "polling_place_allocation"],
  ];

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      {!canEdit && (
        <ReadOnlyBanner
          reason="Allocations are official (NEC managed). Tenants view allocation to validate submission totals and detect anomalies (votes > ballots issued)."
          sources={["polling_center_allocation", "polling_place_allocation"]}
        />
      )}

      <Panel
        title="Allocation"
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
              onClick={() => setTab("CENTERS")}
              style={{
                padding: "8px 10px",
                borderRadius: 10,
                border: "1px solid #e5e7eb",
                background: "#fff",
                fontWeight: 700,
              }}
            >
              Centers
            </button>
            <button
              type="button"
              onClick={() => setTab("PLACES")}
              style={{
                padding: "8px 10px",
                borderRadius: 10,
                border: "1px solid #e5e7eb",
                background: "#fff",
                fontWeight: 700,
              }}
            >
              Places
            </button>
            <Badge
              text={canEdit ? "Editable (NEC/SYSTEM)" : "Read-only (Tenant)"}
            />
            {canEdit && (
              <button
                type="button"
                style={{
                  padding: "8px 10px",
                  borderRadius: 10,
                  border: "1px solid #e5e7eb",
                  background: "#fff",
                }}
              >
                Bulk Import (later)
              </button>
            )}
          </div>
        }
      >
        {tab === "CENTERS" && (
          <>
            <SimpleTable
              columns={[
                "County",
                "Center",
                "Registered Voters",
                "Ballots Issued",
                "Source",
              ]}
              rows={centerAllocations}
            />
            <div style={{ marginTop: 12 }}>
              <PlaceholderNote
                title="Later behavior"
                bullets={[
                  "NEC/SYSTEM can import allocation CSV and validate totals.",
                  "Org submissions will be validated against ballots issued to detect anomalies (votes > ballots).",
                ]}
              />
            </div>
          </>
        )}

        {tab === "PLACES" && (
          <>
            <SimpleTable
              columns={[
                "County",
                "Center",
                "Place",
                "Registered Voters",
                "Ballots Issued",
                "Source",
              ]}
              rows={placeAllocations}
            />
            <div style={{ marginTop: 12 }}>
              <PlaceholderNote
                title="Later behavior"
                bullets={[
                  "Places enable more granular reporting and integrity checks.",
                  "If place-level allocation exists, submissions should align to place totals for best accuracy.",
                ]}
              />
            </div>
          </>
        )}
      </Panel>
    </div>
  );
}
