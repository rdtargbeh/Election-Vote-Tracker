import React, { useEffect, useMemo, useState } from "react";
import type { RoleName, UserCreateRequest } from "../types/userTypes";

type PartyOption = { partyId: string; partyName: string };
type CountyOption = { countyId: string; countyName: string };

interface Props {
  onClose: () => void;
  onSubmit: (req: UserCreateRequest) => void | Promise<void>;
  isLoading: boolean;

  // ✅ creation scenario
  mode: "TENANT" | "BOOTSTRAP";

  // ✅ tenant requires org
  effectiveOrgId?: string | null;

  // dropdowns
  roleOptions: RoleName[];
  partyOptions: PartyOption[];
  countyOptions: CountyOption[];
}

const CreateUserModal: React.FC<Props> = ({
  onClose,
  onSubmit,
  isLoading,
  mode,
  effectiveOrgId,
  roleOptions,
  partyOptions,
  countyOptions,
}) => {
  const hasParties = (partyOptions?.length ?? 0) > 0;

  const defaultRole = useMemo<RoleName>(() => {
    if (roleOptions.includes("AGENT" as RoleName)) return "AGENT";
    return (roleOptions[0] as RoleName) ?? "AGENT";
  }, [roleOptions]);

  const [form, setForm] = useState<UserCreateRequest>({
    firstName: "",
    lastName: "",
    userName: "",
    email: "",
    password: "",
    phoneNumber: "",
    position: "",

    roleName: defaultRole,

    partyId: null,
    assignedCountyId: null,
    defaultOrgId: null,

    profileImageUrl: undefined,
    profileImageUploadId: undefined,
  });

  useEffect(() => {
    setForm((p) => ({ ...p, roleName: p.roleName || defaultRole }));
  }, [defaultRole]);

  const set = (k: keyof UserCreateRequest, v: any) =>
    setForm((p) => ({ ...p, [k]: v }));

  const submit = async () => {
    // ✅ tenant create must have org id
    if (mode === "TENANT" && !effectiveOrgId) {
      alert(
        "Select an organization first. Tenant user creation requires X-Org-Id."
      );
      return;
    }

    if (!form.firstName?.trim() || !form.lastName?.trim()) {
      alert("First name and last name are required.");
      return;
    }
    if (!form.userName?.trim()) {
      alert("Username is required.");
      return;
    }
    if (!form.email?.trim()) {
      alert("Email is required.");
      return;
    }
    if (!form.password || form.password.length < 8) {
      alert("Password is required (min 8 characters).");
      return;
    }
    if (!form.roleName) {
      alert("Role is required.");
      return;
    }

    // ✅ clean payload (never send empty string for UUIDs)
    const payload: UserCreateRequest = {
      ...form,
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      userName: form.userName.trim(),
      email: form.email.trim(),
      phoneNumber: form.phoneNumber?.trim() || undefined,
      position: form.position?.trim() || undefined,

      partyId:
        form.partyId && String(form.partyId).trim()
          ? String(form.partyId)
          : null,
      assignedCountyId:
        form.assignedCountyId && String(form.assignedCountyId).trim()
          ? String(form.assignedCountyId)
          : null,
      defaultOrgId: null, // keep null; tenant membership controls org linkage
    };

    await onSubmit(payload);
  };

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
          {mode === "BOOTSTRAP"
            ? "Create First System Admin (Bootstrap)"
            : "Create User"}
        </h2>

        {mode === "TENANT" && !effectiveOrgId && (
          <div
            style={{
              padding: 10,
              border: "1px solid #f0c36d",
              background: "#fff7e6",
              borderRadius: 8,
              marginBottom: 10,
              fontSize: 13,
            }}
          >
            <b>No organization selected.</b> Tenant user creation requires{" "}
            <code>X-Org-Id</code>.
          </div>
        )}

        <div style={{ display: "grid", gap: 10 }}>
          <label>
            First Name
            <input
              value={form.firstName}
              onChange={(e) => set("firstName", e.target.value)}
              style={{ width: "100%" }}
            />
          </label>

          <label>
            Last Name
            <input
              value={form.lastName}
              onChange={(e) => set("lastName", e.target.value)}
              style={{ width: "100%" }}
            />
          </label>

          <label>
            Username
            <input
              value={form.userName}
              onChange={(e) => set("userName", e.target.value)}
              style={{ width: "100%" }}
            />
          </label>

          <label>
            Email
            <input
              value={form.email}
              onChange={(e) => set("email", e.target.value)}
              style={{ width: "100%" }}
            />
          </label>

          <label>
            Password
            <input
              type="password"
              value={form.password}
              onChange={(e) => set("password", e.target.value)}
              style={{ width: "100%" }}
            />
          </label>

          <label>
            Phone (optional)
            <input
              value={form.phoneNumber ?? ""}
              onChange={(e) => set("phoneNumber", e.target.value)}
              style={{ width: "100%" }}
            />
          </label>

          <label>
            Position (optional)
            <input
              value={form.position ?? ""}
              onChange={(e) => set("position", e.target.value)}
              style={{ width: "100%" }}
            />
          </label>

          <label>
            Role
            <select
              value={form.roleName}
              onChange={(e) => set("roleName", e.target.value as RoleName)}
              style={{ width: "100%" }}
            >
              {roleOptions.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
          </label>

          {/* ✅ Tenant-only optional fields */}
          {mode === "TENANT" && (
            <>
              <label>
                Party (optional)
                <select
                  value={form.partyId ?? ""}
                  onChange={(e) => set("partyId", e.target.value || null)}
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

              <label>
                Assign County (optional)
                <select
                  value={form.assignedCountyId ?? ""}
                  onChange={(e) =>
                    set("assignedCountyId", e.target.value || null)
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
            </>
          )}
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
            {isLoading ? "Creating..." : "Create"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default CreateUserModal;
