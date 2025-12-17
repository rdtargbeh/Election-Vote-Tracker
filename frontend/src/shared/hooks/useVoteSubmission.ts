// src/shared/hooks/useVoteSubmission.ts
// ------------------------------------------------------
// React Query hook to load a single vote submission by id.
//
// Usage:
//   const { data: submission } = useVoteSubmission(submissionId);
// ------------------------------------------------------

import { useQuery } from "@tanstack/react-query";
import {
  fetchVoteSubmission,
  type VoteSubmissionDto,
} from "../services/voteSubmissionService";

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

// import { useQuery } from "@tanstack/react-query";
// import {
//   fetchVoteSubmission,
//   type VoteSubmissionDto,
// } from "../services/voteSubmissionService";

// export function useVoteSubmission(submissionId: string | null) {
//   return useQuery<VoteSubmissionDto, Error>({
//     queryKey: ["vote-submission", submissionId],
//     enabled: !!submissionId,
//     queryFn: () => {
//       if (!submissionId) {
//         // Should not run when disabled, but keeps TS happy
//         return Promise.reject(new Error("submissionId is required"));
//       }
//       return fetchVoteSubmission(submissionId);
//     },
//     staleTime: 60_000,
//   });
// }
