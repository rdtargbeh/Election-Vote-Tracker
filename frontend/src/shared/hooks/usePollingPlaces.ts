// src/shared/hook/usePollingPlaces.ts
// ------------------------------------------------------
// React Query hook to load polling places for a given
// centerId.
// ------------------------------------------------------
// src/shared/hook/usePollingPlaces.ts
// ------------------------------------------------------
// React Query hook to load polling places for a given
// centerId.
// ------------------------------------------------------

import { useQuery } from "@tanstack/react-query";
import {
  fetchPollingPlacesByCenter,
  type PollingPlaceDto,
} from "../services/pollingPlaceService";

export function usePollingPlaces(centerId: string | null) {
  return useQuery<PollingPlaceDto[], Error>({
    queryKey: ["polling-places", centerId],
    enabled: !!centerId, // only run when we have a real centerId
    queryFn: () => {
      if (!centerId) {
        return Promise.resolve([]);
      }
      return fetchPollingPlacesByCenter(centerId);
    },
    staleTime: 60_000,
  });
}
