// import { apiClient } from "../lib/apiClient";

// export type OrganizationDto = {
//   orgId: string;
//   orgName: string;
//   organizationType: string;
//   logoUrl?: string | null;
//   primaryColor?: string | null;
//   subdomain?: string | null;
//   active: boolean;
//   partyName?: string | null;
// };

// export async function fetchOrganizationById(
//   orgId: string
// ): Promise<OrganizationDto> {
//   const { data } = await apiClient.get(`/orgs/${orgId}`, {
//     headers: { "X-Org-Id": orgId },
//   });
//   return data;
// }

// // ✅ Backend you added: PATCH /api/orgs/{id}/branding
// export async function patchOrganizationBranding(
//   orgId: string,
//   patch: { logoUrl?: string | null; primaryColor?: string | null }
// ): Promise<OrganizationDto> {
//   const { data } = await apiClient.patch(`/orgs/${orgId}/branding`, patch, {
//     headers: { "X-Org-Id": orgId },
//   });
//   return data;
// }
