// import { apiClient } from "../lib/apiClient";

// /** Backend enum */
// export type ElectionType =
//   | "PRESIDENTIAL"
//   | "LEGISLATIVE"
//   | "SENATORIAL"
//   | "REPRESENTATIVE"
//   | "REFERENDUM"
//   | "PRESIDENTIAL_GENERAL"
//   | "BY_ELECTION"
//   | "LOCAL";

// export type ElectionDto = {
//   electionId: string;
//   electionName: string;
//   year: number;
//   electionType: ElectionType;
//   isActive: boolean;
//   dateCreated: string;
//   dateUpdated: string;
// };

// export type ElectionCreateRequest = {
//   electionName: string;
//   year: number;
//   electionType: ElectionType;
//   isActive: boolean;
// };

// export type ElectionUpdateRequest = Partial<ElectionCreateRequest>;

// /** List all elections (global) */
// export async function fetchElections(): Promise<ElectionDto[]> {
//   const res = await apiClient.get<ElectionDto[]>("/elections");
//   return res.data;
// }

// /** Create election (global) */
// export async function createElection(
//   req: ElectionCreateRequest
// ): Promise<ElectionDto> {
//   const res = await apiClient.post<ElectionDto>("/elections", req);
//   return res.data;
// }

// /** Update election (global) */
// export async function updateElection(
//   electionId: string,
//   req: ElectionUpdateRequest
// ): Promise<ElectionDto> {
//   const res = await apiClient.put<ElectionDto>(`/elections/${electionId}`, req);
//   return res.data;
// }

// /** Delete election (global) */
// export async function deleteElection(electionId: string): Promise<void> {
//   await apiClient.delete(`/elections/${electionId}`);
// }

// /** Activate / Deactivate election (use PUT or PATCH depending on backend)
//  * If you don't have a dedicated endpoint, we just call updateElection with { isActive }.
//  */
// export async function setElectionActive(
//   electionId: string,
//   isActive: boolean
// ): Promise<ElectionDto> {
//   const res = await apiClient.patch<ElectionDto>(
//     `/elections/${electionId}/active`,
//     null,
//     { params: { active: isActive } }
//   );
//   return res.data;
// }
