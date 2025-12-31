// // src/shared/services/observerReportService.ts
// // ------------------------------------------------------
// // Service wrapper for observer reports.
// //
// // Backend endpoints (expected):
// //  GET    /api/observer-reports           (paged search)
// //  GET    /api/observer-reports/{id}      (single by id)
// //  (Create/update/delete can be added later)
// // ------------------------------------------------------

// import { apiClient } from "../lib/apiClient";
// import type { PageResponse } from "./voteSubmissionService";

// // Types in DB:
// //   type IN ('VIOLENCE','INTIMIDATION','EQUIPMENT_ISSUE','OTHER')
// export type ObserverReportType =
//   | "VIOLENCE"
//   | "INTIMIDATION"
//   | "EQUIPMENT_ISSUE"
//   | "OTHER";

// export interface ObserverReportDto {
//   reportId: string;

//   orgId: string;
//   observerId: string;

//   // Optional location linkage + labels
//   countyId?: string | null;
//   countyName?: string | null;
//   centerId?: string | null;
//   centerName?: string | null;

//   type: ObserverReportType;
//   description: string;
//   mediaUrl?: string | null;

//   // Flattened GPS point if backend exposes it
//   latitude?: number | null;
//   longitude?: number | null;

//   timestamp: string; // ISO string
//   resolved: boolean;

//   // Helpful labels
//   observerName?: string | null;

//   // Optional audit fields
//   dateCreated?: string | null;
//   dateUpdated?: string | null;
// }

// // Query params lining up with a typical controller signature
// export interface ObserverReportQuery {
//   orgId?: string;
//   countyId?: string;
//   centerId?: string;
//   type?: ObserverReportType;
//   resolved?: boolean; // true/false, undefined = all
//   from?: string; // ISO date-time string
//   to?: string; // ISO date-time string
//   q?: string;
//   page?: number;
//   size?: number;
// }

// // ---------- API functions ----------

// /**
//  * Paged search for observer reports.
//  * Mirrors:
//  *   GET /api/observer-reports
//  */
// export async function fetchObserverReports(
//   params: ObserverReportQuery = {}
// ): Promise<PageResponse<ObserverReportDto>> {
//   const {
//     orgId,
//     countyId,
//     centerId,
//     type,
//     resolved,
//     from,
//     to,
//     q,
//     page = 0,
//     size = 20,
//   } = params;

//   const res = await apiClient.get<PageResponse<ObserverReportDto>>(
//     "/observer-reports",
//     {
//       params: {
//         orgId,
//         countyId,
//         centerId,
//         type,
//         resolved,
//         from,
//         to,
//         q: q && q.trim().length > 0 ? q.trim() : undefined,
//         page,
//         size,
//       },
//     }
//   );

//   return res.data;
// }

// /**
//  * Load a single observer report by id.
//  * Mirrors:
//  *   GET /api/observer-reports/{id}
//  */
// export async function fetchObserverReport(
//   reportId: string
// ): Promise<ObserverReportDto> {
//   const res = await apiClient.get<ObserverReportDto>(
//     `/observer-reports/${reportId}`
//   );
//   return res.data;
// }
