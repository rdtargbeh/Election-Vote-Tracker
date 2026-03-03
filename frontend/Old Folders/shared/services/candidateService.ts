// // src/shared/services/candidateService.ts
// // ------------------------------------------------------
// // Read-only service for candidate lookups.
// // Backend endpoint:
// //   GET /api/election-candidates/{electionId}/candidates
// // ------------------------------------------------------

// import { apiClient } from "../lib/apiClient";

// export interface CandidateDto {
//   candidateId: string;
//   fullName: string;

//   // optional fields if your backend returns them
//   partyId?: string | null;
//   partyAbbrev?: string | null;
//   independent?: boolean;
// }

// export type CandidateOption = {
//   value: string;
//   label: string;
//   partyAbbrev?: string | null;
// };

// export async function fetchCandidates(): Promise<CandidateDto[]> {
//   const res = await apiClient.get<CandidateDto[]>("/candidates");
//   return res.data;
// }

// export function toCandidateOptions(list: CandidateDto[]): CandidateOption[] {
//   return (list ?? []).map((c) => ({
//     value: c.candidateId,
//     label: c.fullName,
//     partyAbbrev: c.partyAbbrev ?? null,
//   }));
// }

// // import { apiClient } from "../lib/apiClient";

// // export interface ElectionCandidateDto {
// //   electId: string;
// //   electionId: string;
// //   electionName: string;

// //   candidateId: string;
// //   fullName: string;

// //   centerId?: string | null;
// //   centerName?: string | null;
// // }

// // /**
// //  * Candidates on the ballot for a given election.
// //  * Mirrors:
// //  *   GET /api/election-candidates/{electionId}/candidates
// //  */
// // export async function fetchElectionCandidatesByElection(
// //   electionId: string
// // ): Promise<ElectionCandidateDto[]> {
// //   const res = await apiClient.get<ElectionCandidateDto[]>(
// //     `/election-candidates/${electionId}/candidates`
// //   );
// //   return res.data;
// // }
