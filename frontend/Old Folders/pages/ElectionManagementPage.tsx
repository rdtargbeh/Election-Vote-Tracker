// ElectionManagementPage.tsx (FINAL)
// Assumptions:
// - Route is already protected by <RequireElectionAdmin /> so this page does NOT re-check permissions.
// - Backend exposes PATCH /api/elections/{id}/active?active=true|false (via electionService.setElectionActive)
// - fetchElections returns real DB values (including isActive) after you align DTO JSON property name.

import React, { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import CreateElectionModal from "../shared/components/CreateElectionModal";
import EditElectionModal from "../shared/components/EditElectionModal";

import {
  fetchElections,
  createElection,
  updateElection,
  deleteElection,
  setElectionActive,
  type ElectionDto,
  type ElectionCreateRequest,
  type ElectionUpdateRequest,
} from "../shared/services/electionService";

const ElectionManagementPage: React.FC = () => {
  const qc = useQueryClient();

  const [isCreating, setIsCreating] = useState(false);
  const [editing, setEditing] = useState<ElectionDto | null>(null);

  const {
    data: elections,
    isLoading,
    isError,
    error,
    isFetching,
  } = useQuery<ElectionDto[], Error>({
    queryKey: ["elections"],
    queryFn: fetchElections,

    // ✅ reflect DB values (status) consistently
    staleTime: 0,
    refetchOnMount: "always",
    refetchOnWindowFocus: true,
  });

  const createMutation = useMutation<ElectionDto, Error, ElectionCreateRequest>(
    {
      mutationFn: (req) => createElection(req),
      onSuccess: async () => {
        await qc.invalidateQueries({ queryKey: ["elections"] });
        setIsCreating(false);
      },
    }
  );

  const updateMutation = useMutation<
    ElectionDto,
    Error,
    { electionId: string; req: ElectionUpdateRequest }
  >({
    mutationFn: ({ electionId, req }) => updateElection(electionId, req),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ["elections"] });
      setEditing(null);
    },
  });

  const deleteMutation = useMutation<void, Error, { electionId: string }>({
    mutationFn: ({ electionId }) => deleteElection(electionId),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ["elections"] });
    },
  });

  const toggleMutation = useMutation<
    ElectionDto,
    Error,
    { electionId: string; isActive: boolean }
  >({
    mutationFn: ({ electionId, isActive }) =>
      setElectionActive(electionId, isActive),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ["elections"] });
    },
  });

  const busy =
    createMutation.isPending ||
    updateMutation.isPending ||
    deleteMutation.isPending ||
    toggleMutation.isPending;

  async function onCreate(req: ElectionCreateRequest) {
    await createMutation.mutateAsync(req);
  }

  async function onEditSave(req: ElectionUpdateRequest) {
    if (!editing) return;
    await updateMutation.mutateAsync({ electionId: editing.electionId, req });
  }

  async function onToggleActive(e: ElectionDto) {
    await toggleMutation.mutateAsync({
      electionId: e.electionId,
      isActive: !e.isActive,
    });
  }

  async function onDelete(e: ElectionDto) {
    const ok = window.confirm(`Delete election "${e.electionName}"?`);
    if (!ok) return;
    await deleteMutation.mutateAsync({ electionId: e.electionId });
  }

  if (isLoading) return <div className="p-6">Loading elections...</div>;
  if (isError)
    return <div className="p-6 text-red-600">Error: {error?.message}</div>;

  return (
    <div className="space-y-4">
      {/* Header + Create */}
      <div className="flex items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold">Election Management</h1>
          {isFetching && (
            <p className="mt-1 text-xs text-gray-500">Refreshing…</p>
          )}
        </div>

        <button
          type="button"
          onClick={() => setIsCreating(true)}
          disabled={busy}
          className="rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          + Create New Election
        </button>
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-xl border bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="min-w-full table-fixed border-collapse">
            <thead className="bg-gray-50">
              <tr className="text-left text-xs font-semibold uppercase tracking-wide text-gray-600">
                <th className="w-[35%] px-4 py-3">Name</th>
                <th className="w-[12%] px-4 py-3">Year</th>
                <th className="w-[25%] px-4 py-3">Type</th>
                <th className="w-[13%] px-4 py-3">Status</th>
                <th className="w-[15%] px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>

            <tbody className="divide-y">
              {(elections ?? []).map((e) => (
                <tr key={e.electionId} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <div className="truncate font-medium text-gray-900">
                      {e.electionName}
                    </div>
                  </td>

                  <td className="px-4 py-3 text-sm text-gray-700">{e.year}</td>

                  <td className="px-4 py-3 text-sm text-gray-700">
                    {e.electionType}
                  </td>

                  {/* ✅ DB-driven (ensure backend returns JSON property "isActive") */}
                  <td className="px-4 py-3">
                    {e.isActive ? (
                      <span className="inline-flex items-center rounded-full bg-green-100 px-3 py-1 text-xs font-semibold text-green-700">
                        Active
                      </span>
                    ) : (
                      <span className="inline-flex items-center rounded-full bg-gray-200 px-3 py-1 text-xs font-semibold text-gray-700">
                        Inactive
                      </span>
                    )}
                  </td>

                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        onClick={() => setEditing(e)}
                        disabled={busy}
                        className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60"
                      >
                        Edit
                      </button>

                      <button
                        type="button"
                        onClick={() => onToggleActive(e)}
                        disabled={busy}
                        className={`rounded-md px-3 py-1.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60 ${
                          e.isActive
                            ? "bg-red-600 hover:bg-red-700"
                            : "bg-blue-600 hover:bg-blue-700"
                        }`}
                      >
                        {e.isActive ? "Deactivate" : "Activate"}
                      </button>

                      <button
                        type="button"
                        onClick={() => onDelete(e)}
                        disabled={busy}
                        className="rounded-md bg-gray-900 px-3 py-1.5 text-sm font-semibold text-white hover:bg-black disabled:cursor-not-allowed disabled:opacity-60"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}

              {(elections ?? []).length === 0 && (
                <tr>
                  <td
                    colSpan={5}
                    className="px-4 py-6 text-center text-sm text-gray-600"
                  >
                    No elections found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modals */}
      {isCreating && (
        <CreateElectionModal
          onClose={() => setIsCreating(false)}
          onSubmit={onCreate}
          isLoading={createMutation.isPending}
        />
      )}

      {editing && (
        <EditElectionModal
          election={editing}
          onClose={() => setEditing(null)}
          onSubmit={onEditSave}
          isLoading={updateMutation.isPending}
        />
      )}
    </div>
  );
};

export default ElectionManagementPage;
