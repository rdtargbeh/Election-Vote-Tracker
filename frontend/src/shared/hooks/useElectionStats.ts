// src/shared/hooks/useElectionStats.ts
// ------------------------------------------------------
// React Query hook to load org + election summary stats.
// Uses canonical types from src/shared/types/api.ts and
// returns typed ApiError on failure.
//
// Backend endpoint:
//   GET /api/elections/org-election/stats?orgId=<UUID>&electionId=<UUID>
// ------------------------------------------------------

import { useQuery } from "@tanstack/react-query";
import { apiClient } from "../lib/apiClient";
import type { ElectionStatsDto, ApiError } from "../types/api";

export function useElectionStats(
  orgId: string | null,
  electionId: string | null
) {
  return useQuery<ElectionStatsDto, ApiError>({
    queryKey: ["election-stats", orgId, electionId],
    enabled: !!orgId && !!electionId,
    retry: false,
    queryFn: async () => {
      if (!orgId || !electionId) {
        throw new Error("orgId and electionId are required");
      }
      const res = await apiClient.get<ElectionStatsDto>(
        "/elections/org-election/stats",
        { params: { orgId, electionId } }
      );
      return res.data;
    },
    staleTime: 60_000, // 1 minute caching
  });
}

// // src/shared/hooks/useElectionStats.ts
// // ------------------------------------------------------
// // React Query hook to load summary stats for a given
// // orgId + electionId.
// //
// // Backend endpoint:
// //   GET /api/elections/org-election/stats?orgId=<UUID>&electionId=<UUID>
// // ------------------------------------------------------

// import { useQuery } from "@tanstack/react-query";
// import { apiClient } from "../lib/apiClient";

// export interface ElectionStatsDto {
//   orgId: string;
//   electionId: string;

//   registeredVoters: number;
//   ballotsCast: number;
//   validVotes: number;
//   invalidTotal: number;

//   turnoutPct: number | null;
//   invalidPct: number | null;
// }

// export function useElectionStats(
//   orgId: string | null,
//   electionId: string | null
// ) {
//   return useQuery<ElectionStatsDto, Error>({
//     queryKey: ["election-stats", orgId, electionId],
//     enabled: !!orgId && !!electionId, // 🔒 only fire when both are set
//     retry: false, // 🔒 don't keep retrying on 500
//     queryFn: async () => {
//       if (!orgId || !electionId) {
//         throw new Error("orgId and electionId are required");
//       }

//       const res = await apiClient.get<ElectionStatsDto>(
//         "/elections/org-election/stats",
//         {
//           params: { orgId, electionId },
//         }
//       );

//       return res.data;
//     },
//     staleTime: 60_000, // 1 minute caching
//   });
// }
