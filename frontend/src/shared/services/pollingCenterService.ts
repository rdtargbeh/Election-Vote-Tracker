
// src/shared/services/pollingCenterService.ts
// ------------------------------------------------------
// Service wrapper for Polling Centers.
//
// Backend expectation:
//   GET /api/polling-centers?districtId=<UUID>&q=<optional>
//
// This returns a simple list of PollingCenterDto.
// ------------------------------------------------------

import { apiClient } from "../lib/apiClient";

export interface PollingCenterDto {
  centerId: string;
  centerName: string;
  code: string;
  registeredVoters: number;

  // Optional extra fields if your backend includes them
  districtId?: string;
  districtName?: string;
  countyId?: string;
  countyName?: string;
}

export interface PollingCenterQuery {
  districtId?: string | null;
  q?: string;
}

/**
 * Fetch polling centers from the backend.
 */
export async function fetchPollingCenters(
  params: PollingCenterQuery = {}
): Promise<PollingCenterDto[]> {
  const { districtId, q } = params;

  const searchParams = new URLSearchParams();
  if (districtId) searchParams.append("districtId", districtId);
  if (q && q.trim()) searchParams.append("q", q.trim());

  const queryString = searchParams.toString();
  const url = queryString
    ? `/polling-centers?${queryString}`
    : "/polling-centers";

  const res = await apiClient.get<PollingCenterDto[]>(url);
  return res.data;
}
