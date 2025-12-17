// src/shared/hooks/useElectionCandidates.ts
// ------------------------------------------------------
// React Query hook to load all candidates on a ballot
// for a given election.
// ------------------------------------------------------

// src/shared/hooks/useElectionCandidates.ts
// ------------------------------------------------------
// React Query hook to load all candidates on a ballot
// for a given election.
// ------------------------------------------------------

import { useQuery } from "@tanstack/react-query";
import {
  fetchElectionCandidatesByElection,
  type ElectionCandidateDto,
} from "../services/candidateService";

export function useElectionCandidates(electionId: string | null) {
  return useQuery<ElectionCandidateDto[], Error>({
    queryKey: ["election-candidates", electionId],
    queryFn: () => fetchElectionCandidatesByElection(electionId as string),
    enabled: !!electionId,
  });
}
