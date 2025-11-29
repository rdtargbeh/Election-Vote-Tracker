// src/shared/hooks/useObserverReports.ts
// ------------------------------------------------------
// React Query hooks for observer reports:
//   - useObserverReports: paged list
//   - useObserverReport:  single report by id
// ------------------------------------------------------

import { useQuery } from "@tanstack/react-query";
import {
  fetchObserverReports,
  fetchObserverReport,
  type ObserverReportDto,
  type ObserverReportQuery,
} from "../services/observerReportService";
import type { PageResponse } from "../services/voteSubmissionService";

export interface UseObserverReportsParams extends ObserverReportQuery {}

export function useObserverReports(params: UseObserverReportsParams) {
  return useQuery<PageResponse<ObserverReportDto>, Error>({
    queryKey: ["observer-reports", params],
    queryFn: () => fetchObserverReports(params),
    staleTime: 30_000,
  });
}

export function useObserverReport(reportId: string | null) {
  return useQuery<ObserverReportDto, Error>({
    queryKey: ["observer-report", reportId],
    enabled: !!reportId,
    queryFn: () => {
      if (!reportId) {
        return Promise.reject(new Error("reportId is required"));
      }
      return fetchObserverReport(reportId);
    },
    staleTime: 60_000,
  });
}
