import React, { useMemo } from "react";
import { NavLink, Outlet, useNavigate, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  fetchElections,
  type ElectionDto,
} from "../../shared/services/electionService";

const tabClass = (isActive: boolean) =>
  [
    "rounded-lg px-3 py-2 text-sm font-semibold",
    isActive ? "bg-blue-600 text-white" : "text-gray-700 hover:bg-gray-100",
  ].join(" ");

const ElectionsLayout: React.FC = () => {
  const navigate = useNavigate();
  const { electionId } = useParams<{ electionId: string }>();

  const electionsQ = useQuery<ElectionDto[], Error>({
    queryKey: ["elections"],
    queryFn: fetchElections,
    staleTime: 1000 * 60 * 2,
  });

  const elections = electionsQ.data ?? [];

  const selectedElection = useMemo(() => {
    if (!electionId) return null;
    return elections.find((e) => e.electionId === electionId) ?? null;
  }, [electionId, elections]);

  const onSelectElection = (id: string) => {
    if (!id) {
      navigate("/admin/elections");
      return;
    }
    navigate(`/admin/elections/${id}/candidates`);
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold">Elections</h1>
          <p className="text-sm text-gray-600">
            Manage elections and election-related modules (candidates, parties,
            centers, places).
          </p>
        </div>

        {/* Election selector */}
        <div className="flex items-center gap-2">
          <select
            className="h-10 min-w-[280px] rounded-lg border bg-white px-3 text-sm"
            value={electionId ?? ""}
            onChange={(e) => onSelectElection(e.target.value)}
            disabled={electionsQ.isLoading || electionsQ.isError}
          >
            <option value="">— Select election —</option>
            {elections.map((e) => (
              <option key={e.electionId} value={e.electionId}>
                {e.electionName} ({e.year})
              </option>
            ))}
          </select>

          {/* Back to list */}
          <button
            type="button"
            onClick={() => navigate("/admin/elections")}
            className="h-10 rounded-lg border bg-white px-3 text-sm font-semibold text-gray-700 hover:bg-gray-50"
          >
            Election List
          </button>
        </div>
      </div>

      {/* Tabs (only show when election is selected) */}
      {electionId && (
        <div className="rounded-xl border bg-white p-3">
          <div className="flex flex-wrap items-center gap-2">
            <NavLink
              to={`/admin/elections/${electionId}/candidates`}
              className={({ isActive }) => tabClass(isActive)}
            >
              Candidates
            </NavLink>
            <NavLink
              to={`/admin/elections/${electionId}/parties`}
              className={({ isActive }) => tabClass(isActive)}
            >
              Parties
            </NavLink>
            <NavLink
              to={`/admin/elections/${electionId}/centers`}
              className={({ isActive }) => tabClass(isActive)}
            >
              Polling Centers
            </NavLink>
            <NavLink
              to={`/admin/elections/${electionId}/places`}
              className={({ isActive }) => tabClass(isActive)}
            >
              Polling Places
            </NavLink>

            <div className="ml-auto text-xs text-gray-500">
              {selectedElection ? (
                <span>
                  Selected:{" "}
                  <span className="font-semibold text-gray-700">
                    {selectedElection.electionName}
                  </span>
                </span>
              ) : (
                <span>Selected: —</span>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Child pages render here */}
      <Outlet />
    </div>
  );
};

export default ElectionsLayout;
