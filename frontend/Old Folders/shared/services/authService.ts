// // src/shared/services/authService.ts
// import { apiClient } from "../lib/apiClient";
// import type { AuthUser } from "../store/authStore";

// /**
//  * Fetch current user profile.
//  * - If orgId is provided, force X-Org-Id for this request (tenant user).
//  * - If not provided, call without X-Org-Id (system admin platform mode).
//  */
// export async function fetchMe(orgId?: string | null): Promise<AuthUser> {
//   const res = await apiClient.get<AuthUser>("/users/me", {
//     headers: orgId ? { "X-Org-Id": orgId } : undefined,
//   });
//   return res.data;
// }
