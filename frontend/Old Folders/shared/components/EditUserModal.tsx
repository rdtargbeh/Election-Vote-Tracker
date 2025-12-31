// src/shared/components/EditUserModal.tsx
// ✅ FINAL CHANGE: ensure party selection writes partyId as uuid or null (never empty string)

import React, { useEffect, useMemo, useState } from "react";
import type { RoleName, UserDto } from "../types/userTypes";

type CountyOption = { countyId: string; countyName: string };
type PartyOption = { partyId: string; partyName: string };

interface EditUserModalProps {
  user: UserDto;
  onClose: () => void;
  onSubmit: (updatedUser: UserDto) => void | Promise<void>;
  isLoading: boolean;

  countyOptions: CountyOption[];
  partyOptions: PartyOption[];

  roleOptions: RoleName[];
  canEditTenantRoles: boolean;
  isSystemAdmin: boolean;
}

function normalizeRole(raw: any): string | null {
  if (!raw) return null;
  const s = String(raw).trim().toUpperCase();
  return s.startsWith("ROLE_") ? s.replace("ROLE_", "") : s;
}

const PRIVILEGED_ROLES = new Set<string>([
  "SYSTEM_ADMIN",
  "NEC_ADMIN",
  "ADMIN",
  "PARTY_ADMIN",
]);

const EditUserModal: React.FC<EditUserModalProps> = ({
  user,
  onClose,
  onSubmit,
  isLoading,
  countyOptions,
  partyOptions,
  roleOptions,
  canEditTenantRoles,
  isSystemAdmin,
}) => {
  const [edited, setEdited] = useState<UserDto>({ ...user });

  useEffect(() => setEdited({ ...user }), [user]);

  const onChangeText = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setEdited((p) => ({ ...p, [name]: value }));
  };

  const onToggle = (key: "isActive" | "isVerified") => {
    setEdited((p) => ({ ...p, [key]: !p[key] }));
  };

  const dbRole = useMemo(() => normalizeRole(user.roleName), [user.roleName]);
  const editedRole = useMemo(
    () => normalizeRole((edited as any).roleName),
    [edited]
  );

  const targetIsPrivileged = !!dbRole && PRIVILEGED_ROLES.has(dbRole);
  const roleDisabled = targetIsPrivileged
    ? !isSystemAdmin
    : !canEditTenantRoles;

  const effectiveRoleValue: string = editedRole || dbRole || "AGENT";

  const safeTenantRoleValue: RoleName = roleOptions.includes(
    effectiveRoleValue as any
  )
    ? (effectiveRoleValue as any)
    : ("AGENT" as RoleName);

  const selectRoleValue = targetIsPrivileged
    ? dbRole ?? "AGENT"
    : safeTenantRoleValue;

  const submit = () => {
    if (
      !edited.firstName?.trim() ||
      !edited.lastName?.trim() ||
      !edited.email?.trim()
    ) {
      alert("First name, last name and email are required.");
      return;
    }

    if (targetIsPrivileged && !isSystemAdmin) {
      onSubmit({ ...edited, roleName: (dbRole as any) ?? edited.roleName });
      return;
    }

    if (!canEditTenantRoles && !isSystemAdmin) {
      onSubmit({ ...edited, roleName: (dbRole as any) ?? edited.roleName });
      return;
    }

    onSubmit({
      ...edited,
      roleName: (selectRoleValue as any) ?? edited.roleName,
    });
  };

  const hasParties = (partyOptions?.length ?? 0) > 0;

  return (
    <div
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(0,0,0,.35)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: 16,
        zIndex: 9999,
      }}
    >
      <div
        style={{
          background: "#fff",
          padding: 16,
          borderRadius: 10,
          width: 560,
          maxWidth: "100%",
        }}
      >
        <h2 style={{ marginTop: 0 }}>
          Edit User: {user.firstName} {user.lastName}
        </h2>

        <div style={{ display: "grid", gap: 10 }}>
          <label>
            First Name
            <input
              name="firstName"
              value={edited.firstName ?? ""}
              onChange={onChangeText}
              style={{ width: "100%" }}
            />
          </label>

          <label>
            Last Name
            <input
              name="lastName"
              value={edited.lastName ?? ""}
              onChange={onChangeText}
              style={{ width: "100%" }}
            />
          </label>

          <label>
            Email
            <input
              name="email"
              value={edited.email ?? ""}
              onChange={onChangeText}
              style={{ width: "100%" }}
            />
          </label>

          <label>
            Phone
            <input
              name="phoneNumber"
              value={edited.phoneNumber ?? ""}
              onChange={onChangeText}
              style={{ width: "100%" }}
            />
          </label>

          <label>
            Position
            <input
              name="position"
              value={edited.position ?? ""}
              onChange={onChangeText}
              style={{ width: "100%" }}
            />
          </label>

          {/* ✅ Party dropdown (optional, tenant-scoped) */}
          <label>
            Party (optional)
            <select
              value={edited.partyId ?? ""}
              onChange={(e) =>
                setEdited((p) => ({
                  ...p,
                  partyId: e.target.value ? e.target.value : null, // ✅ NULL when "None"
                }))
              }
              disabled={!hasParties}
              style={{ width: "100%" }}
            >
              <option value="">None</option>
              {partyOptions.map((p) => (
                <option key={p.partyId} value={p.partyId}>
                  {p.partyName}
                </option>
              ))}
            </select>
            {!hasParties && (
              <div style={{ fontSize: 12, opacity: 0.75, marginTop: 4 }}>
                This organization has no parties configured.
              </div>
            )}
          </label>

          {/* ✅ Role dropdown */}
          <label>
            Role
            <select
              value={selectRoleValue}
              disabled={roleDisabled}
              onChange={(e) =>
                setEdited((p) => ({ ...p, roleName: e.target.value as any }))
              }
              style={{ width: "100%" }}
            >
              {targetIsPrivileged && dbRole && (
                <option value={dbRole}>{dbRole}</option>
              )}
              {roleOptions.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
            {targetIsPrivileged && !isSystemAdmin && (
              <div style={{ fontSize: 12, opacity: 0.75, marginTop: 4 }}>
                This user’s role ({dbRole}) can only be changed by SYSTEM_ADMIN.
              </div>
            )}
          </label>

          {/* ✅ County */}
          <label>
            Assign County
            <select
              value={edited.assignedCountyId ?? ""}
              onChange={(e) =>
                setEdited((p) => ({
                  ...p,
                  assignedCountyId: e.target.value || null,
                }))
              }
              style={{ width: "100%" }}
            >
              <option value="">None</option>
              {countyOptions.map((c) => (
                <option key={c.countyId} value={c.countyId}>
                  {c.countyName}
                </option>
              ))}
            </select>
          </label>

          {/* ✅ Active / Verified */}
          <div style={{ display: "flex", gap: 18, marginTop: 6 }}>
            <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
              <input
                type="checkbox"
                checked={!!edited.isActive}
                onChange={() => onToggle("isActive")}
              />
              Active
            </label>

            <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
              <input
                type="checkbox"
                checked={!!edited.isVerified}
                onChange={() => onToggle("isVerified")}
              />
              Verified
            </label>
          </div>
        </div>

        <div
          style={{
            display: "flex",
            gap: 8,
            justifyContent: "flex-end",
            marginTop: 14,
          }}
        >
          <button onClick={onClose} disabled={isLoading}>
            Cancel
          </button>
          <button onClick={submit} disabled={isLoading}>
            {isLoading ? "Saving..." : "Save"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default EditUserModal;
