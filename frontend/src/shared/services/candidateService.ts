// src/shared/services/candidateService.ts
// ------------------------------------------------------
// Read-only service for candidate lookups.
// Backend endpoint:
//   GET /api/election-candidates/{electionId}/candidates
// ------------------------------------------------------

import { apiClient } from "../lib/apiClient";

export interface ElectionCandidateDto {
  electId: string;
  electionId: string;
  electionName: string;

  candidateId: string;
  fullName: string;

  centerId?: string | null;
  centerName?: string | null;
}

/**
 * Candidates on the ballot for a given election.
 * Mirrors:
 *   GET /api/election-candidates/{electionId}/candidates
 */
export async function fetchElectionCandidatesByElection(
  electionId: string
): Promise<ElectionCandidateDto[]> {
  const res = await apiClient.get<ElectionCandidateDto[]>(
    `/election-candidates/${electionId}/candidates`
  );
  return res.data;
}
