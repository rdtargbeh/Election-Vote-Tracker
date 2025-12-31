// import { apiClient } from "../lib/apiClient";

// export type PartyDto = {
//   partyId: string;
//   partyName: string;
// };

// export type PageResponse<T> = {
//   content: T[];
//   totalElements: number;
//   totalPages: number;
//   size: number;
//   number: number;
// };

// // If apiClient already prefixes "/api", change to "/parties"
// const PARTIES_URL = "/parties";

// /** Global party search/list (paged) */
// export async function fetchPartiesPage(args?: {
//   q?: string;
//   page?: number;
//   size?: number;
// }): Promise<PageResponse<PartyDto>> {
//   const { q, page = 0, size = 200 } = args ?? {};
//   const res = await apiClient.get<PageResponse<PartyDto>>(PARTIES_URL, {
//     params: { q, page, size },
//   });
//   return res.data;
// }

// /** Convenience: fetch list for dropdown */
// export async function fetchPartiesList(q?: string): Promise<PartyDto[]> {
//   const page = await fetchPartiesPage({ q, page: 0, size: 200 });
//   return page.content ?? [];
// }

// /** Alias (so you can import fetchParties like before) */
// export async function fetchParties(q?: string): Promise<PartyDto[]> {
//   return fetchPartiesList(q);
// }
