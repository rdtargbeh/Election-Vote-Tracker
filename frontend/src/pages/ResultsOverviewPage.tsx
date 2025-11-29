// src/pages/ResultsOverviewPage.tsx
// ------------------------------------------------------
// Results & Analytics overview (tenant-scoped).
//
// Uses the same stats endpoint as the dashboard:
//   - /api/elections/active         -> useActiveElections
//   - /api/stats/election-summary   -> useElectionStats
//
// Shows:
//   - Top-level party/tenant stats for the selected election
//   - Turnout and invalid % with visual bars
//   - Plain-language interpretation text
// ------------------------------------------------------

import React, { useEffect, useState } from "react";
import { useAuthStore } from "../shared/store/authStore";
import { useActiveElections } from "../shared/hooks/useActiveElections";
import { useElectionStats } from "../shared/hooks/useElectionStats";

const ResultsOverviewPage: React.FC = () => {
  const currentOrgId = useAuthStore((s) => s.currentOrgId);

  // --- election selection ---
  const electionsQuery = useActiveElections();
  const [selectedElectionId, setSelectedElectionId] = useState<string | null>(
    null
  );

  useEffect(() => {
    if (
      electionsQuery.data &&
      electionsQuery.data.length > 0 &&
      !selectedElectionId
    ) {
      setSelectedElectionId(electionsQuery.data[0].electionId);
    }
  }, [electionsQuery.data, selectedElectionId]);

  // --- stats for selected election ---
  const statsQuery = useElectionStats(currentOrgId, selectedElectionId);
  const stats = statsQuery.data;

  const turnoutPct = stats?.turnoutPct ?? 0;
  const invalidPct = stats?.invalidPct ?? 0;

  const registeredVoters = stats?.registeredVoters ?? 0;
  const ballotsCast = stats?.ballotsCast ?? 0;
  const validVotes = stats?.validVotes ?? 0;
  const invalidTotal = stats?.invalidTotal ?? 0;

  // Safety: clamp for progress bars
  const turnoutPctClamped = Math.min(Math.max(Number(turnoutPct) || 0, 0), 100);
  const invalidPctClamped = Math.min(Math.max(Number(invalidPct) || 0, 0), 100);

  return (
    <div className="space-y-6">
      {/* Header */}
      <section className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">
            Results &amp; Analytics
          </h1>
          <p className="text-xs text-slate-500">
            High-level results for your organization, based on verified
            submissions.
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
            <span className="text-xs text-slate-400">No active elections</span>
          )}
        </div>
      </section>

      {/* Loading / error / no election states */}
      {!selectedElectionId && !electionsQuery.isLoading && (
        <section className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 text-sm text-slate-600">
          Select an election to see results.
        </section>
      )}

      {selectedElectionId && statsQuery.isLoading && !stats && (
        <section className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 text-sm text-slate-600">
          Loading election stats…
        </section>
      )}

      {selectedElectionId && statsQuery.isError && (
        <section className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 text-sm text-red-600">
          Error loading stats for this election. Make sure there are verified
          submissions and try again.
        </section>
      )}

      {/* Main content when we have an election selected */}
      {selectedElectionId && !statsQuery.isLoading && !statsQuery.isError && (
        <>
          {/* Top metric cards */}
          <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <MetricCard
              label="Registered Voters (party view)"
              value={registeredVoters}
              helper="Sum of registered voters for centers where your org has verified submissions."
            />
            <MetricCard
              label="Ballots Cast"
              value={ballotsCast}
              helper="Total ballots cast across those centers."
            />
            <MetricCard
              label="Valid Votes"
              value={validVotes}
              helper="Sum of candidate votes from verified submissions."
            />
            <MetricCard
              label="Invalid / Other"
              value={invalidTotal}
              helper="Invalid, blank, rejected, and spoiled ballots."
            />
          </section>

          {/* Turnout & quality section */}
          <section className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {/* Turnout card */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
              <h2 className="text-sm font-semibold text-slate-900 mb-1">
                Turnout
              </h2>
              <p className="text-xs text-slate-500 mb-3">
                Based on verified submissions only.
              </p>

              <div className="flex items-baseline gap-2 mb-2">
                <span className="text-2xl font-bold text-slate-900">
                  {turnoutPct.toFixed ? turnoutPct.toFixed(2) : turnoutPct}%
                </span>
                <span className="text-xs text-slate-500">
                  of registered voters have cast ballots in the centers where
                  your org has verified tallies.
                </span>
              </div>

              <ProgressBar percentage={turnoutPctClamped} />

              <p className="mt-3 text-[11px] text-slate-500">
                Calculation: ballots_cast / registered_voters * 100, using your
                party’s parallel tally (view v_election_stats_party).
              </p>
            </div>

            {/* Invalid ballots / quality card */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
              <h2 className="text-sm font-semibold text-slate-900 mb-1">
                Ballot Quality
              </h2>
              <p className="text-xs text-slate-500 mb-3">
                Share of ballots that are invalid, blank, rejected, or spoiled.
              </p>

              <div className="flex items-baseline gap-2 mb-2">
                <span className="text-2xl font-bold text-slate-900">
                  {invalidPct.toFixed ? invalidPct.toFixed(2) : invalidPct}%
                </span>
                <span className="text-xs text-slate-500">
                  of cast ballots are currently classified as non-valid.
                </span>
              </div>

              <ProgressBar percentage={invalidPctClamped} tone="warning" />

              <p className="mt-3 text-[11px] text-slate-500">
                Calculation: invalid_total / ballots_cast * 100. High values may
                indicate training issues, confusion, or potential
                irregularities.
              </p>
            </div>
          </section>

          {/* Narrative interpretation */}
          <section className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
            <h2 className="text-sm font-semibold text-slate-900 mb-2">
              Interpretation (Party Parallel Tally)
            </h2>

            {ballotsCast === 0 ? (
              <p className="text-xs text-slate-600">
                No verified submissions yet for this election under your
                organization. As agents verify more polling places, this section
                will highlight turnout trends and ballot quality.
              </p>
            ) : (
              <ul className="list-disc list-inside text-xs text-slate-700 space-y-1">
                <li>
                  Your party has recorded{" "}
                  <span className="font-semibold">{ballotsCast}</span> ballots
                  cast out of{" "}
                  <span className="font-semibold">{registeredVoters}</span>{" "}
                  registered voters across the centers where you have verified
                  results.
                </li>
                <li>
                  Turnout in those centers is approximately{" "}
                  <span className="font-semibold">
                    {turnoutPct.toFixed ? turnoutPct.toFixed(2) : turnoutPct}%
                  </span>
                  .
                </li>
                <li>
                  Invalid/other ballots currently represent{" "}
                  <span className="font-semibold">
                    {invalidPct.toFixed ? invalidPct.toFixed(2) : invalidPct}%
                  </span>{" "}
                  of ballots cast, totaling{" "}
                  <span className="font-semibold">{invalidTotal}</span> ballots.
                </li>
                <li className="text-[11px] text-slate-500 pt-1">
                  Note: This is a{" "}
                  <span className="font-semibold">party-level</span> view based
                  on your organization’s parallel tally. Official NEC results
                  can be layered on later via the nec_result and
                  v_election_stats_official views.
                </li>
              </ul>
            )}
          </section>
        </>
      )}
    </div>
  );
};

export default ResultsOverviewPage;

// ───────────────────────────────────────────
// Small reusable components
// ───────────────────────────────────────────

interface MetricCardProps {
  label: string;
  value: number;
  helper?: string;
}

const MetricCard: React.FC<MetricCardProps> = ({ label, value, helper }) => {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
      <p className="text-[11px] font-semibold text-slate-500 uppercase">
        {label}
      </p>
      <p className="mt-2 text-2xl font-bold text-slate-900">{value}</p>
      {helper && (
        <p className="mt-1 text-[11px] text-slate-500 leading-snug">{helper}</p>
      )}
    </div>
  );
};

const ProgressBar: React.FC<{
  percentage: number;
  tone?: "default" | "warning";
}> = ({ percentage, tone = "default" }) => {
  const barTone = tone === "warning" ? "bg-amber-500" : "bg-emerald-500";

  return (
    <div className="w-full h-2 rounded-full bg-slate-100 overflow-hidden">
      <div
        className={`h-full ${barTone} transition-all`}
        style={{ width: `${percentage}%` }}
      />
    </div>
  );
};
