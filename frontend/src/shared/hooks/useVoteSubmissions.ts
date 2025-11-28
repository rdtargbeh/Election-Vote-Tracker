// src/shared/hooks/useVoteSubmissions.ts
// ------------------------------------------------------
// React Query hook for paged vote submissions list.
//
// Usage example:
//
// const { data, isLoading } = useVoteSubmissions({
//   orgId: currentOrgId,
//   electionId: activeElectionId,
//   centerId: selectedCenterId,
//   page,
//   size,
// });
// ------------------------------------------------------

import { useQuery } from "@tanstack/react-query";
import {
  fetchVoteSubmissions,
  type VoteSubmissionDto,
  type PageResponse,
  type VoteSubmissionQuery,
} from "../services/voteSubmissionService";

export interface UseVoteSubmissionsParams extends VoteSubmissionQuery {}

export function useVoteSubmissions(params: UseVoteSubmissionsParams) {
  return useQuery<PageResponse<VoteSubmissionDto>, Error>({
    queryKey: ["vote-submissions", params],
    queryFn: () => fetchVoteSubmissions(params),
    staleTime: 30_000, // 30 seconds is fine for dashboard-style lists
  });
}
