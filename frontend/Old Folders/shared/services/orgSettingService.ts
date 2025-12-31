// import { apiClient } from "../lib/apiClient";

// export type OrgSettingDto = {
//   orgId: string;
//   settings: Record<string, any>;
// };

// export const ORG_SETTING_KEYS = {
//   RATE_LIMIT_PER_MIN: "rate_limit_per_min",
//   SHOW_OFFICIAL: "show_official",
//   LOCKOUT_THRESHOLD: "lockout_threshold",
//   LOCKOUT_MINUTES: "lockout_minutes",
// } as const;

// export async function fetchOrgSettings(orgId: string): Promise<OrgSettingDto> {
//   const { data } = await apiClient.get(`/org-settings`, {
//     headers: { "X-Org-Id": orgId },
//   });
//   return data;
// }

// export async function patchOrgSettings(
//   orgId: string,
//   patch: Record<string, any>
// ): Promise<OrgSettingDto> {
//   const { data } = await apiClient.patch(`/org-settings`, patch, {
//     headers: { "X-Org-Id": orgId },
//   });
//   return data;
// }
