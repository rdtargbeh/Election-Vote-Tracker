import React, { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  fetchElections,
  type ElectionDto,
} from "../../shared/services/electionService";

import {
  fetchCandidates,
  type CandidateDto,
  type CandidateOption,
  toCandidateOptions,
} from "../../shared/services/candidateService";

import {
  fetchPollingCenters,
  type PollingCenterDto,
  type PollingCenterOption,
  type PollingCenterQuery,
  toPollingCenterOptions,
} from "../../shared/services/pollingCenterService";

import {
  listElectionCandidatesByElection,
  createElectionCandidate,
  updateElectionCandidate,
  deleteElectionCandidate,
  type ElectionCandidateDto,
  type ElectionCandidateCreateRequest,
  type ElectionCandidateUpdateRequest,
} from "../../shared/services/electionCandidateService";

const ElectionCandidatesPage: React.FC = () => {
  const qc = useQueryClient();

  const [selectedElectionId, setSelectedElectionId] = useState<string>("");

  // Create / edit modal states (simple)
  const [showCreate, setShowCreate] = useState(false);
  const [editing, setEditing] = useState<ElectionCandidateDto | null>(null);

  // Form state (basic)
  const [candidateId, setCandidateId] = useState<string>("");
  const [centerId, setCenterId] = useState<string | "">("");

  // -----------------------------
  // Queries
  // -----------------------------
  const electionsQ = useQuery<ElectionDto[], Error>({
    queryKey: ["elections"],
    queryFn: fetchElections,
    staleTime: 1000 * 60 * 2,
  });

  // IMPORTANT: query returns CandidateDto[], NOT CandidateOption[]
  const candidatesQ = useQuery<CandidateDto[], Error>({
    queryKey: ["candidates"],
    queryFn: fetchCandidates,
    staleTime: 1000 * 60 * 5,
  });

  const candidateOptions: CandidateOption[] = useMemo(() => {
    return toCandidateOptions(candidatesQ.data ?? []);
  }, [candidatesQ.data]);

  const pollingParams: PollingCenterQuery = useMemo(
    () => ({
      q: "",
      districtId: null,
    }),
    []
  );

  // IMPORTANT: query returns PollingCenterDto[], NOT PollingCenterOption[]
  const centersQ = useQuery<PollingCenterDto[], Error>({
    queryKey: ["polling-centers", pollingParams],
    queryFn: () => fetchPollingCenters(pollingParams),
    staleTime: 1000 * 60 * 5,
  });

  const centerOptions: PollingCenterOption[] = useMemo(() => {
    return toPollingCenterOptions(centersQ.data ?? []);
  }, [centersQ.data]);

  // Election candidates (assignments) for selected election
  const electionCandidatesQ = useQuery<ElectionCandidateDto[], Error>({
    queryKey: ["election-candidates", selectedElectionId],
    queryFn: () => listElectionCandidatesByElection(selectedElectionId),
    enabled: !!selectedElectionId,
    staleTime: 0,
  });

  // -----------------------------
  // Mutations
  // -----------------------------
  const createMut = useMutation<
    ElectionCandidateDto,
    Error,
    ElectionCandidateCreateRequest
  >({
    mutationFn: (req) => createElectionCandidate(req),
    onSuccess: () => {
      qc.invalidateQueries({
        queryKey: ["election-candidates", selectedElectionId],
      });
      setShowCreate(false);
      setCandidateId("");
      setCenterId("");
    },
  });

  const updateMut = useMutation<
    ElectionCandidateDto,
    Error,
    { electId: string; req: ElectionCandidateUpdateRequest }
  >({
    mutationFn: ({ electId, req }) => updateElectionCandidate(electId, req),
    onSuccess: () => {
      qc.invalidateQueries({
        queryKey: ["election-candidates", selectedElectionId],
      });
      setEditing(null);
      setCenterId("");
    },
  });

  const deleteMut = useMutation<void, Error, { electId: string }>({
    mutationFn: ({ electId }) => deleteElectionCandidate(electId),
    onSuccess: () => {
      qc.invalidateQueries({
        queryKey: ["election-candidates", selectedElectionId],
      });
    },
  });

  const busy =
    createMut.isPending || updateMut.isPending || deleteMut.isPending;

  // -----------------------------
  // Handlers
  // -----------------------------
  const openCreate = () => {
    setCandidateId("");
    setCenterId("");
    setShowCreate(true);
  };

  const submitCreate = async () => {
    if (!selectedElectionId) {
      alert("Please select an election first.");
      return;
    }
    if (!candidateId) {
      alert("Please select a candidate.");
      return;
    }

    await createMut.mutateAsync({
      electionId: selectedElectionId,
      candidateId,
      centerId: centerId === "" ? null : centerId,
    });
  };

  const openEdit = (row: ElectionCandidateDto) => {
    setEditing(row);
    setCenterId(row.centerId ?? "");
  };

  const submitEdit = async () => {
    if (!editing) return;
    await updateMut.mutateAsync({
      electId: editing.electId,
      req: { centerId: centerId === "" ? null : centerId },
    });
  };

  const removeRow = async (row: ElectionCandidateDto) => {
    const ok = window.confirm(`Remove "${row.fullName}" from this election?`);
    if (!ok) return;
    await deleteMut.mutateAsync({ electId: row.electId });
  };

  // -----------------------------
  // UI
  // -----------------------------
  if (electionsQ.isLoading) return <div className="p-6">Loading...</div>;
  if (electionsQ.isError)
    return <div className="p-6 text-red-600">{electionsQ.error.message}</div>;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-bold">Election Candidates</h1>

        <div className="flex items-center gap-2">
          <select
            className="h-10 rounded-lg border bg-white px-3 text-sm"
            value={selectedElectionId}
            onChange={(e) => setSelectedElectionId(e.target.value)}
          >
            <option value="">Select election...</option>
            {(electionsQ.data ?? []).map((e) => (
              <option key={e.electionId} value={e.electionId}>
                {e.electionName} ({e.year})
              </option>
            ))}
          </select>

          <button
            type="button"
            onClick={openCreate}
            disabled={!selectedElectionId || busy}
            className={[
              "h-10 rounded-lg px-4 text-sm font-semibold",
              selectedElectionId && !busy
                ? "bg-blue-600 text-white hover:bg-blue-700"
                : "cursor-not-allowed bg-gray-200 text-gray-500",
            ].join(" ")}
          >
            + Assign Candidate
          </button>
        </div>
      </div>

      {!selectedElectionId ? (
        <div className="rounded-xl border bg-white p-6 text-sm text-gray-600">
          Select an election to view and manage assigned candidates.
        </div>
      ) : electionCandidatesQ.isLoading ? (
        <div className="p-6">Loading assigned candidates...</div>
      ) : electionCandidatesQ.isError ? (
        <div className="p-6 text-red-600">
          {electionCandidatesQ.error.message}
        </div>
      ) : (
        <div className="overflow-hidden rounded-xl border bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="min-w-full table-fixed border-collapse">
              <thead className="bg-gray-50">
                <tr className="text-left text-xs font-semibold uppercase tracking-wide text-gray-600">
                  <th className="w-[30%] px-4 py-3">Candidate</th>
                  <th className="w-[15%] px-4 py-3">Party</th>
                  <th className="w-[35%] px-4 py-3">Center</th>
                  <th className="w-[20%] px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>

              <tbody className="divide-y">
                {(electionCandidatesQ.data ?? []).map((row) => (
                  <tr key={row.electId} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <div className="font-medium text-gray-900">
                        {row.fullName}
                      </div>
                      <div className="text-xs text-gray-500">
                        {row.candidateId}
                      </div>
                    </td>

                    <td className="px-4 py-3 text-sm text-gray-700">
                      {row.partyAbbrev ?? "—"}
                    </td>

                    <td className="px-4 py-3 text-sm text-gray-700">
                      {row.centerName ?? (
                        <span className="text-gray-400">Nationwide</span>
                      )}
                    </td>

                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => openEdit(row)}
                          disabled={busy}
                          className={[
                            "rounded-md border px-3 py-1.5 text-sm font-medium",
                            !busy
                              ? "border-gray-300 bg-white text-gray-700 hover:bg-gray-50"
                              : "cursor-not-allowed border-gray-200 bg-gray-100 text-gray-400",
                          ].join(" ")}
                        >
                          Edit
                        </button>

                        <button
                          type="button"
                          onClick={() => removeRow(row)}
                          disabled={busy}
                          className={[
                            "rounded-md px-3 py-1.5 text-sm font-semibold",
                            !busy
                              ? "bg-gray-900 text-white hover:bg-black"
                              : "cursor-not-allowed bg-gray-200 text-gray-400",
                          ].join(" ")}
                        >
                          Remove
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}

                {(electionCandidatesQ.data ?? []).length === 0 && (
                  <tr>
                    <td
                      colSpan={4}
                      className="px-4 py-6 text-center text-sm text-gray-600"
                    >
                      No candidates assigned to this election.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* -----------------------------
          Create Modal (simple)
         ----------------------------- */}
      {showCreate && (
        <div className="fixed inset-0 z-[999999] flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-xl rounded-xl bg-white p-6 shadow-lg">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">Assign Candidate</h2>
              <button
                className="rounded-md px-2 py-1 text-sm text-gray-600 hover:bg-gray-100"
                onClick={() => setShowCreate(false)}
                disabled={busy}
              >
                ✕
              </button>
            </div>

            <div className="mt-4 space-y-3">
              <div>
                <label className="block text-sm font-medium text-gray-700">
                  Candidate
                </label>
                <select
                  className="mt-1 w-full rounded-lg border px-3 py-2"
                  value={candidateId}
                  onChange={(e) => setCandidateId(e.target.value)}
                >
                  <option value="">Select candidate...</option>
                  {candidateOptions.map((c) => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                      {c.partyAbbrev ? ` (${c.partyAbbrev})` : ""}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">
                  Polling Center (optional)
                </label>
                <select
                  className="mt-1 w-full rounded-lg border px-3 py-2"
                  value={centerId}
                  onChange={(e) => setCenterId(e.target.value)}
                >
                  <option value="">Nationwide (no center)</option>
                  {centerOptions.map((c) => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <button
                onClick={() => setShowCreate(false)}
                disabled={busy}
                className="rounded-lg border px-4 py-2 text-sm"
              >
                Cancel
              </button>
              <button
                onClick={submitCreate}
                disabled={busy}
                className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-60"
              >
                {busy ? "Saving..." : "Assign"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* -----------------------------
          Edit Modal (center only)
         ----------------------------- */}
      {editing && (
        <div className="fixed inset-0 z-[999999] flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-xl rounded-xl bg-white p-6 shadow-lg">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">Edit Assignment</h2>
              <button
                className="rounded-md px-2 py-1 text-sm text-gray-600 hover:bg-gray-100"
                onClick={() => setEditing(null)}
                disabled={busy}
              >
                ✕
              </button>
            </div>

            <div className="mt-4 space-y-3">
              <div className="rounded-lg border bg-gray-50 px-3 py-2 text-sm text-gray-700">
                <div className="font-semibold">{editing.fullName}</div>
                <div className="text-xs text-gray-500">
                  {editing.partyAbbrev ?? "—"} • {editing.electionName}
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">
                  Polling Center (optional)
                </label>
                <select
                  className="mt-1 w-full rounded-lg border px-3 py-2"
                  value={centerId}
                  onChange={(e) => setCenterId(e.target.value)}
                >
                  <option value="">Nationwide (no center)</option>
                  {centerOptions.map((c) => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <button
                onClick={() => setEditing(null)}
                disabled={busy}
                className="rounded-lg border px-4 py-2 text-sm"
              >
                Cancel
              </button>
              <button
                onClick={submitEdit}
                disabled={busy}
                className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-60"
              >
                {busy ? "Saving..." : "Save"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ElectionCandidatesPage;
