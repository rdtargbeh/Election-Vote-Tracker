
// src/shared/services/geographyService.ts
// ------------------------------------------------------
// Small service layer for counties and districts.
//
// Backend assumptions (Spring Data style):
//   GET /api/counties
//     -> Page<CountyDto> { content: CountyDto[], ... }
//   GET /api/districts?countyId=<uuid>
//     -> Page<DistrictDto> { content: DistrictDto[], ... }
//
// If your controller names differ, only adjust the paths.
// ------------------------------------------------------

import { apiClient } from "../lib/apiClient";

export interface CountyDto {
  countyId: string;
  countyName: string;
}

export interface DistrictDto {
  districtId: string;
  districtName: string;
  countyId: string;
  countyName: string;
}

// Generic Spring Data page wrapper
interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // current page
  size: number;
}

export async function fetchCounties(q?: string): Promise<CountyDto[]> {
  const res = await apiClient.get<PageResult<CountyDto>>("/counties", {
    params: {
      q: q && q.trim().length > 0 ? q.trim() : undefined,
      page: 0,
      size: 100, // enough for Liberia counties
    },
  });

  return res.data.content;
}

export async function fetchDistrictsByCounty(
  countyId: string
): Promise<DistrictDto[]> {
  const res = await apiClient.get<PageResult<DistrictDto>>("/districts", {
    params: {
      countyId,
      page: 0,
      size: 200, // districts per county
    },
  });

  return res.data.content;
}
