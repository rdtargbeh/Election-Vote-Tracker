// src/shared/hooks/useVoteSubmission.ts
// ------------------------------------------------------
// React Query hook to load a single vote submission by id.
//
// Usage:
//   const { data: submission } = useVoteSubmission(submissionId);
// ------------------------------------------------------

import { useQuery } from "@tanstack/react-query";
import { fetchVoteSubmission } from "../services/voteSubmissionService";
import type { VoteSubmissionDto } from "../types/api";

export function useVoteSubmission(submissionId: string | null | undefined) {
  return useQuery<VoteSubmissionDto>({
    queryKey: ["voteSubmission", submissionId],
    queryFn: () => {
      if (!submissionId) {
        throw new Error("No submissionId provided");
      }
      return fetchVoteSubmission(submissionId);
    },
    enabled: !!submissionId,
  });
}
