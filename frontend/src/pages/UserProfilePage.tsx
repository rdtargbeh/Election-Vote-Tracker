// src/pages/UserProfilePage.tsx
//
// ✅ Read-only profile view + "Edit Profile" mode
// ✅ Active/Verified always reflect DB values
// ✅ Active/Verified/Role/Party/County/Default Org are READ-ONLY (admin-managed)
// ✅ Position added to view + edit + update payload
// ✅ Uses React Query `select()` to normalize response (prevents “missing position” due to mapper/service shaping)
// ✅ Tenant required for org users; SYSTEM_ADMIN can load without orgId

import React, { useEffect, useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "../shared/store/authStore";
import {
  fetchUserById,
  updateUser,
  uploadProfilePhoto,
} from "../shared/services/userService";
import { apiClient } from "../shared/lib/apiClient";
import type { UserDto, UserUpdateRequest } from "../shared/types/userTypes";

// ----------------------------
// JWT helpers
// ----------------------------
function getUserIdFromToken(token: string): string | null {
  try {
    const parts = token.split(".");
    if (parts.length < 2) return null;

    const b64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(b64)
        .split("")
        .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
        .join("")
    );

    const payload = JSON.parse(json) as any;
    const id = payload.userId ?? payload.user_id ?? payload.uid ?? payload.sub;
    return typeof id === "string" && id.length > 0 ? id : null;
  } catch {
    return null;
  }
}

function decodeRoleFromToken(token: string | null): string | null {
  try {
    if (!token) return null;
    const parts = token.split(".");
    if (parts.length < 2) return null;

    const b64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(b64)
        .split("")
        .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
        .join("")
    );
    const payload = JSON.parse(json) as any;

    const role =
      payload.globalRoleName ??
      payload.roleName ??
      payload.role ??
      payload.authority ??
      payload.authorities?.[0];

    return typeof role === "string" ? role : null;
  } catch {
    return null;
  }
}

function decodeIsSystemAdmin(token: string | null): boolean {
  try {
    if (!token) return false;
    const role = decodeRoleFromToken(token);

    const parts = token.split(".");
    if (parts.length < 2) return false;

    const b64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(b64)
        .split("")
        .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
        .join("")
    );
    const payload = JSON.parse(json) as any;

    const isSysAdminFlag =
      payload.isSystemAdmin ?? payload.is_system_admin ?? payload.sysAdmin;

    return (
      isSysAdminFlag === true ||
      role === "SYSTEM_ADMIN" ||
      payload?.roles?.includes?.("SYSTEM_ADMIN")
    );
  } catch {
    return false;
  }
}

// ----------------------------
// Lookups (names instead of IDs)
// ----------------------------
type PartyOption = { partyId: string; partyName: string };
type CountyOption = { countyId: string; countyName: string };
type OrgOption = { orgId: string; orgName: string };

async function fetchParties(): Promise<PartyOption[]> {
  const { data } = await apiClient.get("/parties");
  return (Array.isArray(data) ? data : data?.content ?? []) as PartyOption[];
}
async function fetchCounties(): Promise<CountyOption[]> {
  const { data } = await apiClient.get("/counties");
  return (Array.isArray(data) ? data : data?.content ?? []) as CountyOption[];
}
async function fetchOrgs(): Promise<OrgOption[]> {
  const { data } = await apiClient.get("/orgs");
  return (Array.isArray(data) ? data : data?.content ?? []) as OrgOption[];
}

// ----------------------------
// Normalizers
// ----------------------------
function readBool(v: any): boolean {
  if (v === true) return true;
  if (v === false) return false;
  if (typeof v === "string") return v.toLowerCase() === "true";
  if (typeof v === "number") return v === 1;
  return false;
}

function readString(v: any): string {
  if (typeof v === "string") return v;
  if (v == null) return "";
  return String(v);
}

