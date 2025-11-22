// src/pages/DashboardPage.tsx
// ------------------------------------------------------
// Organization-scoped dashboard.
//
// Data wiring:
// - Active elections: /api/elections/active  (useActiveElections)
// - Election stats:   /api/stats/election-summary?electionId=...
//   (useElectionStats)
// ------------------------------------------------------


import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../shared/store/authStore";
import { useActiveElections } from "../shared/hook/useActiveElections";
import { useElectionStats } from "../shared/hook/useElectionStats";

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const currentOrgId = useAuthStore((state) => state.currentOrgId);
  const clearAuth = useAuthStore((state) => state.clearAuth);

  const electionsQuery = useActiveElections();

  // Track selected election
  const [selectedElectionId, setSelectedElectionId] = useState<string | null>(null);

  // Load stats when org + election chosen
  const statsQuery = useElectionStats(currentOrgId ?? null, selectedElectionId ?? null);
  const stats = statsQuery.data;

  // Auto-select first active election
  useEffect(() => {
    if (
      electionsQuery.data &&
      electionsQuery.data.length > 0 &&
      !selectedElectionId
    ) {
      setSelectedElectionId(electionsQuery.data[0].electionId);
    }
  }, [electionsQuery.data, selectedElectionId]);

  const handleLogout = () => {
    clearAuth();
    navigate("/login");
  };

  return (
    <div className="min-h-screen flex bg-slate-100">
      {/* SIDEBAR */}
      <aside className="w-64 bg-slate-900 text-slate-50 flex flex-col">
        <div className="px-4 py-4 border-b border-slate-800">
          <h1 className="text-lg font-bold">Election Vote Tracker</h1>
          <p className="text-xs text-slate-400 mt-1">Real-time vote tracking</p>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1 text-sm">
          <p className="px-2 text-xs font-semibold text-slate-500 uppercase mb-2">Main</p>

          <button className="w-full text-left px-3 py-2 rounded-md bg-slate-800 text-slate-100">
            Dashboard
          </button>
        </nav>

        <div className="px-4 py-3 border-t border-slate-800 text-xs text-slate-400">
          <p>Org ID:</p>
          <p className="font-mono break-all text-slate-300 text-[11px]">
            {currentOrgId ?? "N/A"}
          </p>
        </div>
      </aside>

      {/* MAIN */}
      <div className="flex-1 flex flex-col">
        {/* TOP BAR */}
        <header className="h-14 px-6 flex items-center justify-between bg-white border-b border-slate-200">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Dashboard</h2>
            <p className="text-xs text-slate-500">
              Overview of your organization&apos;s election activity.
            </p>
          </div>

          {/* RIGHT SIDE CONTROLS */}
          <div className="flex items-center gap-4">
            {/* ELECTION SELECTOR */}
            <div className="flex flex-col items-end">
              <span className="text-[11px] text-slate-500 mb-0.5">Active election</span>

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

            <button
              onClick={handleLogout}
              className="text-xs px-3 py-1.5 rounded-md border border-slate-300 text-slate-700 hover:bg-slate-100"
            >
              Logout
            </button>
          </div>
        </header>

        {/* BODY */}
        <main className="flex-1 p-6 space-y-6">
          {/* TOP STAT CARDS */}
          <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">

            {/* ACTIVE ELECTIONS */}
            <StatCard
              title="Active Elections"
              value={electionsQuery.data?.length ?? 0}
              loading={electionsQuery.isLoading}
              error={electionsQuery.isError}
              note="From /api/elections/active"
            />

            {/* REGISTERED VOTERS */}
            <StatCard
              title="Registered Voters"
              value={stats?.registeredVoters ?? 0}
              loading={statsQuery.isLoading}
              error={statsQuery.isError}
              note="From v_election_stats_party"
            />

            {/* BALLOTS CAST */}
            <StatCard
              title="Ballots Cast"
              value={stats?.ballotsCast ?? 0}
              loading={statsQuery.isLoading}
              error={statsQuery.isError}
              note="From v_election_stats_party"
            />

            {/* INVALID TOTAL */}
            <StatCard
              title="Invalid Ballots"
              value={stats?.invalidTotal ?? 0}
              loading={statsQuery.isLoading}
              error={statsQuery.isError}
              note="Sum: invalid + blank + rejected + spoiled"
            />
          </section>

          {/* BOTTOM PLACEHOLDER */}
          <section className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
              <h3 className="text-sm font-semibold text-slate-900 mb-2">Recent Activity</h3>
              <p className="text-xs text-slate-600">Submissions, verifications, reports…</p>
            </div>

            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
              <h3 className="text-sm font-semibold text-slate-900 mb-2">Next Steps</h3>
              <ul className="list-disc list-inside text-xs text-slate-700 space-y-1">
                <li>County / district / center analytics</li>
                <li>Observer reports & incident tracking</li>
                <li>PostGIS heatmaps</li>

                {selectedElectionId && (
                  <li className="text-[11px] text-slate-500">
                    Election ID: <span className="font-mono">{selectedElectionId}</span>
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
        </main>
      </div>
    </div>
  );
};

export default DashboardPage;


// ------------------------------------------------------
// STAT CARD COMPONENT
// ------------------------------------------------------
const StatCard = ({
  title,
  value,
  note,
  loading,
  error,
}: {
  title: string;
  value: number;
  note?: string;
  loading?: boolean;
  error?: boolean;
}) => (
  <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
    <p className="text-xs font-semibold text-slate-500 uppercase">{title}</p>
    <p className="mt-2 text-2xl font-bold text-slate-900">
      {loading ? "…" : error ? "!" : value}
    </p>
    {note && !error && (
      <p className="mt-1 text-xs text-slate-500 italic">{note}</p>
    )}
    {error && (
      <p className="mt-1 text-xs text-red-600 italic">Error loading stats</p>
    )}
  </div>
);
