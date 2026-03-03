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
  return useQuery<DistrictDto[], Error>({
    queryKey: ["districts", { countyId }],
    queryFn: () =>
      countyId ? fetchDistrictsByCounty(countyId) : Promise.resolve([]),
    enabled: !!countyId,
    staleTime: 60_000,
  });
}
