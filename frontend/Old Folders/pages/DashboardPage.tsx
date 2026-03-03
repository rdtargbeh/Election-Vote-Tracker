// src/pages/DashboardPage.tsx
// ------------------------------------------------------
// Enhanced Dashboard (fixed TypeScript issues)
// - Adds a "Create Submission" button that navigates to the create form.
// ------------------------------------------------------

import React, { useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";

import { useAuthStore } from "../../shared/lib/store/authStore";
import { useActiveElections } from "../shared/hooks/useActiveElections";
import { useElectionStats } from "../shared/hooks/useElectionStats";
import type {
  ElectionDto,
  ElectionStatsDto,
  ApiError,
} from "../shared/types/api";

const nf = new Intl.NumberFormat(undefined);
const pct = (v: number | null | undefined) =>
  v == null || Number.isNaN(Number(v)) ? "—" : `${Number(v).toFixed(2)}%`;

export default function DashboardPage(): React.ReactElement {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  // Auth / tenant
  const currentOrgId = useAuthStore((s) => s.currentOrgId);
  const currentElectionId = useAuthStore((s) => s.currentElectionId);
  const setCurrentElection = useAuthStore((s) => s.setCurrentElection);

  // Active elections
  const electionsQuery = useActiveElections();
  const elections = electionsQuery.data ?? [];

  // Local selected election id (fall back to auth store)
  const [selectedElectionId, setSelectedElectionId] = useState<string | null>(
    currentElectionId ?? null
  );

  // Keep selectedElectionId in sync with authStore and available elections
  useEffect(() => {
    // Prefer currentElectionId if it exists and is still in the list
    if (
      currentElectionId &&
      elections.some((e) => e.electionId === currentElectionId)
    ) {
      setSelectedElectionId(currentElectionId);
      return;
    }

    // Keep existing selection if still valid
    if (
      selectedElectionId &&
      elections.some((e) => e.electionId === selectedElectionId)
    ) {
      return;
    }

    // Default to first active election if present
    if (elections.length > 0) {
      setSelectedElectionId(elections[0].electionId);
      setCurrentElection(elections[0].electionId);
    } else {
      setSelectedElectionId(null);
      setCurrentElection(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [elections, currentElectionId]);

  // When user changes selection, persist to authStore
  useEffect(() => {
    if (selectedElectionId !== currentElectionId) {
      setCurrentElection(selectedElectionId);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedElectionId]);

  // Stats for selected election + org
  const statsQuery = useElectionStats(
    currentOrgId ?? null,
    selectedElectionId ?? null
  );
  const stats = statsQuery.data as ElectionStatsDto | undefined;
  const statsError = statsQuery.error as ApiError | null;

  const lastFetched = useMemo(() => {
    // React Query exposes dataUpdatedAt as number (ms) on query result; provide fallback
    const t = (statsQuery as any).dataUpdatedAt as number | undefined;
    return t ? new Date(t) : null;
  }, [statsQuery.dataUpdatedAt]);

  // Refresh handler: use object form so TS accepts the filter
  const handleRefresh = async () => {
    if (!currentOrgId || !selectedElectionId) return;
    await queryClient.invalidateQueries({
      queryKey: ["election-stats", currentOrgId, selectedElectionId],
    });
  };

  const handleCreateSubmission = () => {
    // navigate to the protected create submission route
    navigate("/vote-submissions/create");
  };

  return (
    <div className="space-y-6">
      {/* Header row */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">
            Election Overview
          </h1>
          <p className="text-sm text-slate-500">
            High-level stats for your organization&apos;s active elections.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex flex-col items-end">
            <label className="text-[11px] text-slate-500 mb-1">
              Active election
            </label>

            {electionsQuery.isLoading ? (
              <div className="w-64 h-10 rounded-md bg-slate-100 animate-pulse" />
            ) : electionsQuery.isError ? (
              <div className="text-xs text-red-600">
                Failed to load elections
              </div>
            ) : elections.length === 0 ? (
              <div className="text-xs text-slate-400">No active elections</div>
            ) : (
              <select
                aria-label="Select active election"
                className="text-xs border border-slate-300 rounded-md px-3 py-2 bg-white"
                value={selectedElectionId ?? ""}
                onChange={(e) => setSelectedElectionId(e.target.value || null)}
              >
                {elections.map((ev: ElectionDto) => (
                  <option key={ev.electionId} value={ev.electionId}>
                    {ev.electionName} ({ev.year})
                  </option>
                ))}
              </select>
            )}
          </div>

          {/* Refresh and Create buttons */}
          <div className="flex items-center gap-2">
            <button
              onClick={handleRefresh}
              disabled={statsQuery.isFetching || !selectedElectionId}
              className="inline-flex items-center gap-2 px-3 py-2 border rounded-md text-xs bg-white hover:bg-slate-50 disabled:opacity-60"
            >
              <svg
                className={`w-4 h-4 ${
                  statsQuery.isFetching ? "animate-spin" : ""
                }`}
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
              >
                <path
                  d="M21 12a9 9 0 10-3 6.7"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <path
                  d="M21 3v7h-7"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
              <span>Refresh</span>
            </button>

            <button
              onClick={handleCreateSubmission}
              className="inline-flex items-center gap-2 px-3 py-2 rounded-md text-xs bg-sky-600 text-white hover:bg-sky-700 focus:outline-none focus:ring-2 focus:ring-sky-400"
              aria-label="Create a new submission"
            >
              <svg
                className="w-4 h-4"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
              >
                <path
                  d="M12 5v14M5 12h14"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
              <span>Create</span>
            </button>

            {lastFetched && (
              <div className="text-[11px] text-slate-500">
                Updated {lastFetched.toLocaleTimeString()}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Stats cards */}
      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Active Elections"
          value={elections.length}
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
          note="Invalid + blank + rejected + spoiled"
          loading={statsQuery.isLoading && !!selectedElectionId}
          error={!!statsQuery.isError}
        />
      </section>

      {/* Error / info area */}
      {statsError && (
        <div className="bg-rose-50 border border-rose-100 text-rose-700 p-4 rounded-lg text-sm">
          <div className="font-semibold">Error loading stats</div>
          <div className="mt-1">{statsError.message}</div>
          {statsError.details && (
            <pre className="mt-2 text-xs text-rose-600 bg-white/20 p-2 rounded">
              {JSON.stringify(statsError.details, null, 2)}
            </pre>
          )}
        </div>
      )}

      {/* Lower content: placeholders and next steps */}
      <section className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
          <h3 className="text-sm font-semibold text-slate-900 mb-2">
            Recent Activity
          </h3>
          <p className="text-xs text-slate-600">
            Shows submissions, verifications, observer reports… (hooks to be
            connected).
          </p>
          <div className="mt-3 text-xs text-slate-500">
            <ul className="list-disc list-inside space-y-1">
              <li>Agent submissions and approval events</li>
              <li>Observer incidents and photo uploads</li>
              <li>Realtime notifications (STOMP) to be enabled</li>
            </ul>
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
          <h3 className="text-sm font-semibold text-slate-900 mb-2">
            Next Steps
          </h3>

          <ul className="list-disc list-inside text-xs text-slate-700 space-y-1">
            <li>Integrate center/district drilldowns and maps.</li>
            <li>Hook up submission & verification workflows.</li>
            <li>Enable offline queue + presigned uploads for mobile agents.</li>
            {selectedElectionId && (
              <li className="text-[11px] text-slate-500">
                Selected election:{" "}
                <span className="font-mono">{selectedElectionId}</span>
              </li>
            )}
            {stats && (
              <li className="text-[11px] text-slate-500">
                Turnout: {pct(stats.turnoutPct)} · Invalid:{" "}
                {pct(stats.invalidPct)}
              </li>
            )}
          </ul>
        </div>
      </section>
    </div>
  );
}

/* --------------------------------------------------------------------------
   Small presentational components
   -------------------------------------------------------------------------- */

interface StatCardProps {
  title: string;
  value: number | string;
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
}) => {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
      <p className="text-xs font-semibold text-slate-500 uppercase">{title}</p>

      <div className="mt-3 flex items-baseline justify-between gap-2">
        <p className="text-2xl font-bold text-slate-900">
          {loading ? (
            <span className="inline-block w-16 h-6 bg-slate-100 animate-pulse rounded" />
          ) : error ? (
            <span className="text-rose-600">!</span>
          ) : typeof value === "number" ? (
            nf.format(value)
          ) : (
            <span>{value}</span>
          )}
        </p>
      </div>

      {note && !error && (
        <p className="mt-2 text-xs text-slate-500 italic">{note}</p>
      )}
      {error && (
        <p className="mt-2 text-xs text-rose-600 italic">Error loading stats</p>
      )}
    </div>
  );
};
