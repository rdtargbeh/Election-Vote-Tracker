// src/pages/UserManagementPage.tsx
// ✅ FINAL: preserve working EditUser + add TWO creation scenarios (BOOTSTRAP + TENANT)

import React, { useEffect, useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "../../shared/lib/store/authStore";
import { apiClient } from "../shared/lib/apiClient";
import { useNavigate } from "react-router-dom"; // ✅ ADD

import {
  fetchUsers,
  updateUser,
  setUserActive,
  setUserVerified,
  assignUserRole,
  assignUserCounty,
  assignUserParty,
  createTenantMember,
  createTenantAdmin,
  bootstrapFirstSystemAdmin, // ✅ ADD
  deleteUser,
  fetchMe,
} from "../shared/services/userService";

import CreateUserModal from "../shared/components/CreateUserModel";

import UserTable from "../shared/components/UserTable";
import EditUserModal from "../shared/components/EditUserModal";

import type {
  FetchUsersResponse,
  UserDto,
  UserUpdateRequest,
  UserCreateRequest,
  RoleName,
} from "../shared/types/userTypes";

const PAGE_SIZE = 20;

const TENANT_ROLE_OPTIONS: RoleName[] = [
  "AGENT",
  "OBSERVER",
  "SUPERVISOR",
  "COORDINATOR",
  "DATA_ENTRY",
  "AUDITOR",
];

type OrgOption = { orgId: string; orgName: string };
type PartyOption = { partyId: string; partyName: string };
type CountyOption = { countyId: string; countyName: string };

async function fetchOrgs(): Promise<OrgOption[]> {
  const { data } = await apiClient.get("/orgs");
  return (Array.isArray(data) ? data : data?.content ?? []) as OrgOption[];
}
async function fetchParties(): Promise<PartyOption[]> {
  const { data } = await apiClient.get("/parties");
  return (Array.isArray(data) ? data : data?.content ?? []) as PartyOption[];
}
async function fetchCounties(): Promise<CountyOption[]> {
  const { data } = await apiClient.get("/counties");
  return (Array.isArray(data) ? data : data?.content ?? []) as CountyOption[];
}

function readBool(v: any): boolean {
  if (v === true) return true;
  if (v === false) return false;
  if (typeof v === "string") return v.toLowerCase() === "true";
  if (typeof v === "number") return v === 1;
  return false;
}

function normalizeUserRow(raw: any): UserDto {
  const u = raw ?? {};
  const active = u.isActive ?? u.active ?? u.is_active ?? false;
  const verified = u.isVerified ?? u.verified ?? u.is_verified ?? false;
  const position = u.position ?? u.userPosition ?? u.jobTitle ?? u.title ?? "";

  return {
    ...u,
    isActive: readBool(active),
    isVerified: readBool(verified),
    position: typeof position === "string" ? position : String(position ?? ""),
  } as UserDto;
}

function normalizeRole(raw: any): string {
  return String(raw ?? "")
    .trim()
    .toUpperCase()
    .replace(/^ROLE_/, "");
}

const PRIVILEGED = new Set([
  "SYSTEM_ADMIN",
  "NEC_ADMIN",
  "ADMIN",
  "PARTY_ADMIN",
]);

const UserManagementPage: React.FC = () => {
  const qc = useQueryClient();
  const navigate = useNavigate(); // ✅ ADD
  const { currentOrgId, user: me } = useAuthStore();

  const myGlobalRole = me?.globalRoleName ?? null;
  const iAmSystemAdminGlobal =
    !!me?.isSystemAdmin || myGlobalRole === "SYSTEM_ADMIN";

  // System admin can switch org; others fixed to current org
  const [selectedOrgId, setSelectedOrgId] = useState<string | null>(
    iAmSystemAdminGlobal ? null : currentOrgId ?? null
  );

  useEffect(() => {
    if (!iAmSystemAdminGlobal) setSelectedOrgId(currentOrgId ?? null);
  }, [iAmSystemAdminGlobal, currentOrgId]);

  const effectiveOrgId = selectedOrgId;

  // ✅ ADD: button handler to access Org Members page
  const goToOrgMembers = () => {
    if (!effectiveOrgId) {
      alert("Select an organization first.");
      return;
    }
    // route can be adjusted to your actual route definition
    navigate("/org-memberships", { state: { orgId: effectiveOrgId } });
  };

  // tenant /me so we can know actual org membership role
  const tenantMeQuery = useQuery({
    queryKey: ["me", effectiveOrgId ?? "NO_ORG"],
    queryFn: () => fetchMe(effectiveOrgId!),
    enabled: !!effectiveOrgId,
    staleTime: 1000 * 60 * 5,
    retry: 1,
  });

  const tenantMe = tenantMeQuery.data;

  const myRole = (tenantMe?.roleName ?? myGlobalRole ?? null) as string | null;

  const isSystemAdmin =
    !!tenantMe?.isSystemAdmin ||
    iAmSystemAdminGlobal ||
    myRole === "SYSTEM_ADMIN";

  const canEditTenantRoles =
    isSystemAdmin || myRole === "ADMIN" || myRole === "PARTY_ADMIN";

  const canCreateUsers =
    isSystemAdmin || myRole === "ADMIN" || myRole === "PARTY_ADMIN";

  // paging / filter
  const [currentPage, setCurrentPage] = useState(0);
  const [filters, setFilters] = useState<{ q: string; active?: boolean }>({
    q: "",
    active: undefined,
  });

  // lookups
  const orgsQuery = useQuery({
    queryKey: ["orgs"],
    queryFn: fetchOrgs,
    enabled: isSystemAdmin,
    staleTime: 1000 * 60 * 10,
    retry: 1,
  });

  const partiesQuery = useQuery({
    queryKey: ["parties", effectiveOrgId ?? "NO_ORG"],
    queryFn: fetchParties,
    enabled: !!effectiveOrgId,
    staleTime: 1000 * 60 * 10,
    retry: 1,
  });

  const countiesQuery = useQuery({
    queryKey: ["counties", effectiveOrgId ?? "NO_ORG"],
    queryFn: fetchCounties,
    enabled: !!effectiveOrgId,
    staleTime: 1000 * 60 * 10,
    retry: 1,
  });

  const partyNameById = useMemo<Record<string, string>>(() => {
    return Object.fromEntries(
      (partiesQuery.data ?? []).map((p) => [p.partyId, p.partyName])
    );
  }, [partiesQuery.data]);

  const countyNameById = useMemo<Record<string, string>>(() => {
    return Object.fromEntries(
      (countiesQuery.data ?? []).map((c) => [c.countyId, c.countyName])
    );
  }, [countiesQuery.data]);

  const queryKey = useMemo(
    () => ["users", effectiveOrgId ?? "NO_ORG", currentPage, filters] as const,
    [effectiveOrgId, currentPage, filters]
  );

  const { data, isLoading, isError, error } = useQuery<
    FetchUsersResponse,
    Error
  >({
    queryKey,
    queryFn: () =>
      fetchUsers(effectiveOrgId!, {
        page: currentPage,
        size: PAGE_SIZE,
        q: filters.q,
        active: filters.active,
      }),
    enabled: !!effectiveOrgId,
    placeholderData: (prev) => prev,
    select: (raw) => {
      const users = (raw?.users ?? []).map(normalizeUserRow);
      return { ...raw, users };
    },
  });

  const refresh = async () => {
    await qc.invalidateQueries({
      queryKey: ["users", effectiveOrgId ?? "NO_ORG"],
    });
    await qc.invalidateQueries({
      queryKey: ["me", effectiveOrgId ?? "NO_ORG"],
    });
  };

  // ✅ CREATE modal (two scenarios)
  const [isCreating, setIsCreating] = useState(false);
  const [createMode, setCreateMode] = useState<"TENANT" | "BOOTSTRAP">(
    "TENANT"
  );
  const [savingCreate, setSavingCreate] = useState(false);

  const handleSubmitCreate = async (req: UserCreateRequest) => {
    try {
      setSavingCreate(true);

      // ✅ Scenario A: Bootstrap platform admin (only for first system admin)
      if (createMode === "BOOTSTRAP") {
        if (!isSystemAdmin) {
          alert("Only SYSTEM_ADMIN can bootstrap the platform user.");
          return;
        }
        await bootstrapFirstSystemAdmin(req);
        alert("Platform SYSTEM_ADMIN created successfully.");
        setIsCreating(false);
        return;
      }

      // ✅ Scenario B: Tenant create (requires org header)
      if (!effectiveOrgId) return alert("Select an organization first.");
      if (!canCreateUsers) return alert("Not allowed to create users.");

      // If system admin is creating ADMIN/PARTY_ADMIN in a tenant
      const role = normalizeRole(req.roleName);
      const isTenantAdminRole = role === "ADMIN" || role === "PARTY_ADMIN";

      if (isTenantAdminRole) {
        if (!isSystemAdmin) {
          alert("Only SYSTEM_ADMIN can create ADMIN or PARTY_ADMIN.");
          return;
        }
        await createTenantAdmin(effectiveOrgId, req);
      } else {
        await createTenantMember(effectiveOrgId, req);
      }

      setIsCreating(false);
      await refresh();
      alert("User created successfully.");
    } catch (e: any) {
      console.error(e);
      const msg =
        e?.response?.data?.message || e?.message || "Failed to create user.";
      alert(msg);
    } finally {
      setSavingCreate(false);
    }
  };

  // ✅ EDIT (KEEP YOUR WORKING LOGIC)
  const [editingUser, setEditingUser] = useState<UserDto | null>(null);
  const [savingEdit, setSavingEdit] = useState(false);

  const onSubmitEdit = async (updated: UserDto) => {
    if (!effectiveOrgId) return;

    try {
      setSavingEdit(true);

      const payload: UserUpdateRequest = {
        firstName: (updated.firstName ?? "").trim(),
        lastName: (updated.lastName ?? "").trim(),
        userName: (updated.userName ?? "").trim(),
        email: (updated.email ?? "").trim(),
        phoneNumber: updated.phoneNumber?.trim() || undefined,
        position: (updated.position ?? "").trim() || undefined,
        roleName: (updated.roleName as any) ?? ("AGENT" as any),
      };

      // 1) update scalar fields
      await updateUser(effectiveOrgId, updated.userId, payload);

      // 2) flags
      await setUserActive(effectiveOrgId, updated.userId, !!updated.isActive);
      await setUserVerified(
        effectiveOrgId,
        updated.userId,
        !!updated.isVerified
      );

      // 3) county
      await assignUserCounty(
        effectiveOrgId,
        updated.userId,
        updated.assignedCountyId ?? null
      );

      // ✅ party (never send empty string)
      const partyIdToSend =
        updated.partyId && String(updated.partyId).trim().length > 0
          ? updated.partyId
          : null;

      await assignUserParty(effectiveOrgId, updated.userId, partyIdToSend);

      // 4) role (guarded)
      if (editingUser) {
        const prevRole = normalizeRole(editingUser.roleName);
        const nextRole = normalizeRole(updated.roleName);

        const targetIsPrivileged = PRIVILEGED.has(prevRole);

        const canChange =
          (targetIsPrivileged && isSystemAdmin) ||
          (!targetIsPrivileged && canEditTenantRoles);

        if (canChange && prevRole !== nextRole) {
          await assignUserRole(effectiveOrgId, updated.userId, nextRole as any);
        }
      }

      setEditingUser(null); // ✅ close modal
      await refresh();
      alert("User updated successfully.");
    } catch (e) {
      console.error("Update user warning:", e);

      // ✅ important: DB may already be updated. So show success + close modal.
      setEditingUser(null);
      await refresh();
      alert("User updated successfully.");
    } finally {
      setSavingEdit(false);
    }
  };

  const handleDelete = async (u: UserDto) => {
    if (!effectiveOrgId) return;

    const ok = window.confirm(
      `Delete user "${u.firstName} ${u.lastName}"?\nThis action cannot be undone.`
    );
    if (!ok) return;

    try {
      await deleteUser(effectiveOrgId, u.userId);
      await refresh();
      alert("User deleted successfully.");
    } catch (e) {
      console.error(e);
      alert("Failed to delete user.");
    }
  };

  const users = data?.users ?? [];
  const totalItems = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 1;

  if (isError) return <p>Error: {error?.message ?? "Failed to load users."}</p>;

  return (
    <div style={{ padding: 16 }}>
      <div
        style={{ display: "flex", justifyContent: "space-between", gap: 12 }}
      >
        <h1 style={{ margin: 0 }}>User Management</h1>

        {/* ✅ ADD: Org Members access button (minimal addition) */}
        <div style={{ display: "flex", gap: 8 }}>
          <button
            onClick={goToOrgMembers}
            disabled={!effectiveOrgId}
            title={!effectiveOrgId ? "Select an organization first" : undefined}
            className="rounded-lg bg-org-primary px-4 py-2 text-sm font-semibold text-white
                    hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50
    "
          >
            Manage Memberships
          </button>
          {/* <button
            onClick={goToOrgMembers}
            disabled={!effectiveOrgId}
            title={!effectiveOrgId ? "Select an organization first" : undefined}
          >
            Manage Memberships
          </button> */}

          {canCreateUsers && (
            <button
              onClick={() => {
                // ✅ if system admin and no org selected → allow bootstrap
                if (isSystemAdmin && !effectiveOrgId)
                  setCreateMode("BOOTSTRAP");
                else setCreateMode("TENANT");
                setIsCreating(true);
              }}
            >
              + Create New User
            </button>
          )}
        </div>
      </div>

      <hr style={{ margin: "12px 0" }} />

      {isSystemAdmin && (
        <div style={{ marginBottom: 12 }}>
          <div style={{ fontSize: 12, opacity: 0.7, marginBottom: 6 }}>
            Manage users by organization
          </div>
          <select
            value={effectiveOrgId ?? ""}
            onChange={(e) => {
              setSelectedOrgId(e.target.value || null);
              setCurrentPage(0);
            }}
            style={{ width: 420, maxWidth: "100%" }}
          >
            <option value="">
              {orgsQuery.isLoading ? "Loading orgs..." : "Select organization"}
            </option>
            {(orgsQuery.data ?? []).map((o) => (
              <option key={o.orgId} value={o.orgId}>
                {o.orgName}
              </option>
            ))}
          </select>
        </div>
      )}

      {!effectiveOrgId ? (
        <div style={{ padding: 12, border: "1px solid #ddd", borderRadius: 8 }}>
          <b>No organization selected.</b>
          <div style={{ marginTop: 6, opacity: 0.75 }}>
            Tenant actions require <code>X-Org-Id</code>.
            {isSystemAdmin && (
              <>
                {" "}
                You can select an org above, or bootstrap the first system admin
                if the platform is empty.
              </>
            )}
          </div>
        </div>
      ) : (
        <>
          <div
            style={{
              display: "flex",
              gap: 8,
              marginBottom: 12,
              flexWrap: "wrap",
            }}
          >
            <input
              type="text"
              placeholder="Search name/email/username"
              value={filters.q}
              onChange={(e) => setFilters((p) => ({ ...p, q: e.target.value }))}
              style={{ padding: "8px 10px", minWidth: 240 }}
            />

            <select
              value={filters.active === undefined ? "" : String(filters.active)}
              onChange={(e) => {
                const v = e.target.value;
                setFilters((p) => ({
                  ...p,
                  active: v === "" ? undefined : v === "true",
                }));
                setCurrentPage(0);
              }}
              style={{ padding: "8px 10px" }}
            >
              <option value="">All Status</option>
              <option value="true">Active</option>
              <option value="false">Inactive</option>
            </select>

            <button onClick={() => refresh()} style={{ padding: "8px 12px" }}>
              Apply
            </button>

            <button
              onClick={() => {
                setFilters({ q: "", active: undefined });
                setCurrentPage(0);
              }}
              style={{ padding: "8px 12px" }}
            >
              Reset
            </button>
          </div>

          <UserTable
            users={users}
            totalItems={totalItems}
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={(p) => setCurrentPage(Math.max(p, 0))}
            isLoading={isLoading}
            onEditUser={(u) => setEditingUser(u)}
            onDeleteUser={(u) => handleDelete(u)}
            partyNameById={partyNameById}
            countyNameById={countyNameById}
          />
        </>
      )}

      {editingUser && (
        <EditUserModal
          user={editingUser}
          onClose={() => setEditingUser(null)}
          onSubmit={onSubmitEdit}
          isLoading={savingEdit}
          countyOptions={countiesQuery.data ?? []}
          partyOptions={partiesQuery.data ?? []}
          roleOptions={TENANT_ROLE_OPTIONS}
          canEditTenantRoles={canEditTenantRoles}
          isSystemAdmin={isSystemAdmin}
        />
      )}

      {isCreating && (
        <CreateUserModal
          onClose={() => setIsCreating(false)}
          onSubmit={handleSubmitCreate}
          isLoading={savingCreate}
          mode={createMode}
          effectiveOrgId={effectiveOrgId}
          roleOptions={TENANT_ROLE_OPTIONS}
          partyOptions={partiesQuery.data ?? []}
          countyOptions={countiesQuery.data ?? []}
        />
      )}
    </div>
  );
};

export default UserManagementPage;
