// src/shared/hooks/useCounties.ts
// ------------------------------------------------------
// Simple counties hook: returns an array<CountyDto>.
//
// Use this for:
//  - dropdowns
//  - simple lists without pagination
//
// Under the hood it still calls /api/counties but unwraps
// the PageResponse into a plain array.
// ------------------------------------------------------

import { useQuery } from "@tanstack/react-query";
import { fetchCounties, type CountyDto } from "../services/geographyService";

export function useCounties() {
  return useQuery<CountyDto[], Error>({
    queryKey: ["counties"],
    queryFn: () => fetchCounties(),
    staleTime: 5 * 60_000, // 5 minutes - fairly static reference data
  });
}
