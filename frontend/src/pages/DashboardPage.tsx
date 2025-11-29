// src/pages/DashboardPage.tsx
// ------------------------------------------------------
// Org-scoped dashboard body ONLY.
// Sidebar + TopBar come from AppShell.
// ------------------------------------------------------

import React, { useEffect, useState } from "react";
import { useAuthStore } from "../shared/store/authStore";
import { useActiveElections } from "../shared/hooks/useActiveElections";
import { useElectionStats } from "../shared/hooks/useElectionStats";

const DashboardPage: React.FC = () => {
  const currentOrgId = useAuthStore((state) => state.currentOrgId);
  const electionsQuery = useActiveElections();

  const [selectedElectionId, setSelectedElectionId] = useState<string | null>(
    null
  );

  const statsQuery = useElectionStats(currentOrgId, selectedElectionId);
  const stats = statsQuery.data;

  // Default to first active election
  useEffect(() => {
    if (
      electionsQuery.data &&
      electionsQuery.data.length > 0 &&
      !selectedElectionId
    ) {
      setSelectedElectionId(electionsQuery.data[0].electionId);
    }
  }, [electionsQuery.data, selectedElectionId]);

  return (
    <div className="space-y-6">
      {/* Top row: election selector + usage note */}
      <div className="flex flex-col sm:flex-row justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">
            Election Overview
          </h2>
          <p className="text-xs text-slate-500">
            High-level stats for your organization&apos;s active elections.
          </p>
        </div>

        <div className="flex flex-col items-end">
          <span className="text-[11px] text-slate-500 mb-0.5">
            Active election
          </span>
          {electionsQuery.isLoading ? (
            <span className="text-xs text-slate-400">Loading…</span>
          ) : electionsQuery.isError ? (
            <span className="text-xs text-red-600">Error loading</span>
          ) : electionsQuery.data && electionsQuery.data.length > 0 ? (
            <select
              className="text-xs border border-slate-300 rounded-md px-2 py-1 bg-white"
              value={selectedElectionId ?? ""}
              onChange={(e) => setSelectedElectionId(e.target.value)}
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
      </div>

      {/* Stats cards */}
      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Active elections count */}
        <StatCard
          title="Active Elections"
          value={electionsQuery.data?.length ?? 0}
          note="From /api/elections?activeOnly=true"
          loading={electionsQuery.isLoading}
          error={!!electionsQuery.isError}
        />

        <StatCard
          title="Registered Voters"
          value={stats?.registeredVoters ?? 0}
          note="From v_election_stats_party"
          loading={statsQuery.isLoading && !!selectedElectionId}
          error={!!statsQuery.isError}
        />

        <StatCard
          title="Ballots Cast"
          value={stats?.ballotsCast ?? 0}
          note="From v_election_stats_party"
          loading={statsQuery.isLoading && !!selectedElectionId}
          error={!!statsQuery.isError}
        />

        <StatCard
          title="Invalid Ballots"
          value={stats?.invalidTotal ?? 0}
          note="Sum of invalid, blank, rejected, spoiled"
          loading={statsQuery.isLoading && !!selectedElectionId}
          error={!!statsQuery.isError}
        />
      </section>

      {/* Lower section placeholders */}
      <section className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
          <h3 className="text-sm font-semibold text-slate-900 mb-2">
            Recent Activity
          </h3>
          <p className="text-xs text-slate-600">
            Shows submissions, verifications, observer reports… (hooked later).
          </p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
          <h3 className="text-sm font-semibold text-slate-900 mb-2">
            Next Steps
          </h3>
          <ul className="list-disc list-inside text-xs text-slate-700 space-y-1">
            <li>Integrate county/district/center stats views.</li>
            <li>Hook up submission &amp; verification workflows.</li>
            <li>Observer heatmaps with PostGIS.</li>
            {selectedElectionId && (
              <li className="text-[11px] text-slate-500">
                Selected electionId:{" "}
                <span className="font-mono">{selectedElectionId}</span>
              </li>
            )}
            {stats && (
              <li className="text-[11px] text-slate-500">
                Turnout: {stats.turnoutPct}% · Invalid: {stats.invalidPct}%
              </li>
            )}
          </ul>
        </div>
      </section>
    </div>
  );
};

export default DashboardPage;

interface StatCardProps {
  title: string;
  value: number;
  note?: string;
  loading?: boolean;
  error?: boolean;
}

const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  note,
  loading,
  error,
}) => (
  <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
    <p className="text-xs font-semibold text-slate-500 uppercase">{title}</p>
    <p className="mt-2 text-2xl font-bold text-slate-900">
      {loading ? "..." : error ? "!" : value}
    </p>
    {note && !error && (
      <p className="mt-1 text-xs text-slate-500 italic">{note}</p>
    )}
    {error && (
      <p className="mt-1 text-xs text-red-600 italic">Error loading stats</p>
    )}
  </div>
);
