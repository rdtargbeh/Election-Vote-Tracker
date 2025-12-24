// src/pages/VoteSubmissionsPage.tsx
// ------------------------------------------------------
// Vote Submissions workspace (tenant-scoped).
//
// Features:
// - Active election selector
// - Search by free text (center/place/agent/etc. - backend "q")
// - Paginated table (page/size)
// - Clear empty/error/loading states
// - Click row → right-side detail drawer
// ------------------------------------------------------

import React, { useEffect, useState } from "react";
import { useAuthStore } from "../shared/store/authStore";
import { useActiveElections } from "../shared/hooks/useActiveElections";
import { useVoteSubmissions } from "../shared/hooks/useVoteSubmissions";
import type { VoteSubmissionDto } from "../shared/types/api";
import { VoteSubmissionDetailDrawer } from "../shared/components/VoteSubmissionDetailDrawer";

const PAGE_SIZE = 20;

const VoteSubmissionsPage: React.FC = () => {
  const currentOrgId = useAuthStore((s) => s.currentOrgId);

  // --- election selector ---
  const electionsQuery = useActiveElections();
  const [selectedElectionId, setSelectedElectionId] = useState<string | null>(
    null
  );

  // --- search + pagination state ---
  const [searchInput, setSearchInput] = useState("");
  const [searchTerm, setSearchTerm] = useState<string>(""); // actually sent to backend
  const [page, setPage] = useState(0);

  // --- selected submission for drawer ---
  const [selectedSubmissionId, setSelectedSubmissionId] = useState<
    string | null
  >(null);

  // When active elections load, default to the first one
  useEffect(() => {
    if (
      electionsQuery.data &&
      electionsQuery.data.length > 0 &&
      !selectedElectionId
    ) {
      setSelectedElectionId(electionsQuery.data[0].electionId);
    }
  }, [electionsQuery.data, selectedElectionId]);

  // Reset page when election or search changes
  useEffect(() => {
    setPage(0);
  }, [selectedElectionId, searchTerm]);

  // --- load submissions ---
  const submissionsQuery = useVoteSubmissions({
    orgId: currentOrgId ?? undefined,
    electionId: selectedElectionId ?? undefined,
    page,
    size: PAGE_SIZE,
    q: searchTerm || undefined,
  });

  const submissionsPage = submissionsQuery.data;
  const submissions = submissionsPage?.content ?? [];
  const totalElements = submissionsPage?.totalElements ?? 0;
  const totalPages = submissionsPage?.totalPages ?? 0;
  const currentPageNumber = submissionsPage?.number ?? page;

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
    <>
      <div className="space-y-6">
        {/* Header / context */}
        <section className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-xl font-semibold text-slate-900">
              Vote Submissions
            </h1>
            <p className="text-xs text-slate-500">
              View and monitor agent submissions for the selected election.
            </p>
          </div>

          {/* Election selector */}
          <div className="flex flex-col items-start gap-1 md:items-end">
            <span className="text-[11px] text-slate-500 mb-0.5">
              Active election
            </span>
            {electionsQuery.isLoading ? (
              <span className="text-xs text-slate-400">Loading elections…</span>
            ) : electionsQuery.isError ? (
              <span className="text-xs text-red-600">
                Error loading elections
              </span>
            ) : electionsQuery.data && electionsQuery.data.length > 0 ? (
              <select
                className="text-xs border border-slate-300 rounded-md px-2 py-1 bg-white"
                value={selectedElectionId ?? ""}
                onChange={(e) => setSelectedElectionId(e.target.value || null)}
              >
                {electionsQuery.data.map((election) => (
                  <option key={election.electionId} value={election.electionId}>
                    {election.electionName} ({election.year})
                  </option>
                ))}
              </select>
            ) : (
              <span className="text-xs text-slate-400">
                No active elections
              </span>
            )}
          </div>
        </section>

        {/* Filters + summary row */}
        <section className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          {/* Search form */}
          <form
            onSubmit={handleSearchSubmit}
            className="flex w-full max-w-md gap-2"
          >
            <input
              type="text"
              className="flex-1 text-sm border border-slate-300 rounded-md px-3 py-2 focus:outline-none focus:ring-1 focus:ring-slate-500"
              placeholder="Search by center, place, agent, comments…"
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

          {/* Summary */}
          <div className="text-right text-xs text-slate-500">
            <p>
              Showing{" "}
              <span className="font-semibold text-slate-700">
                {submissions.length}
              </span>{" "}
              of{" "}
              <span className="font-semibold text-slate-700">
                {totalElements}
              </span>{" "}
              submissions
            </p>
            {selectedElectionId && (
              <p className="mt-0.5 text-[11px] text-slate-400">
                Election ID:{" "}
                <span className="font-mono">
                  {selectedElectionId.slice(0, 8)}…
                </span>
              </p>
            )}
          </div>
        </section>

        {/* Main table / body */}
        <section className="bg-white rounded-xl shadow-sm border border-slate-200">
          {/* Loading / error / no election states */}
          {submissionsQuery.isLoading && !submissionsPage && (
            <div className="p-6 text-sm text-slate-600">
              Loading submissions…
            </div>
          )}

          {!selectedElectionId && !electionsQuery.isLoading && (
            <div className="p-6 text-sm text-slate-600">
              Select an election to view submissions.
            </div>
          )}

          {submissionsQuery.isError && (
            <div className="p-6 text-sm text-red-600">
              Error loading submissions. Please try again.
            </div>
          )}

          {/* Data table */}
          {!submissionsQuery.isLoading &&
            !submissionsQuery.isError &&
            selectedElectionId && (
              <>
                {submissions.length === 0 ? (
                  <div className="p-8 text-center text-sm text-slate-600">
                    <p className="font-medium text-slate-800 mb-1">
                      No submissions found for this election yet.
                    </p>
                    <p className="text-xs text-slate-500">
                      As agents report results from polling places, submissions
                      will appear here in real time.
                    </p>
                  </div>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="min-w-full text-sm">
                      <thead className="bg-slate-50 border-b border-slate-200">
                        <tr>
                          <Th>Time</Th>
                          <Th>Center / Place</Th>
                          <Th>Agent</Th>
                          <Th className="text-right">Ballots Cast</Th>
                          <Th className="text-right">Invalid / Other</Th>
                          <Th>Status</Th>
                        </tr>
                      </thead>
                      <tbody>
                        {submissions.map((s) => (
                          <SubmissionRow
                            key={s.submissionId}
                            submission={s}
                            onSelect={() =>
                              setSelectedSubmissionId(s.submissionId)
                            }
                          />
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
                      onClick={() =>
                        canPrev && setPage((p) => Math.max(0, p - 1))
                      }
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

      {/* Right-side detail drawer */}
      <VoteSubmissionDetailDrawer
        submissionId={selectedSubmissionId}
        onClose={() => setSelectedSubmissionId(null)}
      />
    </>
  );
};

export default VoteSubmissionsPage;

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

const SubmissionRow: React.FC<{
  submission: VoteSubmissionDto;
  onSelect: () => void;
}> = ({ submission, onSelect }) => {
  const submittedAt = submission.submissionTime
    ? new Date(submission.submissionTime).toLocaleString()
    : "—";

  const centerLabel =
    submission.centerName || submission.centerCode || "Center";

  const placeLabel =
    submission.placeLabel ||
    submission.placeCode ||
    (submission.placeNumber != null
      ? `Place ${submission.placeNumber}`
      : "Place");

  const agentLabel = submission.agentName || "Agent";

  const ballotsCast = submission.ballotsCast ?? 0;

  const invalidTotal =
    (submission.invalidBallots ?? 0) +
    (submission.blankBallots ?? 0) +
    (submission.rejectedBallots ?? 0) +
    (submission.spoiledBallots ?? 0);

  const status = (submission.status as string) || "PENDING";

  return (
    <tr
      className="border-b border-slate-100 hover:bg-slate-50/80 cursor-pointer"
      onClick={onSelect}
    >
      <td className="px-3 py-2 align-top">
        <div className="text-xs text-slate-800">{submittedAt}</div>
        <div className="text-[11px] text-slate-400 font-mono">
          {submission.submissionId?.slice(0, 8)}…
        </div>
      </td>

      <td className="px-3 py-2 align-top">
        <div className="text-xs font-medium text-slate-900">{centerLabel}</div>
        <div className="text-[11px] text-slate-500">{placeLabel}</div>
      </td>

      <td className="px-3 py-2 align-top">
        <div className="text-xs text-slate-800">{agentLabel}</div>
      </td>

      <td className="px-3 py-2 align-top text-right">
        <div className="text-xs font-semibold text-slate-900">
          {ballotsCast}
        </div>
      </td>

      <td className="px-3 py-2 align-top text-right">
        <div className="text-xs text-slate-800">{invalidTotal}</div>
        <div className="text-[11px] text-slate-400">
          invalid + blank + rejected + spoiled
        </div>
      </td>

      <td className="px-3 py-2 align-top">
        <StatusBadge status={status} />
      </td>
    </tr>
  );
};

const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const normalized = status.toUpperCase();

  let bg = "bg-slate-100 text-slate-700 border-slate-200";
  if (normalized === "VERIFIED") {
    bg = "bg-emerald-100 text-emerald-800 border-emerald-200";
  } else if (normalized === "PENDING") {
    bg = "bg-amber-100 text-amber-800 border-amber-200";
  } else if (normalized === "REJECTED" || normalized === "FLAGGED") {
    bg = "bg-red-100 text-red-800 border-red-200";
  }

  return (
    <span
      className={`inline-flex items-center px-2 py-1 rounded-full border text-[11px] font-medium ${bg}`}
    >
      {normalized}
    </span>
  );
};
