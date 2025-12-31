/**
 * ELECTIONS UI SHARED COMPONENTS
 * PURPOSE:
 * - Shared layout primitives for Elections Hub + Workspace tabs.
 * - Keeps “Election Workspace” consistent and scalable.
 */

import React from "react";
import { NavLink, useLocation } from "react-router-dom";

export function SectionTitle({
  title,
  subtitle,
}: {
  title: string;
  subtitle?: string;
}) {
  return (
    <div>
      <div style={{ fontSize: 18, fontWeight: 900 }}>{title}</div>
      {subtitle && (
        <div style={{ fontSize: 13, opacity: 0.75, marginTop: 4 }}>
          {subtitle}
        </div>
      )}
    </div>
  );
}

export function WorkspaceHeader({
  electionName,
  meta,
  right,
}: {
  electionName: string;
  meta: string;
  right?: React.ReactNode;
}) {
  return (
    <div
      style={{
        border: "1px solid #e5e7eb",
        borderRadius: 14,
        background: "#fff",
        padding: 14,
      }}
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          gap: 12,
          flexWrap: "wrap",
        }}
      >
        <div>
          <div style={{ fontSize: 18, fontWeight: 900 }}>{electionName}</div>
          <div style={{ fontSize: 13, opacity: 0.75, marginTop: 4 }}>
            {meta}
          </div>
        </div>
        {right}
      </div>
    </div>
  );
}

export function TabsBar({
  tabs,
}: {
  tabs: Array<{ to: string; label: string; hidden?: boolean }>;
}) {
  const location = useLocation();
  return (
    <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
      {tabs
        .filter((t) => !t.hidden)
        .map((t) => (
          <NavLink
            key={t.to}
            to={t.to}
            style={({ isActive }) => ({
              textDecoration: "none",
              fontSize: 13,
              padding: "8px 10px",
              borderRadius: 10,
              border: "1px solid #e5e7eb",
              background:
                isActive || location.pathname.endsWith(t.to)
                  ? "#f3f4f6"
                  : "#fff",
              color: "#111827",
              fontWeight: 700,
            })}
          >
            {t.label}
          </NavLink>
        ))}
    </div>
  );
}

export function Panel({
  title,
  right,
  children,
}: {
  title: string;
  right?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <section
      style={{
        border: "1px solid #e5e7eb",
        borderRadius: 14,
        background: "#fff",
      }}
    >
      <div
        style={{
          padding: 14,
          borderBottom: "1px solid #e5e7eb",
          display: "flex",
          justifyContent: "space-between",
          gap: 12,
          alignItems: "center",
        }}
      >
        <div style={{ fontWeight: 800 }}>{title}</div>
        {right}
      </div>
      <div style={{ padding: 14 }}>{children}</div>
    </section>
  );
}

export function Badge({ text }: { text: string }) {
  return (
    <span
      style={{
        fontSize: 12,
        padding: "2px 8px",
        borderRadius: 999,
        border: "1px solid #e5e7eb",
      }}
    >
      {text}
    </span>
  );
}

export function ReadOnlyBanner({
  reason,
  sources,
}: {
  reason: string;
  sources: string[];
}) {
  return (
    <div
      style={{
        border: "1px dashed #cbd5e1",
        borderRadius: 14,
        padding: 12,
        background: "#fafafa",
      }}
    >
      <div style={{ fontWeight: 900 }}>Read-only (Tenant View)</div>
      <div style={{ fontSize: 13, opacity: 0.85, marginTop: 6 }}>{reason}</div>
      <div style={{ fontSize: 12, opacity: 0.75, marginTop: 6 }}>
        Data sources: {sources.join(", ")}
      </div>
    </div>
  );
}

export function PlaceholderNote({
  title,
  bullets,
}: {
  title: string;
  bullets: string[];
}) {
  return (
    <div
      style={{
        border: "1px dashed #cbd5e1",
        borderRadius: 12,
        padding: 12,
        background: "#fafafa",
      }}
    >
      <div style={{ fontWeight: 900, marginBottom: 6 }}>{title}</div>
      <ul style={{ margin: 0, paddingLeft: 18, fontSize: 13, opacity: 0.85 }}>
        {bullets.map((b) => (
          <li key={b} style={{ margin: "4px 0" }}>
            {b}
          </li>
        ))}
      </ul>
    </div>
  );
}

export function SimpleTable({
  columns,
  rows,
}: {
  columns: string[];
  rows: Array<Array<string | number>>;
}) {
  return (
    <div style={{ width: "100%", overflowX: "auto" }}>
      <table style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
          <tr>
            {columns.map((c) => (
              <th
                key={c}
                style={{
                  textAlign: "left",
                  fontSize: 12,
                  opacity: 0.75,
                  padding: "10px 8px",
                  borderBottom: "1px solid #e5e7eb",
                  whiteSpace: "nowrap",
                }}
              >
                {c}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((r, idx) => (
            <tr key={idx}>
              {r.map((cell, j) => (
                <td
                  key={j}
                  style={{
                    padding: "10px 8px",
                    borderBottom: "1px solid #f1f5f9",
                    fontSize: 13,
                    whiteSpace: "nowrap",
                  }}
                >
                  {cell}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
