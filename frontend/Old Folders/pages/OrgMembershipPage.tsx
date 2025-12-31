// src/pages/OrgMembershipPage.tsx

import React, { useMemo, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useLocation } from "react-router-dom";
import { useAuthStore } from "../../shared/lib/store/authStore";
import { fetchMe } from "../shared/services/userService";

import {
  fetchOrgMembers,
  toggleOrgMembership,
  assignOrgMembershipRole,
  removeOrgMember,
} from "../shared/services/orgMembershipService";

import type { OrgMembershipDto } from "../shared/types/userTypes";

const PAGE_SIZE = 20;

const ROLE_OPTIONS = [
  "ADMIN",
  "AGENT",
  "OBSERVER",
  "SUPERVISOR",
  "COORDINATOR",
  "DATA_ENTRY",
  "AUDITOR",
];

const normalizeRole = (raw: any) =>
  String(raw ?? "")
    .trim()
    .toUpperCase()
    .replace(/^ROLE_/, "");

const displayName = (m: any) => {
  const full = String(m.fullName ?? "").trim();
  const first = String(m.firstName ?? "").trim();
  const last = String(m.lastName ?? "").trim();
  const uname = String(m.userName ?? m.username ?? "").trim();
  const email = String(m.email ?? "").trim();

  if (full) return full;
  if (first || last) return `${first} ${last}`.trim();
  if (uname) return uname;
  if (email) return email;
  return m.userId;
};

