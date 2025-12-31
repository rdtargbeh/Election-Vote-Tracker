// import { apiClient } from "../lib/apiClient";

// export type ElectionCandidateDto = {
//   electId: string;

//   electionId: string;
//   electionName: string;

//   candidateId: string;
//   fullName: string;

//   centerId: string | null;
//   centerName: string | null;

//   partyId: string | null;
//   partyAbbrev: string | null;

//   dateCreated: string;
//   dateUpdated: string;
// };

// export type ElectionCandidateCreateRequest = {
//   electionId: string;
//   candidateId: string;
//   centerId?: string | null;
// };

// export type ElectionCandidateUpdateRequest = {
//   centerId?: string | null;
// };

// export async function listElectionCandidatesByElection(
//   electionId: string
// ): Promise<ElectionCandidateDto[]> {
//   const res = await apiClient.get<ElectionCandidateDto[]>(
//     `/election-candidates/${electionId}/candidates`
//   );
//   return res.data;
// }

// /**
//  * ✅ Assign candidate to election (optional center)
//  * POST /api/election-candidates
//  */
// export async function createElectionCandidate(
//   req: ElectionCandidateCreateRequest
// ): Promise<ElectionCandidateDto> {
//   const res = await apiClient.post<ElectionCandidateDto>(
//     "/election-candidates",
//     {
//       electionId: req.electionId,
//       candidateId: req.candidateId,
//       centerId: req.centerId ?? null,
//     }
//   );
//   return res.data;
// }

// /**
//  * ✅ Update assignment (center only)
//  * PUT /api/election-candidates/{electId}
//  */
// export async function updateElectionCandidate(
//   electId: string,
//   req: ElectionCandidateUpdateRequest
// ): Promise<ElectionCandidateDto> {
//   const res = await apiClient.put<ElectionCandidateDto>(
//     `/election-candidates/${electId}`,
//     {
//       centerId: req.centerId ?? null,
//     }
//   );
//   return res.data;
// }

// /**
//  * ✅ Delete assignment
//  * DELETE /api/election-candidates/{electId}
//  */
// export async function deleteElectionCandidate(electId: string): Promise<void> {
//   await apiClient.delete(`/election-candidates/${electId}`);
// }
