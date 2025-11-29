
// src/shared/services/electionService.ts
import { apiClient } from "../lib/apiClient";

export type ElectionDto = {
  electionId: string;
  electionName: string;
  year: number;
  electionType: string;
  isActive: boolean;
  dateCreated: string;
  dateUpdated: string;
};

// 1) Simple list: active elections (non-paged)
export async function fetchActiveElections(): Promise<ElectionDto[]> {
  const res = await apiClient.get<ElectionDto[]>("/elections");
  return res.data;
}

// 2) Search with paging
export type ElectionSearchParams = {
  q?: string;
  year?: number;
  type?: string;   // "PRESIDENTIAL", "LEGISLATIVE", etc.
  active?: boolean;
  page?: number;
  size?: number;
};

export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // current page index
};

export async function searchElections(
  params: ElectionSearchParams
): Promise<PageResponse<ElectionDto>> {
  const res = await apiClient.get<PageResponse<ElectionDto>>("/elections/search", {
    params,
  });
  return res.data;
}
