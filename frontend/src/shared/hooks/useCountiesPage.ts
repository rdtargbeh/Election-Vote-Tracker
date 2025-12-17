
// src/shared/hooks/useCountiesPage.ts
// ------------------------------------------------------
// Paged counties hook: returns PageResponse<CountyDto>.
//
// Use this for:
//  - admin/manage screen
//  - tables with pagination
//  - searchable lists
// ------------------------------------------------------

import { useQuery } from "@tanstack/react-query";
import {
  fetchCountiesPage,
  type CountyDto,
  type PageResponse,
  type CountyPageParams,
} from "../services/countyService";

export interface UseCountiesPageParams extends CountyPageParams {}

/**
 * Hook to load a paged list of counties.
 *
 * Example usage:
 *   const { data, isLoading } = useCountiesPage({ q, page, size });
 *   data?.content.map(...)
 */
export function useCountiesPage(params: UseCountiesPageParams = {}) {
  const { q, page = 0, size = 20 } = params;

  return useQuery<PageResponse<CountyDto>, Error>({
    queryKey: ["counties-page", { q, page, size }],
    queryFn: () => fetchCountiesPage({ q, page, size }),
    staleTime: 30_000, // 30 seconds is enough for admin lists
  });
}
