
// src/shared/api/useHealthCheck.ts
// ------------------------------------------------------------------
// Simple health check hook using React Query.
// Verifies that apiClient, headers, Axios interceptors,
// and network routing are all working correctly.
// ------------------------------------------------------------------

import { useQuery } from "@tanstack/react-query";
import { apiClient } from "../lib/apiClient";

export function useHealthCheck() {
  return useQuery({
    queryKey: ["health-check"],
    queryFn: async () => {
      const res = await apiClient.get("/health");
      return res.data;
    },
    retry: false,
  });
}
