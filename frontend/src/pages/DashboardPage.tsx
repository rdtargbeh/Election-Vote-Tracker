
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

  // Track which election is selected for this dashboard view
  const [selectedElectionId, setSelectedElectionId] = useState<string | null>(
    null
  );

  // Load stats for selected election
  const statsQuery = useElectionStats(currentOrgId, selectedElectionId);
  const stats = statsQuery.data;

  // When elections load for the first time, default to the first one (if any)
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
      {/* Sidebar */}
      <aside className="w-64 bg-slate-900 text-slate-50 flex flex-col">
        <div className="px-4 py-4 border-b border-slate-800">
          <h1 className="text-lg font-bold">Election Vote Tracker</h1>
          <p className="text-xs text-slate-400 mt-1">
            Multi-tenant, real-time vote tracking
          </p>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1 text-sm">
          <p className="px-2 text-xs font-semibold text-slate-500 uppercase mb-2">
            Main
          </p>

          <button className="w-full text-left px-3 py-2 rounded-md bg-slate-800 text-slate-100">
            Dashboard
          </button>

          <button className="w-full text-left px-3 py-2 rounded-md hover:bg-slate-800/60">
            Elections
          </button>

          <button
            className="w-full text-left px-3 py-2 rounded-md hover:bg-slate-800/60"
            onClick={() => navigate("/geography")}
          >
            Geography &amp; Centers
          </button>

          <button className="w-full text-left px-3 py-2 rounded-md hover:bg-slate-800/60">
            Vote Submissions
          </button>
          <button className="w-full text-left px-3 py-2 rounded-md hover:bg-slate-800/60">
            Observer Reports
          </button>

          <p className="px-2 text-xs font-semibold text-slate-500 uppercase mt-4 mb-2">
            Admin
          </p>
          <button className="w-full text-left px-3 py-2 rounded-md hover:bg-slate-800/60">
            Users &amp; Roles
          </button>
          <button className="w-full text-left px-3 py-2 rounded-md hover:bg-slate-800/60">
            Organization Settings
          </button>
        </nav>

        <div className="px-4 py-3 border-t border-slate-800 text-xs text-slate-400">
          <p>Org ID:</p>
          <p className="font-mono break-all text-slate-300 text-[11px]">
            {currentOrgId ?? "N/A"}
          </p>
        </div>
      </aside>

      {/* Main content area */}
      <div className="flex-1 flex flex-col">
        {/* Top bar */}
        <header className="h-14 px-6 flex items-center justify-between bg-white border-b border-slate-200">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Dashboard</h2>
            <p className="text-xs text-slate-500">
              High-level overview of your organization&apos;s election activity.
            </p>
          </div>

          <div className="flex items-center gap-4">
            {/* Election selector */}
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
                    <option
                      key={election.electionId}
                      value={election.electionId}
                    >
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

            <div className="text-right">
              <p className="text-sm font-medium text-slate-800">Logged in</p>
              <p className="text-xs text-slate-500">Tenant-scoped view</p>
            </div>

            <button
              onClick={handleLogout}
              className="text-xs px-3 py-1.5 rounded-md border border-slate-300 text-slate-700 hover:bg-slate-100"
            >
              Logout
            </button>
          </div>
        </header>

        {/* Main dashboard body */}
        <main className="flex-1 p-6 space-y-6">
          {/* Top Stats */}
          <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* ACTIVE ELECTIONS — count */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
              <p className="text-xs font-semibold text-slate-500 uppercase">
                Active Elections
              </p>

              {electionsQuery.isLoading ? (
                <p className="mt-2 text-2xl font-bold text-slate-900">...</p>
              ) : electionsQuery.isError ? (
                <p className="mt-2 text-sm text-red-600">Error loading</p>
              ) : (
                <>
                  <p className="mt-2 text-2xl font-bold text-slate-900">
                    {electionsQuery.data?.length ?? 0}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">
                    Retrieved from /api/elections/active
                  </p>
                </>
              )}
            </div>

            {/* REGISTERED VOTERS */}
            <StatCard
              title="Registered Voters"
              value={stats?.registeredVoters ?? 0}
              note="From v_election_stats_party"
              loading={statsQuery.isLoading && !!selectedElectionId}
              error={statsQuery.isError}
            />

            {/* BALLOTS CAST */}
            <StatCard
              title="Ballots Cast"
              value={stats?.ballotsCast ?? 0}
              note="From v_election_stats_party"
              loading={statsQuery.isLoading && !!selectedElectionId}
              error={statsQuery.isError}
            />

            {/* INVALID BALLOTS TOTAL */}
            <StatCard
              title="Invalid Ballots"
              value={stats?.invalidTotal ?? 0}
              note="Sum of invalid, blank, rejected, spoiled"
              loading={statsQuery.isLoading && !!selectedElectionId}
              error={statsQuery.isError}
            />
          </section>

          {/* Lower placeholder section */}
          <section className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
              <h3 className="text-sm font-semibold text-slate-900 mb-2">
                Recent Activity
              </h3>
              <p className="text-xs text-slate-600">
                Shows submissions, verifications, observer reports…
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
        </main>
      </div>
    </div>
  );
};

export default DashboardPage;

// ───────────────────────────────────────────
// SUPPORTING COMPONENT
// ───────────────────────────────────────────
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
