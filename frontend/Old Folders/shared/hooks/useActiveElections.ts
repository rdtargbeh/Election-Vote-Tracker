// src/shared/hooks/useActiveElections.ts
// ------------------------------------------------------
// React Query hook to load active elections.
// Uses canonical types from src/shared/types/api.ts and
// returns typed ApiError on failure.
// Backend endpoint used:
//   GET /api/elections?activeOnly=true
// ------------------------------------------------------

import { useQuery } from "@tanstack/react-query";
import { apiClient } from "../lib/apiClient";
import type { ElectionDto, ApiError } from "../types/api";

export function useActiveElections() {
  return useQuery<ElectionDto[], ApiError>({
    queryKey: ["elections", "active"],
    queryFn: async () => {
      const res = await apiClient.get<ElectionDto[]>("/elections", {
        params: { activeOnly: true },
      });
      return res.data;
    },
    staleTime: 1000 * 60 * 2, // 2 minutes
    retry: false, // let UI decide retry behavior
  });
}
