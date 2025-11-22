

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
import { fetchCounties, type CountyDto } from "../services/countyService";

/**
 * Simple counties hook.
 *
 * Example:
 *   const { data: counties } = useCounties();
 *   counties?.map(c => <option ...>{c.countyName}</option>)
 */
export function useCounties(q?: string) {
  return useQuery<CountyDto[], Error>({
    queryKey: ["counties-simple", { q }],
    queryFn: () => fetchCounties(q),
    staleTime: 5 * 60_000, // 5 minutes
  });
}
