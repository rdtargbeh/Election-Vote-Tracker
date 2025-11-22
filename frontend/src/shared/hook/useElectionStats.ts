

// src/shared/hooks/useElectionStats.ts
// ------------------------------------------------------
// React Query hook to load summary stats for a given
// electionId, using the statsService wrapper.
//
// Backend endpoint (expected):
//   GET /api/stats/election-summary?electionId=<UUID>
// ------------------------------------------------------

import { useQuery } from "@tanstack/react-query";
import { apiClient } from "../lib/apiClient";

export interface ElectionStatsDto {
  orgId: string;
  electionId: string;

  registeredVoters: number;
  ballotsCast: number;
  validVotes: number;
  invalidTotal: number;

  turnoutPct: number | null;
  invalidPct: number | null;
}

export function useElectionStats(orgId: string | null, electionId: string | null) {
  return useQuery<ElectionStatsDto>({
    queryKey: ["election-stats", orgId, electionId],
    enabled: !!orgId && !!electionId,

    queryFn: async () => {
      if (!orgId || !electionId) {
        throw new Error("orgId and electionId are required");
      }

      const res = await apiClient.get<ElectionStatsDto>("/elections/org-election/stats", {
        params: { orgId, electionId },
      });

      return res.data;
    },
    staleTime: 60_000, // 1 minute caching
  });
}

