// src/shared/services/voteSubmissionService.ts
import { apiClient } from "../lib/apiClient";

export type VoteSubmissionDto = {
  submissionId: string;

  orgId?: string;
  orgName?: string;

  electionId: string;
  electionName?: string;
  year?: number;

  countyId?: string | null;
  countyName?: string | null;

  districtId?: string | null;
  districtName?: string | null;

  centerId?: string;
  centerCode?: string | null;
  centerName?: string;

  placeId?: string;
  placeCode?: string | null;
  placeNumber?: number | null;
  placeLabel?: string | null;

  contestId: string;
  contestName?: string;
  contestCategory?: string;
  contestScopeType?: string;

  agentId?: string;
  agentName?: string;

  verifiedBy?: string;
  verifiedByName?: string;
  dateVerified?: string;

  submissionTime?: string;

  candidateVotes?: Record<string, number>;

  // --- ballots ---
  ballotsCast?: number;

  invalidBallots?: number;
  unmarkedBallots?: number; //
  spoiledBallots?: number;
  rejectedBallots?: number;
  unusedBallots?: number;

  // --- derived helpers ---
  validVotes?: number;
  invalidTotal?: number;
  turnoutPct?: number;
  invalidPct?: number;

  // --- allocation read-only (derived from place allocation) ---
  registeredVoters?: number;
  ballotsIssued?: number;
  allocationSource?: "PLACE" | "CENTER" | "NONE" | string;

  status?: string;
  comments?: string;

  latitude?: number;
  longitude?: number;

  tallySheetUrl?: string | null;
  tallySheetCount?: number | null;
};

// CreateRequest
export type VoteSubmissionCreateRequest = {
  orgId: string;
  electionId: string;
  centerId: string;
  placeId: string;

  agentId: string;
  contestId: string;

  candidateVotes: Record<string, number>;

  // backend still accepts/uses it (you compute it in UI)
  ballotsCast?: number;

  invalidBallots?: number;
  unmarkedBallots?: number;
  rejectedBallots?: number;
  spoiledBallots?: number;
  unusedBallots?: number;

  comments?: string;

  latitude?: number;
  longitude?: number;

  idempotencyKey?: string;
};

export type VoteSubmissionUpdateRequest = {
  candidateVotes?: Record<string, number>;

  // if backend allows updating cast, keep optional; otherwise remove
  ballotsCast?: number;
  invalidBallots?: number;
  unmarkedBallots?: number;
  rejectedBallots?: number;
  spoiledBallots?: number;
  unusedBallots?: number;

  comments?: string;

  latitude?: number;
  longitude?: number;
};

export type VoteSubmissionVerifyRequest = {
  note?: string;
  status?: "VERIFIED" | "FLAGGED";
};

export type PageResult<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

function mapSpringPage<T>(p: any): PageResult<T> {
  const items = (p?.content ?? []) as T[];
  return {
    items,
    page: Number(p?.number ?? 0),
    size: Number(p?.size ?? items.length ?? 0),
    totalItems: Number(p?.totalElements ?? items.length ?? 0),
    totalPages: Math.max(1, Number(p?.totalPages ?? 1)),
  };
}

export async function searchSubmissions(params: {
  page?: number;
  size?: number;

  orgId?: string;
  electionId?: string;

  countyId?: string;
  districtId?: string;
  centerId?: string;
  placeId?: string;

  contestId?: string;
  agentId?: string;

  status?: string;
  from?: string;
  to?: string;

  q?: string;
  category?: string;
  scopeType?: string;
}): Promise<PageResult<VoteSubmissionDto>> {
  const res = await apiClient.get("/vote-submissions", {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 20,

      orgId: params.orgId,
      electionId: params.electionId,

      countyId: params.countyId,
      districtId: params.districtId,
      centerId: params.centerId,
      placeId: params.placeId,

      contestId: params.contestId,
      agentId: params.agentId,

      status: params.status,
      from: params.from,
      to: params.to,

      q: params.q,
      category: params.category,
      scopeType: params.scopeType,
    },
  });

  return mapSpringPage<VoteSubmissionDto>(res.data);
}

/** JSON create (no files) */
export async function createSubmissionJson(
  req: VoteSubmissionCreateRequest
): Promise<VoteSubmissionDto> {
  const res = await apiClient.post("/vote-submissions", req);
  return res.data as VoteSubmissionDto;
}

/** Multipart create (tally sheet required) */
export async function createSubmissionMultipart(params: {
  payload: VoteSubmissionCreateRequest;
  files: File[];
}): Promise<VoteSubmissionDto> {
  const fd = new FormData();
  fd.append(
    "payload",
    new Blob([JSON.stringify(params.payload)], { type: "application/json" })
  );
  params.files.forEach((f) => fd.append("files", f));

  const res = await apiClient.post("/vote-submissions", fd, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data as VoteSubmissionDto;
}

export async function getSubmission(id: string): Promise<VoteSubmissionDto> {
  const res = await apiClient.get(`/vote-submissions/${id}`);
  return res.data as VoteSubmissionDto;
}

/** JSON update (no files) */
export async function updateSubmissionJson(
  id: string,
  req: VoteSubmissionUpdateRequest
): Promise<VoteSubmissionDto> {
  const res = await apiClient.put(`/vote-submissions/${id}`, req);
  return res.data as VoteSubmissionDto;
}

/** Multipart update (optional files) */
export async function updateSubmissionMultipart(params: {
  id: string;
  payload: VoteSubmissionUpdateRequest;
  files?: File[];
}): Promise<VoteSubmissionDto> {
  const fd = new FormData();
  fd.append(
    "payload",
    new Blob([JSON.stringify(params.payload)], { type: "application/json" })
  );
  (params.files ?? []).forEach((f) => fd.append("files", f));

  const res = await apiClient.put(`/vote-submissions/${params.id}`, fd, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data as VoteSubmissionDto;
}

export async function deleteSubmission(id: string): Promise<void> {
  await apiClient.delete(`/vote-submissions/${id}`);
}

export async function verifySubmission(
  id: string,
  req: VoteSubmissionVerifyRequest
): Promise<VoteSubmissionDto> {
  const res = await apiClient.post(`/vote-submissions/${id}/verify`, req);
  return res.data as VoteSubmissionDto;
}
