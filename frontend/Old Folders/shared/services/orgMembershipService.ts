// import { apiClient } from "../lib/apiClient";
// import type { AxiosResponse } from "axios";
// import type { OrgMembershipDto } from "../types/userTypes";

// /** Fetch organization members (tenant-scoped) */
// export async function fetchOrgMembers(
//   orgId: string,
//   params: {
//     page: number;
//     size: number;
//     search?: string;
//     roleName?: string;
//     enabled?: boolean;
//   }
// ): Promise<{ members: OrgMembershipDto[]; totalElements: number }> {
//   const res: AxiosResponse<any> = await apiClient.get(`/members`, {
//     params: {
//       page: params.page,
//       size: params.size,
//       q: params.search || undefined,
//       roleName: params.roleName || undefined,
//       enabled: params.enabled ?? undefined,
//     },
//     headers: { "X-Org-Id": orgId }, // Tenant-scoped API
//   });

//   return {
//     members: res.data?.content || [],
//     totalElements: res.data?.totalElements || 0,
//   };
// }

// /** Enable or disable organization membership */
// export async function toggleOrgMembership(
//   orgId: string,
//   userId: string,
//   enabled: boolean
// ): Promise<void> {
//   await apiClient.patch(
//     `/members/${userId}/enabled`,
//     { enabled },
//     { headers: { "X-Org-Id": orgId } }
//   );
// }

// /** Assign a new role to an organization member */
// export async function assignOrgMembershipRole(
//   orgId: string,
//   userId: string,
//   roleName: string
// ): Promise<void> {
//   await apiClient.patch(
//     `/members/${userId}/role`,
//     { roleName },
//     { headers: { "X-Org-Id": orgId } }
//   );
// }

// export async function removeOrgMember(
//   orgId: string,
//   userId: string
// ): Promise<void> {
//   await apiClient.delete(`/members/${userId}`, {
//     headers: { "X-Org-Id": orgId },
//   });
// }
