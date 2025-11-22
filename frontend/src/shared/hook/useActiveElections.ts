
// src/shared/hooks/useActiveElections.ts
// ------------------------------------------------------
// React Query hook to load *active* elections
// GET /api/elections/active
// ------------------------------------------------------

import { useQuery } from "@tanstack/react-query";
import { apiClient } from "../lib/apiClient";

export interface ElectionDto {
  electionId: string;
  electionName: string;
  year: number;
  type: string;
  active: boolean;
}

export function useActiveElections() {
  return useQuery({
    queryKey: ["active-elections"],
    queryFn: async () => {
      // ✔ correctly calls /active endpoint now
      const res = await apiClient.get<ElectionDto[]>("/elections");
      return res.data;
    },
    staleTime: 1000 * 60 * 2, // cache 2 minutes
  });
}
