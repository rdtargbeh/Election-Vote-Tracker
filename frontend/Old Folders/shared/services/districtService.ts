// // src/shared/services/districtService.ts
// // ------------------------------------------------------
// // Service wrapper for District-related API calls.
// //
// // Backend endpoints (expected):
// //   GET /api/districts?q=...&countyId=...&page=...&size=...
// //   GET /api/districts/by-county/{countyId}
// //
// // This file provides:
// //   - DistrictDto type
// //   - fetchDistricts(...)  -> paged list (for tables/search)
// //   - fetchDistrictsByCounty(countyId) -> flat list for dropdowns
// // ------------------------------------------------------

// import { apiClient } from "../lib/apiClient";
// import type { PageResponse } from "./countyService";

// // Shape of a district from your backend DTO:
// export interface DistrictDto {
//   districtId: string;
//   districtName: string;
//   countyId: string;
//   countyName: string;
// }

// export interface DistrictPageParams {
//   q?: string;
//   countyId?: string;
//   page?: number;
//   size?: number;
// }

// /**
//  * Paged district search.
//  *
//  * Maps to backend controller:
//  *   GET /api/districts?q=...&countyId=...&page=...&size=...
//  */
// export async function fetchDistricts(
//   params: DistrictPageParams = {}
// ): Promise<PageResponse<DistrictDto>> {
//   const { q, countyId, page = 0, size = 20 } = params;

//   const res = await apiClient.get<PageResponse<DistrictDto>>("/districts", {
//     params: {
//       q,
//       countyId,
//       page,
//       size,
//     },
//   });

//   return res.data;
// }

// /**
//  * Convenience helper for dropdowns:
//  *
//  * Maps to backend method:
//  *   GET /api/districts/by-county/{countyId}
//  */
// export async function fetchDistrictsByCounty(
//   countyId: string
// ): Promise<DistrictDto[]> {
//   const res = await apiClient.get<DistrictDto[]>(
//     `/districts/by-county/${countyId}`
//   );
//   return res.data;
// }
