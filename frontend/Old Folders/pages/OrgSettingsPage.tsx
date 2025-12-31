import React, { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "../../shared/lib/store/authStore";
import { fetchMe } from "../shared/services/userService";

import {
  fetchOrganizationById,
  patchOrganizationBranding,
} from "../shared/services/organizationBrandingService";

import {
  fetchOrgSettings,
  patchOrgSettings,
  ORG_SETTING_KEYS,
} from "../shared/services/orgSettingService";

const normalizeRole = (r: any) =>
  String(r ?? "")
    .trim()
    .toUpperCase()
    .replace(/^ROLE_/, "");

type BrandingForm = {
  logoUrl: string;
  primaryColor: string;
};

type SettingForm = {
  rate_limit_per_min: string;
  show_official: boolean;
  lockout_threshold: string;
  lockout_minutes: string;
};

const toStr = (v: any) => (v === null || v === undefined ? "" : String(v));

const OrgSettingsPage: React.FC = () => {
  const qc = useQueryClient();
  const { currentOrgId, user } = useAuthStore();
  const orgId = currentOrgId;

  // role checks
  const tenantMeQuery = useQuery({
    queryKey: ["me", orgId ?? "NO_ORG"],
    queryFn: () => fetchMe(orgId!),
    enabled: !!orgId,
    staleTime: 1000 * 60 * 5,
    retry: 1,
  });

  const myRole = normalizeRole(
    tenantMeQuery.data?.roleName ?? user?.globalRoleName
  );

  const isSystemAdmin =
    !!tenantMeQuery.data?.isSystemAdmin || myRole === "SYSTEM_ADMIN";

  // ✅ org-level admins can update branding + org settings (their org only enforced by backend)
  const canEditOrgConfig =
    isSystemAdmin || myRole === "ADMIN" || myRole === "PARTY_ADMIN";

  // --- Queries ---
  const orgQuery = useQuery({
    queryKey: ["orgProfile", orgId ?? "NO_ORG"],
    queryFn: () => fetchOrganizationById(orgId!),
    enabled: !!orgId,
    staleTime: 1000 * 60 * 5,
  });

  const settingsQuery = useQuery({
    queryKey: ["orgSettings", orgId ?? "NO_ORG"],
    queryFn: () => fetchOrgSettings(orgId!),
    enabled: !!orgId,
    staleTime: 1000 * 60 * 5,
  });

  // --- Branding form ---
  const org = orgQuery.data;

  const brandingInitial: BrandingForm = useMemo(
    () => ({
      logoUrl: String(org?.logoUrl ?? ""),
      primaryColor: String(org?.primaryColor ?? "#0A84FF"),
    }),
    [org?.logoUrl, org?.primaryColor]
  );

  const [branding, setBranding] = useState<BrandingForm>(brandingInitial);

  useEffect(() => {
    if (orgQuery.isSuccess) setBranding(brandingInitial);
  }, [orgQuery.isSuccess, brandingInitial]);

  // --- Settings form ---
  const serverSettings = settingsQuery.data?.settings ?? {};

  const settingsInitial: SettingForm = useMemo(
    () => ({
      rate_limit_per_min: toStr(
        serverSettings[ORG_SETTING_KEYS.RATE_LIMIT_PER_MIN]
      ),
      show_official: Boolean(serverSettings[ORG_SETTING_KEYS.SHOW_OFFICIAL]),
      lockout_threshold: toStr(
        serverSettings[ORG_SETTING_KEYS.LOCKOUT_THRESHOLD]
      ),
      lockout_minutes: toStr(serverSettings[ORG_SETTING_KEYS.LOCKOUT_MINUTES]),
    }),
    [serverSettings]
  );

  const [settingsForm, setSettingsForm] =
    useState<SettingForm>(settingsInitial);

  useEffect(() => {
    if (settingsQuery.isSuccess) setSettingsForm(settingsInitial);
  }, [settingsQuery.isSuccess, settingsInitial]);

  // --- Mutations ---
  const brandingMutation = useMutation({
    mutationFn: (patch: {
      logoUrl?: string | null;
      primaryColor?: string | null;
    }) => patchOrganizationBranding(orgId!, patch),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ["orgProfile", orgId] });
      alert("Branding updated.");
    },
    onError: (err: any) => {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        "Failed to update branding.";
      alert(msg);
      console.error(err);
    },
  });

  const settingsMutation = useMutation({
    mutationFn: (patch: Record<string, any>) => patchOrgSettings(orgId!, patch),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ["orgSettings", orgId] });
      alert("Org settings updated.");
    },
    onError: (err: any) => {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        "Failed to update settings.";
      alert(msg);
      console.error(err);
    },
  });

  const buildBrandingPatch = () => {
    const patch: any = {};
    if (branding.logoUrl !== brandingInitial.logoUrl)
      patch.logoUrl =
        branding.logoUrl.trim() === "" ? null : branding.logoUrl.trim();
    if (branding.primaryColor !== brandingInitial.primaryColor)
      patch.primaryColor =
        branding.primaryColor.trim() === ""
          ? null
          : branding.primaryColor.trim();
    return patch;
  };

  const buildSettingsPatch = () => {
    const patch: Record<string, any> = {};

    if (
      settingsForm.rate_limit_per_min !== settingsInitial.rate_limit_per_min
    ) {
      patch[ORG_SETTING_KEYS.RATE_LIMIT_PER_MIN] =
        settingsForm.rate_limit_per_min.trim() === ""
          ? null
          : Number(settingsForm.rate_limit_per_min);
    }

    if (settingsForm.show_official !== settingsInitial.show_official) {
      patch[ORG_SETTING_KEYS.SHOW_OFFICIAL] = settingsForm.show_official;
    }

    if (settingsForm.lockout_threshold !== settingsInitial.lockout_threshold) {
      patch[ORG_SETTING_KEYS.LOCKOUT_THRESHOLD] =
        settingsForm.lockout_threshold.trim() === ""
          ? null
          : Number(settingsForm.lockout_threshold);
    }

    if (settingsForm.lockout_minutes !== settingsInitial.lockout_minutes) {
      patch[ORG_SETTING_KEYS.LOCKOUT_MINUTES] =
        settingsForm.lockout_minutes.trim() === ""
          ? null
          : Number(settingsForm.lockout_minutes);
    }

    return patch;
  };

  const resetSettingsToDefaults = () => {
    settingsMutation.mutate({
      [ORG_SETTING_KEYS.RATE_LIMIT_PER_MIN]: null,
      [ORG_SETTING_KEYS.SHOW_OFFICIAL]: null,
      [ORG_SETTING_KEYS.LOCKOUT_THRESHOLD]: null,
      [ORG_SETTING_KEYS.LOCKOUT_MINUTES]: null,
    });
  };

  if (!orgId)
    return <div style={{ padding: 16 }}>No organization selected.</div>;
  if (orgQuery.isLoading || settingsQuery.isLoading || tenantMeQuery.isLoading)
    return (
      <div style={{ padding: 16 }}>Loading organization configuration...</div>
    );

  return (
    <div style={{ padding: 16, maxWidth: 980 }}>
      <h1 style={{ marginTop: 0 }}>Organization Settings</h1>

      {!canEditOrgConfig && (
        <div style={{ padding: 12, border: "1px solid #ddd", borderRadius: 8 }}>
          You don’t have permission to edit organization settings.
        </div>
      )}

      {/* ---- Branding (Organization table) ---- */}
      <section
        style={{
          marginTop: 12,
          padding: 12,
          border: "1px solid #ddd",
          borderRadius: 8,
        }}
      >
        <h3 style={{ marginTop: 0 }}>Branding</h3>

        <div style={{ display: "grid", gap: 10 }}>
          <label style={{ display: "grid", gap: 6 }}>
            Logo URL
            <input
              value={branding.logoUrl}
              disabled={!canEditOrgConfig}
              onChange={(e) =>
                setBranding((p) => ({ ...p, logoUrl: e.target.value }))
              }
              placeholder="https://..."
            />
          </label>

          <label style={{ display: "flex", alignItems: "center", gap: 12 }}>
            Primary Color
            <input
              type="color"
              value={branding.primaryColor || "#0A84FF"}
              disabled={!canEditOrgConfig}
              onChange={(e) =>
                setBranding((p) => ({ ...p, primaryColor: e.target.value }))
              }
            />
            <code>{branding.primaryColor}</code>
          </label>

          <div style={{ display: "flex", gap: 10 }}>
            <button
              disabled={!canEditOrgConfig || brandingMutation.isPending}
              onClick={() => brandingMutation.mutate(buildBrandingPatch())}
            >
              Save Branding
            </button>

            <button
              disabled={brandingMutation.isPending}
              onClick={() => setBranding(brandingInitial)}
            >
              Cancel
            </button>
          </div>
        </div>
      </section>

      {/* ---- Advanced settings (JSONB) ---- */}
      <section
        style={{
          marginTop: 12,
          padding: 12,
          border: "1px solid #ddd",
          borderRadius: 8,
        }}
      >
        <h3 style={{ marginTop: 0 }}>Security & Limits</h3>

        <div style={{ display: "grid", gap: 10 }}>
          <label style={{ display: "grid", gap: 6 }}>
            Rate limit per minute (60–10000)
            <input
              value={settingsForm.rate_limit_per_min}
              disabled={!canEditOrgConfig}
              onChange={(e) =>
                setSettingsForm((p) => ({
                  ...p,
                  rate_limit_per_min: e.target.value,
                }))
              }
              placeholder="e.g. 500"
            />
          </label>

          <label style={{ display: "grid", gap: 6 }}>
            Lockout threshold (1–20)
            <input
              value={settingsForm.lockout_threshold}
              disabled={!canEditOrgConfig}
              onChange={(e) =>
                setSettingsForm((p) => ({
                  ...p,
                  lockout_threshold: e.target.value,
                }))
              }
              placeholder="e.g. 5"
            />
          </label>

          <label style={{ display: "grid", gap: 6 }}>
            Lockout minutes (5–240)
            <input
              value={settingsForm.lockout_minutes}
              disabled={!canEditOrgConfig}
              onChange={(e) =>
                setSettingsForm((p) => ({
                  ...p,
                  lockout_minutes: e.target.value,
                }))
              }
              placeholder="e.g. 15"
            />
          </label>

          <label style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <input
              type="checkbox"
              checked={settingsForm.show_official}
              disabled={!canEditOrgConfig}
              onChange={(e) =>
                setSettingsForm((p) => ({
                  ...p,
                  show_official: e.target.checked,
                }))
              }
            />
            Show official NEC results alongside party tallies
          </label>

          <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
            <button
              disabled={!canEditOrgConfig || settingsMutation.isPending}
              onClick={() => settingsMutation.mutate(buildSettingsPatch())}
            >
              Save Settings
            </button>

            <button
              disabled={settingsMutation.isPending}
              onClick={() => setSettingsForm(settingsInitial)}
            >
              Cancel
            </button>

            <button
              disabled={!canEditOrgConfig || settingsMutation.isPending}
              onClick={resetSettingsToDefaults}
            >
              Reset to Defaults
            </button>
          </div>
        </div>
      </section>
    </div>
  );
};

export default OrgSettingsPage;