function normalizeUser(raw: any): UserDto {
  const u = raw ?? {};

  const active = u.isActive ?? u.active ?? u.is_active ?? false;
  const verified = u.isVerified ?? u.verified ?? u.is_verified ?? false;

  // ✅ Position may be missing due to service shaping; accept multiple keys
  const position = u.position ?? u.userPosition ?? u.jobTitle ?? u.title ?? "";

  // ✅ Image key differences
  const profileUrl =
    u.profilePictureUrl ?? u.profileImageUrl ?? u.profile_image_url ?? null;

  return {
    ...u,
    isActive: readBool(active),
    isVerified: readBool(verified),
    position: readString(position),
    phoneNumber: u.phoneNumber ?? undefined,
    profilePictureUrl: profileUrl,
  } as UserDto;
}

// ----------------------------
// Component
// ----------------------------
const UserProfilePage: React.FC = () => {
  const { token, currentOrgId, user: storeUser } = useAuthStore();

  const orgId = currentOrgId; // may be null for SYSTEM_ADMIN
  const userId = useMemo(
    () => (token ? getUserIdFromToken(token) : null),
    [token]
  );

  const isSystemAdmin =
    !!storeUser?.isSystemAdmin ||
    storeUser?.globalRoleName === "SYSTEM_ADMIN" ||
    decodeIsSystemAdmin(token);

  const defaultUserState: UserDto = {
    userId: "",
    firstName: "",
    lastName: "",
    userName: "",
    email: "",
    position: "",
    phoneNumber: undefined,

    isActive: false,
    isVerified: false,

    roleName: "",

    partyId: null,
    assignedCountyId: null,
    defaultOrgId: null,

    profilePictureUrl: null,

    lastLogin: null,
    failedLoginAttempts: 0,
    lockedUntil: null,
  };

  const queryClient = useQueryClient();
  const canLoadProfile = !!userId && (isSystemAdmin || !!orgId);

  // ✅ KEY FIX: normalize in select() so position never gets dropped by accidental mapping
  const {
    data: user,
    isLoading,
    isError,
    error,
  } = useQuery<any, Error, UserDto>({
    queryKey: ["currentUser", orgId ?? "NO_ORG", userId],
    queryFn: () => fetchUserById(orgId, userId!),
    enabled: canLoadProfile,
    staleTime: 1000 * 60 * 5,
    retry: 2,
    select: (raw) => normalizeUser(raw),
  });

  const partiesQuery = useQuery({
    queryKey: ["parties", orgId],
    queryFn: fetchParties,
    enabled: !isSystemAdmin && !!orgId,
    staleTime: 1000 * 60 * 10,
    retry: 1,
  });

  const countiesQuery = useQuery({
    queryKey: ["counties", orgId],
    queryFn: fetchCounties,
    enabled: !isSystemAdmin && !!orgId,
    staleTime: 1000 * 60 * 10,
    retry: 1,
  });

  const orgsQuery = useQuery({
    queryKey: ["orgs"],
    queryFn: fetchOrgs,
    enabled: !isSystemAdmin,
    staleTime: 1000 * 60 * 10,
    retry: 1,
  });

  const [viewUser, setViewUser] = useState<UserDto>(defaultUserState);
  const [draft, setDraft] = useState<UserDto>(defaultUserState);
  const [isEditing, setIsEditing] = useState(false);

  const [photo, setPhoto] = useState<File | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [isUploading, setIsUploading] = useState(false);

  useEffect(() => {
    if (user) {
      setViewUser(user);
      setDraft(user);
    }
  }, [user]);

  const displayPosition = useMemo(() => {
    const p = (viewUser.position ?? "").trim();
    return p.length ? p : "—";
  }, [viewUser.position]);

  const partyName = useMemo(() => {
    const list = partiesQuery.data ?? [];
    const id = viewUser.partyId;
    if (!id) return "None";
    return list.find((p) => p.partyId === id)?.partyName ?? "Unknown";
  }, [viewUser.partyId, partiesQuery.data]);

  const countyName = useMemo(() => {
    const list = countiesQuery.data ?? [];
    const id = viewUser.assignedCountyId;
    if (!id) return "None";
    return list.find((c) => c.countyId === id)?.countyName ?? "Unknown";
  }, [viewUser.assignedCountyId, countiesQuery.data]);

  const defaultOrgName = useMemo(() => {
    const list = orgsQuery.data ?? [];
    const id = viewUser.defaultOrgId;
    if (!id) return "None";
    return list.find((o) => o.orgId === id)?.orgName ?? "Unknown";
  }, [viewUser.defaultOrgId, orgsQuery.data]);

  const startEdit = () => {
    setDraft(viewUser);
    setIsEditing(true);
  };

  const cancelEdit = () => {
    setDraft(viewUser);
    setPhoto(null);
    setIsEditing(false);
  };

  const handlePhotoUpload = async () => {
    if (!photo || !viewUser.userId) return;

    if (!orgId && !isSystemAdmin) {
      alert("Missing orgId. Unable to upload photo.");
      return;
    }

    try {
      setIsUploading(true);
      const uploadedUrl = await uploadProfilePhoto(
        orgId,
        viewUser.userId,
        photo
      );

      setDraft((p) => ({ ...p, profilePictureUrl: uploadedUrl }));
      setViewUser((p) => ({ ...p, profilePictureUrl: uploadedUrl }));
      setPhoto(null);

      await queryClient.invalidateQueries({
        queryKey: ["currentUser", orgId ?? "NO_ORG", userId],
      });

      alert("Profile photo successfully updated!");
    } catch (err) {
      console.error("Profile photo upload failed:", err);
      alert("Failed to upload profile photo.");
    } finally {
      setIsUploading(false);
    }
  };

  const handleSave = async () => {
    if (!draft.userId) {
      alert("Missing userId. Unable to save.");
      return;
    }

    if (!orgId && !isSystemAdmin) {
      alert("Missing orgId. Unable to save.");
      return;
    }

    const firstName = (draft.firstName ?? "").trim();
    const lastName = (draft.lastName ?? "").trim();
    const userName = (draft.userName ?? "").trim();
    const email = (draft.email ?? "").trim();
    const position = (draft.position ?? "").trim();

    if (!firstName || !lastName || !userName || !email) {
      alert("First name, last name, username, and email are required.");
      return;
    }

    // ✅ Only personal fields
    const payload: UserUpdateRequest = {
      firstName,
      lastName,
      userName,
      email,
      position: position.length ? position : undefined,
      phoneNumber: draft.phoneNumber?.trim() || undefined,
      profileImageUrl: draft.profilePictureUrl ?? undefined,

      // read-only; keep same role
      roleName: (viewUser.roleName as any) || (draft.roleName as any),
    };

    try {
      setIsSaving(true);
      await updateUser(orgId, draft.userId, payload);

      await queryClient.invalidateQueries({
        queryKey: ["currentUser", orgId ?? "NO_ORG", userId],
      });

      setIsEditing(false);
      alert("Profile updated successfully!");
    } catch (err) {
      console.error("Profile update failed:", err);
      alert("Failed to update profile. Please try again.");
    } finally {
      setIsSaving(false);
    }
  };

  // ----------------------------
  // Guards
  // ----------------------------
  if (!token) {
    return (
      <div style={{ maxWidth: "720px", margin: "0 auto", padding: 20 }}>
        <h2>Not authenticated</h2>
        <p>Please sign in so we can load your profile.</p>
      </div>
    );
  }

  if (!userId) {
    return (
      <div style={{ maxWidth: "720px", margin: "0 auto", padding: 20 }}>
        <h2>Cannot resolve user</h2>
        <p>
          Your token does not include a usable <code>userId</code> or{" "}
          <code>sub</code> claim.
        </p>
      </div>
    );
  }

  if (!orgId && !isSystemAdmin) {
    return (
      <div style={{ maxWidth: "720px", margin: "0 auto", padding: 20 }}>
        <h2>Organization not selected</h2>
        <p>
          Please select an organization (tenant) so requests can include{" "}
          <code>X-Org-Id</code>.
        </p>
      </div>
    );
  }

  if (isLoading) return <p>Loading user profile...</p>;

  if (isError) {
    return (
      <div style={{ maxWidth: "720px", margin: "0 auto", padding: 20 }}>
        <h2>Error loading profile</h2>
        <p>{(error as any)?.message ?? "Unable to fetch user profile."}</p>
      </div>
    );
  }

  if (!user) return <p>Error: Unable to fetch user profile.</p>;

  const readonlyRow = (label: string, value: React.ReactNode) => (
    <div style={{ marginBottom: 10 }}>
      <div style={{ fontSize: 12, opacity: 0.7 }}>{label}</div>
      <div style={{ fontSize: 14 }}>{value}</div>
    </div>
  );

  // ----------------------------
  // Render
  // ----------------------------
  return (
    <div style={{ maxWidth: "720px", margin: "0 auto", padding: 20 }}>
      <div
        style={{ display: "flex", justifyContent: "space-between", gap: 12 }}
      >
        <h1 style={{ margin: 0 }}>
          Profile{" "}
          <span style={{ fontSize: 12, opacity: 0.7 }}>
            ({isSystemAdmin ? "System Admin" : `Tenant: ${orgId}`})
          </span>
        </h1>

        {!isEditing ? (
          <button onClick={startEdit} style={{ height: 36 }}>
            Edit Profile
          </button>
        ) : (
          <div style={{ display: "flex", gap: 8 }}>
            <button onClick={cancelEdit} disabled={isSaving}>
              Cancel
            </button>
            <button onClick={handleSave} disabled={isSaving}>
              {isSaving ? "Saving..." : "Save"}
            </button>
          </div>
        )}
      </div>

      <hr style={{ margin: "16px 0" }} />

      {/* READ-ONLY VIEW */}
      {!isEditing && (
        <div>
          <div style={{ display: "flex", gap: 16, alignItems: "center" }}>
            <img
              src={viewUser.profilePictureUrl ?? "/placeholder.png"}
              alt="Profile"
              style={{ width: 96, height: 96, borderRadius: "50%" }}
            />

            <div>
              <div style={{ fontSize: 18, fontWeight: 600 }}>
                {viewUser.firstName} {viewUser.lastName}
              </div>
              <div style={{ fontSize: 13, opacity: 0.8 }}>
                @{viewUser.userName} • {viewUser.email}
              </div>

              {/* ✅ This will NEVER be blank anymore */}
              <div style={{ fontSize: 13, opacity: 0.8 }}>
                Position: {displayPosition}
              </div>

              <div style={{ fontSize: 13, opacity: 0.8 }}>
                Phone: {viewUser.phoneNumber || "—"}
              </div>
            </div>
          </div>

          <hr style={{ margin: "16px 0" }} />

          {readonlyRow("Role", viewUser.roleName || "—")}
          {readonlyRow("Position", displayPosition)}

          <div style={{ display: "flex", gap: 20, marginTop: 6 }}>
            <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
              <input type="checkbox" checked={!!viewUser.isActive} readOnly />
              Active
            </label>

            <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
              <input type="checkbox" checked={!!viewUser.isVerified} readOnly />
              Verified
            </label>
          </div>

          {!isSystemAdmin && (
            <>
              <hr style={{ margin: "16px 0" }} />
              {readonlyRow("Party", partyName)}
              {readonlyRow("Assigned County", countyName)}
              {readonlyRow("Default Organization", defaultOrgName)}
              <div style={{ fontSize: 12, opacity: 0.7, marginTop: 8 }}>
                These fields are managed by administrators.
              </div>
            </>
          )}
        </div>
      )}

      {/* EDIT FORM (Only personal fields) */}
      {isEditing && (
        <div
          style={{
            marginTop: 10,
            padding: 14,
            border: "1px solid #e2e8f0",
            borderRadius: 12,
            background: "#fff",
          }}
        >
          <div style={{ display: "flex", gap: 16, alignItems: "center" }}>
            <img
              src={draft.profilePictureUrl ?? "/placeholder.png"}
              alt="Profile"
              style={{ width: 96, height: 96, borderRadius: "50%" }}
            />

            <div>
              <input
                type="file"
                accept="image/*"
                onChange={(e) => setPhoto(e.target.files?.[0] ?? null)}
              />
              <div style={{ marginTop: 8 }}>
                <button
                  onClick={handlePhotoUpload}
                  disabled={!photo || isUploading}
                >
                  {isUploading ? "Uploading..." : "Upload Photo"}
                </button>
              </div>
            </div>
          </div>

          <hr style={{ margin: "16px 0" }} />

          <label>
            First Name:
            <input
              type="text"
              value={draft.firstName}
              onChange={(e) =>
                setDraft((p) => ({ ...p, firstName: e.target.value }))
              }
              style={{ display: "block", width: "100%", marginTop: 4 }}
            />
          </label>

          <br />

          <label>
            Last Name:
            <input
              type="text"
              value={draft.lastName}
              onChange={(e) =>
                setDraft((p) => ({ ...p, lastName: e.target.value }))
              }
              style={{ display: "block", width: "100%", marginTop: 4 }}
            />
          </label>

          <br />

          <label>
            Username:
            <input
              type="text"
              value={draft.userName}
              onChange={(e) =>
                setDraft((p) => ({ ...p, userName: e.target.value }))
              }
              style={{ display: "block", width: "100%", marginTop: 4 }}
            />
          </label>

          <br />

          <label>
            Email:
            <input
              type="email"
              value={draft.email}
              onChange={(e) =>
                setDraft((p) => ({ ...p, email: e.target.value }))
              }
              style={{ display: "block", width: "100%", marginTop: 4 }}
            />
          </label>

          <br />

          <label>
            Position:
            <input
              type="text"
              value={draft.position ?? ""}
              onChange={(e) =>
                setDraft((p) => ({ ...p, position: e.target.value }))
              }
              style={{ display: "block", width: "100%", marginTop: 4 }}
            />
          </label>

          <br />

          <label>
            Phone Number:
            <input
              type="tel"
              value={draft.phoneNumber ?? ""}
              onChange={(e) =>
                setDraft((p) => ({ ...p, phoneNumber: e.target.value }))
              }
              style={{ display: "block", width: "100%", marginTop: 4 }}
            />
          </label>

          <hr style={{ margin: "16px 0" }} />

          {/* Admin-managed fields shown read-only */}
          <div style={{ opacity: 0.9 }}>
            <div style={{ fontWeight: 600, marginBottom: 10 }}>
              Admin Managed Fields (Read-only)
            </div>

            {readonlyRow("Role", viewUser.roleName || "—")}

            <div style={{ display: "flex", gap: 20, marginTop: 6 }}>
              <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
                <input type="checkbox" checked={!!viewUser.isActive} readOnly />
                Active
              </label>

              <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
                <input
                  type="checkbox"
                  checked={!!viewUser.isVerified}
                  readOnly
                />
                Verified
              </label>
            </div>

            {!isSystemAdmin && (
              <div style={{ marginTop: 12 }}>
                {readonlyRow("Party", partyName)}
                {readonlyRow("Assigned County", countyName)}
                {readonlyRow("Default Organization", defaultOrgName)}
              </div>
            )}

            <div style={{ fontSize: 12, opacity: 0.75, marginTop: 8 }}>
              To change role/active/verified/tenant assignment, contact an
              admin.
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default UserProfilePage;