const OrgMembershipPage: React.FC = () => {
  const queryClient = useQueryClient();
  const location = useLocation();
  const { currentOrgId, user: me } = useAuthStore();

  // orgId from navigation state OR store fallback
  const orgId = (location.state as any)?.orgId ?? currentOrgId ?? null;

  const [currentPage, setCurrentPage] = useState(0);
  const [filters, setFilters] = useState({
    search: "",
    roleName: "",
    enabled: undefined as boolean | undefined,
  });

  // ✅ Row edit state
  const [editingUserId, setEditingUserId] = useState<string | null>(null);
  const [draftRole, setDraftRole] = useState<string>("");

  // tenant /me for org-level role checks
  const tenantMeQuery = useQuery({
    queryKey: ["me", orgId ?? "NO_ORG"],
    queryFn: () => fetchMe(orgId!),
    enabled: !!orgId,
    staleTime: 1000 * 60 * 5,
    retry: 1,
  });

  const tenantMe = tenantMeQuery.data;

  const myTenantRole = normalizeRole(tenantMe?.roleName);
  const isSystemAdmin =
    !!tenantMe?.isSystemAdmin ||
    normalizeRole(me?.globalRoleName) === "SYSTEM_ADMIN";

  // ✅ Role change allowed by ADMIN / PARTY_ADMIN (plus SYSTEM_ADMIN bypass)
  const canEditRoles =
    isSystemAdmin || myTenantRole === "ADMIN" || myTenantRole === "PARTY_ADMIN";

  // ✅ Enable/Disable also allowed by ADMIN / PARTY_ADMIN (plus SYSTEM_ADMIN)
  const canToggleEnabled =
    isSystemAdmin || myTenantRole === "ADMIN" || myTenantRole === "PARTY_ADMIN";

  // ✅ Remove allowed by PARTY_ADMIN / NEC_ADMIN / SYSTEM_ADMIN (per your backend)
  const canRemoveMember =
    isSystemAdmin ||
    myTenantRole === "PARTY_ADMIN" ||
    myTenantRole === "NEC_ADMIN";

  const isPartyAdminMember = (m: any) =>
    normalizeRole(m.roleName) === "PARTY_ADMIN";

  const queryKey = useMemo(
    () => ["orgMembers", orgId ?? "NO_ORG", currentPage, filters] as const,
    [orgId, currentPage, filters]
  );

  const { data, isLoading, isError, error } = useQuery({
    queryKey,
    queryFn: () =>
      fetchOrgMembers(orgId!, {
        page: currentPage,
        size: PAGE_SIZE,
        search: filters.search,
        roleName: filters.roleName,
        enabled: filters.enabled,
      }),
    enabled: !!orgId,
    staleTime: 5 * 60 * 1000,
    placeholderData: (prev) => prev,
  });

  const invalidateMembers = async () => {
    await queryClient.invalidateQueries({
      queryKey: ["orgMembers", orgId ?? "NO_ORG"],
      exact: false,
    });
  };

  const toggleMembershipMutation = useMutation({
    mutationFn: ({ userId, enabled }: { userId: string; enabled: boolean }) =>
      toggleOrgMembership(orgId!, userId, enabled),
    onSuccess: invalidateMembers,
    onError: (err: any) => {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        "Failed to update member status.";
      alert(msg);
      console.error(err);
    },
  });

  const assignRoleMutation = useMutation({
    mutationFn: ({ userId, roleName }: { userId: string; roleName: string }) =>
      assignOrgMembershipRole(orgId!, userId, normalizeRole(roleName)),
    onSuccess: async () => {
      setEditingUserId(null);
      setDraftRole("");
      await invalidateMembers();
    },
    onError: (err: any) => {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        "Failed to assign role.";
      alert(msg);
      console.error(err);
    },
  });

  const removeMemberMutation = useMutation({
    mutationFn: ({ userId }: { userId: string }) =>
      removeOrgMember(orgId!, userId),
    onSuccess: invalidateMembers,
    onError: (err: any) => {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        "Failed to remove member.";
      alert(msg);
      console.error(err);
    },
  });

  if (!orgId) {
    return (
      <div style={{ padding: 16 }}>
        <h1>Organization Members</h1>
        <div style={{ padding: 12, border: "1px solid #ddd", borderRadius: 8 }}>
          <b>No organization selected.</b>
          <div style={{ marginTop: 6, opacity: 0.75 }}>
            This page requires an organization context (<code>X-Org-Id</code>).
          </div>
        </div>
      </div>
    );
  }

  if (isLoading || tenantMeQuery.isLoading)
    return <div>Loading members...</div>;
  if (isError) return <div>Error fetching members: {error.message}</div>;

  const members = (data?.members ?? []) as OrgMembershipDto[];
  const total = data?.totalElements ?? 0;
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <div style={{ padding: 16 }}>
      <h1 style={{ marginTop: 0 }}>Organization Members</h1>

      {/* Filters */}
      <div
        style={{ display: "flex", gap: 10, flexWrap: "wrap", marginBottom: 12 }}
      >
        <input
          type="text"
          placeholder="Search name/email/username"
          value={filters.search}
          onChange={(e) => {
            setFilters({ ...filters, search: e.target.value });
            setCurrentPage(0);
          }}
          style={{ padding: "8px 10px", minWidth: 240 }}
        />

        <select
          value={filters.roleName}
          onChange={(e) => {
            setFilters({ ...filters, roleName: e.target.value });
            setCurrentPage(0);
          }}
          style={{ padding: "8px 10px" }}
        >
          <option value="">All Roles</option>
          <option value="ADMIN">ADMIN</option>
          <option value="PARTY_ADMIN">PARTY_ADMIN</option>
          <option value="AGENT">AGENT</option>
          <option value="OBSERVER">OBSERVER</option>
          <option value="SUPERVISOR">SUPERVISOR</option>
          <option value="COORDINATOR">COORDINATOR</option>
          <option value="DATA_ENTRY">DATA_ENTRY</option>
          <option value="AUDITOR">AUDITOR</option>
        </select>

        <select
          value={filters.enabled === undefined ? "" : String(filters.enabled)}
          onChange={(e) => {
            setFilters({
              ...filters,
              enabled:
                e.target.value === "" ? undefined : e.target.value === "true",
            });
            setCurrentPage(0);
          }}
          style={{ padding: "8px 10px" }}
        >
          <option value="">All Status</option>
          <option value="true">Enabled</option>
          <option value="false">Disabled</option>
        </select>

        <button onClick={invalidateMembers} style={{ padding: "8px 12px" }}>
          Apply
        </button>

        <button
          onClick={() => {
            setFilters({ search: "", roleName: "", enabled: undefined });
            setCurrentPage(0);
          }}
          style={{ padding: "8px 12px" }}
        >
          Reset
        </button>
      </div>

      {/* Table */}
      <table style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
          <tr style={{ textAlign: "left", borderBottom: "1px solid #ddd" }}>
            <th style={{ padding: 6 }}>Member</th>
            <th style={{ padding: 6 }}>Role</th>
            <th style={{ padding: 6 }}>Status</th>
            <th style={{ padding: 6 }}>Actions</th>
          </tr>
        </thead>

        <tbody>
          {members.map((member: any) => {
            const editing = editingUserId === member.userId;
            const partyLocked = isPartyAdminMember(member);

            return (
              <tr
                key={member.membershipId}
                style={{ borderBottom: "1px solid #f2f2f2" }}
              >
                <td style={{ padding: 8 }}>{displayName(member)}</td>

                <td style={{ padding: 8 }}>
                  {!editing ? (
                    <span
                      title={
                        partyLocked
                          ? "PARTY_ADMIN role cannot be changed."
                          : undefined
                      }
                    >
                      {normalizeRole(member.roleName)}
                    </span>
                  ) : (
                    <select
                      value={draftRole}
                      onChange={(e) =>
                        setDraftRole(normalizeRole(e.target.value))
                      }
                      style={{ padding: "6px 8px" }}
                    >
                      {ROLE_OPTIONS.map((r) => (
                        <option key={r} value={r}>
                          {r}
                        </option>
                      ))}
                    </select>
                  )}
                </td>

                <td style={{ padding: 8 }}>
                  {member.enabled ? "Enabled" : "Disabled"}
                </td>

                <td style={{ padding: 8 }}>
                  <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                    {/* Enable / Disable */}
                    {canToggleEnabled && (
                      <button
                        onClick={() =>
                          toggleMembershipMutation.mutate({
                            userId: member.userId,
                            enabled: !member.enabled,
                          })
                        }
                        disabled={toggleMembershipMutation.isPending}
                        style={{ padding: "6px 10px" }}
                      >
                        {member.enabled ? "Disable" : "Enable"}
                      </button>
                    )}

                    {/* Edit Role */}
                    {canEditRoles && (
                      <>
                        {!editing ? (
                          <button
                            onClick={() => {
                              if (partyLocked) {
                                alert("PARTY_ADMIN role cannot be changed.");
                                return;
                              }
                              setEditingUserId(member.userId);
                              setDraftRole(normalizeRole(member.roleName));
                            }}
                            disabled={partyLocked}
                            title={
                              partyLocked
                                ? "PARTY_ADMIN role cannot be changed."
                                : undefined
                            }
                            style={{ padding: "6px 10px" }}
                          >
                            Edit Role
                          </button>
                        ) : (
                          <>
                            <button
                              onClick={() =>
                                assignRoleMutation.mutate({
                                  userId: member.userId,
                                  roleName: draftRole,
                                })
                              }
                              disabled={
                                assignRoleMutation.isPending || !draftRole
                              }
                              style={{ padding: "6px 10px" }}
                            >
                              Save
                            </button>

                            <button
                              onClick={() => setEditingUserId(null)}
                              style={{ padding: "6px 10px" }}
                            >
                              Cancel
                            </button>
                          </>
                        )}
                      </>
                    )}

                    {/* Remove */}
                    {canRemoveMember && (
                      <button
                        onClick={() => {
                          const name = displayName(member);
                          const ok = window.confirm(
                            `Remove "${name}" from this organization?\n(This does NOT delete the user.)`
                          );
                          if (!ok) return;
                          removeMemberMutation.mutate({
                            userId: member.userId,
                          });
                        }}
                        disabled={removeMemberMutation.isPending}
                        style={{ padding: "6px 10px", color: "red" }}
                      >
                        Remove
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            );
          })}

          {members.length === 0 && (
            <tr>
              <td colSpan={4} style={{ padding: 12, opacity: 0.7 }}>
                No members found.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {/* Pagination */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          marginTop: 12,
        }}
      >
        <div style={{ fontSize: 12, opacity: 0.75 }}>
          Total: {total} • Page {currentPage + 1} of {totalPages}
        </div>

        <div style={{ display: "flex", gap: 8 }}>
          <button
            onClick={() => setCurrentPage((p) => Math.max(p - 1, 0))}
            disabled={currentPage === 0}
            style={{ padding: "6px 10px" }}
          >
            Prev
          </button>
          <button
            onClick={() =>
              setCurrentPage((p) => Math.min(p + 1, totalPages - 1))
            }
            disabled={currentPage >= totalPages - 1}
            style={{ padding: "6px 10px" }}
          >
            Next
          </button>
        </div>
      </div>
    </div>
  );
};

export default OrgMembershipPage;
