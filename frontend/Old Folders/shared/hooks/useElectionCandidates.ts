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
  listElectionCandidatesByElection,
  type ElectionCandidateDto,
} from "../services/electionCandidateService";

export function useElectionCandidates(electionId: string | null) {
  return useQuery<ElectionCandidateDto[], Error>({
    queryKey: ["election-candidates", electionId],
    queryFn: () => listElectionCandidatesByElection(electionId as string),
    enabled: !!electionId,
  });
}
