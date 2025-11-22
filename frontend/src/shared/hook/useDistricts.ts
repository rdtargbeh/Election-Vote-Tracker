
// src/shared/hooks/useDistricts.ts
// ------------------------------------------------------
// Loads districts for a given county.
// ------------------------------------------------------

import { useQuery } from "@tanstack/react-query";
import {
  fetchDistrictsByCounty,
  type DistrictDto,
} from "../services/geographyService";

export function useDistricts(countyId: string | null) {
  return useQuery<DistrictDto[]>({
    queryKey: ["districts", countyId],
    enabled: !!countyId,
    queryFn: () => {
      if (!countyId) {
        throw new Error("countyId is required to load districts");
      }
      return fetchDistrictsByCounty(countyId);
    },
    staleTime: 2 * 60_000, // 2 minutes
  });
}
