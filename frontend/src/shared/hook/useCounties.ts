
// src/shared/hooks/useCounties.ts
// ------------------------------------------------------
// Loads all counties (for dropdowns / filters).
// ------------------------------------------------------

import { useQuery } from "@tanstack/react-query";
import { fetchCounties, type CountyDto } from "../services/geographyService";

export function useCounties() {
  return useQuery<CountyDto[]>({
    queryKey: ["counties"],
    queryFn: () => fetchCounties(),
    staleTime: 5 * 60_000, // 5 minutes
  });
}
