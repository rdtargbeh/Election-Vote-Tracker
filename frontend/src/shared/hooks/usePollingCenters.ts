// src/shared/hook/usePollingCenters.ts
// ------------------------------------------------------
// React Query hook to load polling centers for a given
// districtId.
//
// Uses pollingCenterService:
//   GET /api/polling-centers?districtId=...
// ------------------------------------------------------

// src/shared/hook/usePollingCenters.ts
// ------------------------------------------------------
// Loads polling centers for a given district.
// Pattern is identical to useDistricts.
// ------------------------------------------------------

import { useQuery } from "@tanstack/react-query";
import {
  fetchPollingCenters,
  type PollingCenterDto,
} from "../services/pollingCenterService";

export function usePollingCenters(districtId: string | null) {
  return useQuery<PollingCenterDto[], Error>({
    queryKey: ["polling-centers", { districtId }],
    queryFn: () => {
      if (!districtId) {
        // Should never run when disabled, but keeps types happy
        return Promise.resolve([]);
      }
      return fetchPollingCenters({ districtId });
    },
    enabled: !!districtId, // only run when we actually have a districtId
    staleTime: 60_000, // 1 minute is fine for reference data
  });
}
