// // src/shared/services/pollingPlaceService.ts
// // ------------------------------------------------------
// // Service for Polling Places.
// // Backend endpoint:
// //   GET /api/polling-places/by-center/{centerId}
// // where centerId is a UUID.
// // ------------------------------------------------------

// import { apiClient } from "../lib/apiClient";

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

// /** Load polling places for a given centerId (UUID). */
// export async function fetchPollingPlacesByCenter(
//   centerId: string
// ): Promise<PollingPlaceDto[]> {
//   const res = await apiClient.get<PollingPlaceDto[]>(
//     `/polling-places/by-center/${centerId}`
//   );
//   return res.data ?? [];
// }
