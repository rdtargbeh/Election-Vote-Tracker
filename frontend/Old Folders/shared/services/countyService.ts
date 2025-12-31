// // src/shared/services/countyService.ts
// // ------------------------------------------------------
// // County service wrapper around the backend /api/counties
// // endpoint.
// //
// // We support two usage patterns:
// //
// // 1) Paged list (admin tables):
// //    fetchCountiesPage({ q, page, size }) -> PageResponse<CountyDto>
// //
// // 2) Simple list (dropdowns/selectors):
// //    fetchCounties(q?) -> CountyDto[]
// //    (internally calls page=0,size=1000 and returns .content)
// // ------------------------------------------------------

// import { apiClient } from "../lib/apiClient";

// // DTO mirrors your backend CountyDto record:
// // public record CountyDto(UUID countyId, String countyName) {}
// export interface CountyDto {
//   countyId: string;
//   countyName: string;
// }

// // Generic Spring Data page response shape
// export interface PageResponse<T> {
//   content: T[];
//   totalElements: number;
//   totalPages: number;
//   size: number;
//   number: number; // current page index
// }

// // Params for paged fetch
// export interface CountyPageParams {
//   q?: string;
//   page?: number;
//   size?: number;
// }

// /**
//  * Paged fetch for counties.
//  *
//  * Backend endpoint:
//  *   GET /api/counties?q=...&page=...&size=...
//  */
// export async function fetchCountiesPage(
//   params: CountyPageParams = {}
// ): Promise<PageResponse<CountyDto>> {
//   const { q, page = 0, size = 20 } = params;

//   const res = await apiClient.get<PageResponse<CountyDto>>("/counties", {
//     params: {
//       q: q && q.trim().length > 0 ? q.trim() : undefined,
//       page,
//       size,
//     },
//   });

//   return res.data;
// }

// /**
//  * Simple list of counties (array).
//  *
//  * This is perfect for:
//  *  - dropdowns
//  *  - selectors
//  *  - non-paginated lists
//  *
//  * Implementation detail:
//  *   We call the same /api/counties endpoint with
//  *   page=0, size=1000 and unwrap .content.
//  *   Liberia has few counties, so this is safe and simple.
//  */
// export async function fetchCounties(q?: string): Promise<CountyDto[]> {
//   const page = await fetchCountiesPage({
//     q,
//     page: 0,
//     size: 1000,
//   });

//   return page.content ?? [];
// }
