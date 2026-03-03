// // src/shared/services/geographyService.ts
// // ------------------------------------------------------
// // Centralized geography service for:
// //   - Counties
// //   - Districts
// //   - Polling centers
// //   - Polling places
// // ------------------------------------------------------

// import { apiClient } from "../lib/apiClient";

// /** Generic Spring Page response wrapper */
// export interface PageResponse<T> {
//   content: T[];
//   totalElements: number;
//   totalPages: number;
//   size: number;
//   number: number;
// }

// /** County DTO (backend: CountyDto record) */
// export interface CountyDto {
//   countyId: string;
//   countyName: string;
// }

// /** District DTO (backend: DistrictDto record) */
// export interface DistrictDto {
//   districtId: string;
//   districtName: string;
//   countyId: string;
//   countyName: string;
// }

// /** Polling center DTO (backend: PollingCenterDto record) */
// export interface PollingCenterDto {
//   centerId: string;
//   centerName: string;
//   code: string;
//   registeredVoters: number;

//   districtId: string;
//   districtName: string;
//   countyId: string;
//   countyName: string;
// }

// /** Polling place DTO (backend: PollingPlaceDto) */
// export interface PollingPlaceDto {
//   placeId: string;

//   centerId: string;
//   centerCode: string;
//   centerName: string;

//   districtId: string;
//   districtName: string;

//   countyId: string;
//   countyName: string;

//   placeNumber: number;
//   code: string;
//   label?: string | null;

//   active: boolean;
// }

// // ------------------------------------------------------
// // API functions
// // ------------------------------------------------------

// /** Load all counties (we hide paging and return a flat array). */
// export async function fetchCounties(): Promise<CountyDto[]> {
//   const res = await apiClient.get<PageResponse<CountyDto>>("/counties", {
//     params: { size: 100, sort: "countyName,asc" },
//   });
//   return res.data.content ?? [];
// }

// /** Load all districts for a given countyId. */
// export async function fetchDistrictsByCounty(
//   countyId: string
// ): Promise<DistrictDto[]> {
//   if (!countyId) return [];
//   const res = await apiClient.get<DistrictDto[]>(
//     `/districts/by-county/${countyId}`
//   );
//   return res.data ?? [];
// }

// /** Load polling centers for a given districtId. */
// export async function fetchCentersByDistrict(
//   districtId: string
// ): Promise<PollingCenterDto[]> {
//   if (!districtId) return [];
//   const res = await apiClient.get<PageResponse<PollingCenterDto>>(
//     "/polling-centers",
//     {
//       params: {
//         districtId,
//         size: 1000,
//         sort: "centerName,asc",
//       },
//     }
//   );
//   return res.data.content ?? [];
// }

// /** Load polling places for a given centerId. */
// export async function fetchPlacesByCenter(
//   centerId: string
// ): Promise<PollingPlaceDto[]> {
//   if (!centerId) return [];
//   const res = await apiClient.get<PollingPlaceDto[]>(
//     `/polling-places/by-center/${centerId}`
//   );
//   return res.data ?? [];
// }
