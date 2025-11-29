// src/pages/ObserverReportsPage.tsx
// ------------------------------------------------------
// Observer Reports workspace (tenant-scoped).
//
// Features:
// - Search by free text (description, center, observer, etc.)
// - Filter by type (VIOLENCE / INTIMIDATION / EQUIPMENT_ISSUE / OTHER)
// - Filter by status (Open / Resolved / All)
// - Paginated table with modern badges + empty/error states
// ------------------------------------------------------

import React, { useEffect, useState } from "react";
import { useAuthStore } from "../shared/store/authStore";
import { useObserverReports } from "../shared/hooks/useObserverReports";
import type {
  ObserverReportDto,
  ObserverReportType,
} from "../shared/services/observerReportService";

const PAGE_SIZE = 20;

const ObserverReportsPage: React.FC = () => {
  const currentOrgId = useAuthStore((s) => s.currentOrgId);

  // --- filters + pagination state ---
  const [searchInput, setSearchInput] = useState("");
  const [searchTerm, setSearchTerm] = useState<string>("");
  const [typeFilter, setTypeFilter] = useState<ObserverReportType | "">("");
  const [statusFilter, setStatusFilter] = useState<"all" | "open" | "resolved">(
    "all"
  );
  const [page, setPage] = useState(0);

  // Reset page when filters/search change
  useEffect(() => {
    setPage(0);
  }, [searchTerm, typeFilter, statusFilter]);

  const resolvedFilter =
    statusFilter === "open"
      ? false
      : statusFilter === "resolved"
      ? true
      : undefined;

  // --- load reports ---
  const reportsQuery = useObserverReports({
    orgId: currentOrgId ?? undefined,
    type: typeFilter || undefined,
    resolved: resolvedFilter,
    q: searchTerm || undefined,
    page,
    size: PAGE_SIZE,
  });

  const pageData = reportsQuery.data;
  const reports = pageData?.content ?? [];
  const totalElements = pageData?.totalElements ?? 0;
  const totalPages = pageData?.totalPages ?? 0;
  const currentPageNumber = pageData?.number ?? page;

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchTerm(searchInput.trim());
  };

  const handleClearSearch = () => {
    setSearchInput("");
    setSearchTerm("");
  };

  const canPrev = currentPageNumber > 0;
  const canNext = totalPages > 0 && currentPageNumber < totalPages - 1;

  return (
    <div className="space-y-6">
      {/* Header / context */}
      <section className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">
            Observer Reports
          </h1>
          <p className="text-xs text-slate-500">
            Monitor incidents and observations reported from the field.
          </p>
        </div>

        {/* Summary */}
        <div className="text-right text-xs text-slate-500">
          <p>
            Showing{" "}
            <span className="font-semibold text-slate-700">
              {reports.length}
            </span>{" "}
            of{" "}
            <span className="font-semibold text-slate-700">
              {totalElements}
            </span>{" "}
            reports
          </p>
          {currentOrgId && (
            <p className="mt-0.5 text-[11px] text-slate-400">
              Org ID:{" "}
              <span className="font-mono">{currentOrgId.slice(0, 8)}…</span>
            </p>
          )}
        </div>
      </section>

      {/* Filters row */}
      <section className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        {/* Search */}
        <form
          onSubmit={handleSearchSubmit}
          className="flex w-full max-w-md gap-2"
        >
          <input
            type="text"
            className="flex-1 text-sm border border-slate-300 rounded-md px-3 py-2 focus:outline-none focus:ring-1 focus:ring-slate-500"
            placeholder="Search by description, center, observer…"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
          />
          <button
            type="submit"
            className="text-xs px-3 py-2 rounded-md bg-slate-900 text-slate-50 hover:bg-slate-800"
          >
            Search
          </button>
          {searchTerm && (
            <button
              type="button"
              onClick={handleClearSearch}
              className="text-xs px-3 py-2 rounded-md border border-slate-300 text-slate-700 hover:bg-slate-100"
            >
              Clear
            </button>
          )}
        </form>

        {/* Type + status filters */}
        <div className="flex flex-wrap gap-2 text-xs">
          <select
            className="border border-slate-300 rounded-md px-2 py-1 bg-white"
            value={typeFilter}
            onChange={(e) =>
              setTypeFilter((e.target.value || "") as ObserverReportType | "")
            }
          >
            <option value="">All types</option>
            <option value="VIOLENCE">Violence</option>
            <option value="INTIMIDATION">Intimidation</option>
            <option value="EQUIPMENT_ISSUE">Equipment issue</option>
            <option value="OTHER">Other</option>
          </select>

          <select
            className="border border-slate-300 rounded-md px-2 py-1 bg-white"
            value={statusFilter}
            onChange={(e) =>
              setStatusFilter(e.target.value as "all" | "open" | "resolved")
            }
          >
            <option value="all">All statuses</option>
            <option value="open">Open only</option>
            <option value="resolved">Resolved only</option>
          </select>
        </div>
      </section>

      {/* Main table / body */}
      <section className="bg-white rounded-xl shadow-sm border border-slate-200">
        {/* Loading / error */}
        {reportsQuery.isLoading && !pageData && (
          <div className="p-6 text-sm text-slate-600">Loading reports…</div>
        )}

        {reportsQuery.isError && (
          <div className="p-6 text-sm text-red-600">
            Error loading observer reports. Please try again.
          </div>
        )}

        {/* Data table */}
        {!reportsQuery.isLoading && !reportsQuery.isError && (
          <>
            {reports.length === 0 ? (
              <div className="p-8 text-center text-sm text-slate-600">
                <p className="font-medium text-slate-800 mb-1">
                  No observer reports found.
                </p>
                <p className="text-xs text-slate-500">
                  As observers file incident reports, they will appear here in
                  real time.
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full text-sm">
                  <thead className="bg-slate-50 border-b border-slate-200">
                    <tr>
                      <Th>Time</Th>
                      <Th>Location</Th>
                      <Th>Type</Th>
                      <Th>Description</Th>
                      <Th>Status</Th>
                    </tr>
                  </thead>
                  <tbody>
                    {reports.map((r) => (
                      <ObserverReportRow key={r.reportId} report={r} />
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {/* Pagination footer */}
            <div className="flex items-center justify-between px-4 py-3 border-t border-slate-200 text-xs text-slate-600">
              <div>
                Page{" "}
                <span className="font-semibold">
                  {totalPages === 0 ? 0 : currentPageNumber + 1}
                </span>{" "}
                of <span className="font-semibold">{totalPages}</span>
              </div>
              <div className="flex gap-2">
                <button
                  disabled={!canPrev}
                  onClick={() => canPrev && setPage((p) => Math.max(0, p - 1))}
                  className={`px-3 py-1.5 rounded-md border text-xs ${
                    canPrev
                      ? "border-slate-300 text-slate-700 hover:bg-slate-100"
                      : "border-slate-200 text-slate-400 cursor-not-allowed"
                  }`}
                >
                  Previous
                </button>
                <button
                  disabled={!canNext}
                  onClick={() => canNext && setPage((p) => p + 1)}
                  className={`px-3 py-1.5 rounded-md border text-xs ${
                    canNext
                      ? "border-slate-300 text-slate-700 hover:bg-slate-100"
                      : "border-slate-200 text-slate-400 cursor-not-allowed"
                  }`}
                >
                  Next
                </button>
              </div>
            </div>
          </>
        )}
      </section>
    </div>
  );
};

export default ObserverReportsPage;

// ───────────────────────────────────────────
// Small helpers
// ───────────────────────────────────────────

const Th: React.FC<{ children: React.ReactNode; className?: string }> = ({
  children,
  className = "",
}) => (
  <th
    className={
      "px-3 py-2 text-left text-[11px] font-semibold text-slate-500 uppercase " +
      className
    }
  >
    {children}
  </th>
);

const ObserverReportRow: React.FC<{ report: ObserverReportDto }> = ({
  report,
}) => {
  const timeLabel = report.timestamp
    ? new Date(report.timestamp).toLocaleString()
    : "—";

  const locationLabel =
    report.centerName || report.countyName || "Unspecified location";

  const observerLabel = report.observerName || "Observer";

  return (
    <tr className="border-b border-slate-100 hover:bg-slate-50/80">
      <td className="px-3 py-2 align-top">
        <div className="text-xs text-slate-800">{timeLabel}</div>
        <div className="text-[11px] text-slate-400 font-mono">
          {report.reportId.slice(0, 8)}…
        </div>
      </td>

      <td className="px-3 py-2 align-top">
        <div className="text-xs font-medium text-slate-900">
          {locationLabel}
        </div>
        <div className="text-[11px] text-slate-500">{observerLabel}</div>
        {report.latitude != null && report.longitude != null && (
          <div className="text-[10px] text-slate-400 font-mono">
            ({report.latitude.toFixed(4)}, {report.longitude.toFixed(4)})
          </div>
        )}
      </td>

      <td className="px-3 py-2 align-top">
        <TypeBadge type={report.type} />
      </td>

      <td className="px-3 py-2 align-top max-w-xs">
        <div className="text-xs text-slate-800 line-clamp-2">
          {report.description}
        </div>
        {report.mediaUrl && (
          <div className="text-[11px] text-slate-500 mt-0.5">
            Attachment linked
          </div>
        )}
      </td>

      <td className="px-3 py-2 align-top">
        <ReportStatusBadge resolved={report.resolved} />
      </td>
    </tr>
  );
};

const TypeBadge: React.FC<{ type: ObserverReportType }> = ({ type }) => {
  const labelMap: Record<ObserverReportType, string> = {
    VIOLENCE: "Violence",
    INTIMIDATION: "Intimidation",
    EQUIPMENT_ISSUE: "Equipment issue",
    OTHER: "Other",
  };

  let classes = "bg-slate-100 text-slate-800 border-slate-200";

  if (type === "VIOLENCE") {
    classes = "bg-red-100 text-red-800 border-red-200";
  } else if (type === "INTIMIDATION") {
    classes = "bg-amber-100 text-amber-800 border-amber-200";
  } else if (type === "EQUIPMENT_ISSUE") {
    classes = "bg-sky-100 text-sky-800 border-sky-200";
  }

  return (
    <span
      className={`inline-flex items-center px-2 py-1 rounded-full border text-[11px] font-medium ${classes}`}
    >
      {labelMap[type]}
    </span>
  );
};

const ReportStatusBadge: React.FC<{ resolved: boolean }> = ({ resolved }) => {
  const classes = resolved
    ? "bg-emerald-100 text-emerald-800 border-emerald-200"
    : "bg-amber-100 text-amber-800 border-amber-200";

  return (
    <span
      className={`inline-flex items-center px-2 py-1 rounded-full border text-[11px] font-medium ${classes}`}
    >
      {resolved ? "Resolved" : "Open"}
    </span>
  );
};
