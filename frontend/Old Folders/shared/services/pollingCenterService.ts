// // src/shared/services/pollingCenterService.ts

// import { apiClient } from "../lib/apiClient";

// export interface PollingCenterDto {
//   centerId: string; // normalized ID used by the UI
//   centerName: string;
//   code: string;
//   registeredVoters: number;
//   districtId?: string;
//   districtName?: string;
//   countyId?: string;
//   countyName?: string;
// }

// // Raw shape from backend – it might use `id` instead of `centerId`
// interface PollingCenterDtoRaw {
//   id?: string;
//   centerId?: string;
//   centerName: string;
//   code: string;
//   registeredVoters: number;
//   districtId?: string;
//   districtName?: string;
//   countyId?: string;
//   countyName?: string;
// }

// export interface PollingCenterQuery {
//   districtId?: string | null;
//   q?: string;
// }

// // Generic Spring Data page response
// interface PageResponse<T> {
//   content: T[];
//   totalElements: number;
//   totalPages: number;
//   size: number;
//   number: number;
// }

// export type PollingCenterOption = {
//   value: string;
//   label: string;
//   code?: string;
// };

// export function toPollingCenterOptions(
//   list: PollingCenterDto[]
// ): PollingCenterOption[] {
//   return (list ?? [])
//     .filter((c) => !!c.centerId)
//     .map((c) => ({
//       value: c.centerId,
//       label: c.centerName,
//       code: c.code,
//     }));
// }

// export async function fetchPollingCenters(
//   params: PollingCenterQuery = {}
// ): Promise<PollingCenterDto[]> {
//   const res = await apiClient.get<PageResponse<PollingCenterDtoRaw>>(
//     "/polling-centers",
//     {
//       params,
//     }
//   );

//   const raw = res.data.content ?? [];

//   // Normalize centerId so the rest of the app can rely on it
//   return raw.map((c) => {
//     const normalizedId = c.centerId ?? c.id;
//     if (!normalizedId) {
//       // Fall back to a stable but non-ideal key if needed
//       // (should not happen if backend always provides some ID)
//       throw new Error(
//         "Polling center missing id/centerId from backend response"
//       );
//       return {
//         ...c,
//         centerId: "",
//       };
//     }

//     return {
//       ...c,
//       centerId: normalizedId,
//     };
//   });
// }
