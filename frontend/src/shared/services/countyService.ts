import { apiClient } from "../lib/apiClient";

export interface CountyDto {
  countyId: string;
  countyName: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface CountyPageParams {
  q?: string;
  page?: number;
  size?: number;
}

const tenantHeaders = (orgId?: string | null) =>
  orgId ? { headers: { "X-Org-Id": orgId } } : undefined;

export async function fetchCountiesPage(
  params: CountyPageParams = {},
  orgId?: string | null
): Promise<PageResponse<CountyDto>> {
  const { q, page = 0, size = 20 } = params;

  const res = await apiClient.get<PageResponse<CountyDto>>("/counties", {
    ...(tenantHeaders(orgId) ?? {}),
    params: {
      q: q && q.trim().length > 0 ? q.trim() : undefined,
      page,
      size,
    },
  });

  return res.data;
}

export async function fetchCounties(
  q?: string,
  orgId?: string | null
): Promise<CountyDto[]> {
  const page = await fetchCountiesPage({ q, page: 0, size: 1000 }, orgId);
  return page.content ?? [];
}
