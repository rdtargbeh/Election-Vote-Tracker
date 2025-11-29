// src/shared/services/voteSubmissionService.ts
// ------------------------------------------------------
// Service wrapper for vote submissions.
//
// Backend endpoints:
//
//  POST   /api/vote-submissions           (multipart, @RequestPart("payload"))
//  PUT    /api/vote-submissions/{id}      (multipart, @RequestPart("payload"))
//  GET    /api/vote-submissions           (paged search)
//  GET    /api/vote-submissions/{id}      (single by id)
//  DELETE /api/vote-submissions/{id}
// ------------------------------------------------------

import { apiClient } from "../lib/apiClient";

// ---------- Shared types ----------

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // page index
}

export type VoteStatus = "PENDING" | "VERIFIED" | "FLAGGED" | "REJECTED";

export type CandidateVotesMap = Record<string, number>;

// This matches your backend VoteSubmissionDto + mapper,
// but keeps many fields optional so the UI is resilient.
export interface VoteSubmissionDto {
  submissionId: string;

  orgId: string;
  orgName?: string;

  electionId: string;
  electionName?: string;
  year?: number;

  // Polling center
  centerId: string;
  centerCode?: string;
  centerName?: string;

  // Polling place
  placeId: string;
  placeCode?: string;
  placeNumber?: number | null;
  placeLabel?: string | null;

  // Agent
  agentId: string;
  agentName?: string;

  submissionTime?: string | null; // ISO string from LocalDateTime

  // Votes
  candidateVotes?: CandidateVotesMap;

  ballotsCast?: number | null;
  invalidBallots?: number | null;
  blankBallots?: number | null;
  rejectedBallots?: number | null;
  spoiledBallots?: number | null;

  status: VoteStatus;
  comments?: string | null;

  // GPS (from latitude / longitude)
  latitude?: number | null;
  longitude?: number | null;

  // Verification / audit
  verifiedBy?: string | null;
  verifiedByName?: string | null;
  dateVerified?: string | null;

  clientIp?: string | null;
  userAgent?: string | null;
  submissionHash?: string | null;

  version?: number | null;

  // Derived helpers (your DTO + mapper)
  validVotes?: number | null;
  invalidTotal?: number | null;
  turnoutPct?: number | null;
  invalidPct?: number | null;
}

// Shape of the create request as the backend expects it
// (names should line up with your VoteSubmissionCreateRequest).
export interface VoteSubmissionCreatePayload {
  orgId: string;
  electionId: string;
  centerId: string;
  placeId: string;
  agentId: string;

  candidateVotes: CandidateVotesMap;

  ballotsCast: number;
  invalidBallots?: number;
  blankBallots?: number;
  rejectedBallots?: number;
  spoiledBallots?: number;

  comments?: string;

  // Optional extras (if supported by your request DTO)
  latitude?: number;
  longitude?: number;
}

// For update we only send what may change.
export interface VoteSubmissionUpdatePayload {
  candidateVotes?: CandidateVotesMap;

  ballotsCast?: number;
  invalidBallots?: number;
  blankBallots?: number;
  rejectedBallots?: number;
  spoiledBallots?: number;

  comments?: string;
}

// Search filters line up with controller's @RequestParam signature
export interface VoteSubmissionQuery {
  orgId?: string;
  electionId?: string;
  centerId?: string;
  agentId?: string;
  status?: VoteStatus;
  from?: string; // ISO date-time string
  to?: string; // ISO date-time string
  q?: string;
  page?: number;
  size?: number;
}

// ---------- Helpers to build multipart ----------

function buildMultipartForm(
  payload: VoteSubmissionCreatePayload | VoteSubmissionUpdatePayload,
  files?: File[]
): FormData {
  const formData = new FormData();

  // Spring @RequestPart("payload") expects JSON.
  // Using Blob keeps the content-type application/json.
  const json = JSON.stringify(payload);
  const blob = new Blob([json], { type: "application/json" });
  formData.append("payload", blob);

  if (files && files.length > 0) {
    for (const f of files) {
      formData.append("files", f);
    }
  }

  return formData;
}

// ---------- API functions ----------

/**
 * Paged search for vote submissions.
 * Mirrors:
 *   GET /api/vote-submissions
 */
export async function fetchVoteSubmissions(
  params: VoteSubmissionQuery = {}
): Promise<PageResponse<VoteSubmissionDto>> {
  const {
    orgId,
    electionId,
    centerId,
    agentId,
    status,
    from,
    to,
    q,
    page = 0,
    size = 20,
  } = params;

  const res = await apiClient.get<PageResponse<VoteSubmissionDto>>(
    "/vote-submissions",
    {
      params: {
        orgId,
        electionId,
        centerId,
        agentId,
        status,
        from,
        to,
        q: q && q.trim().length > 0 ? q.trim() : undefined,
        page,
        size,
      },
    }
  );

  return res.data;
}

/**
 * Load a single submission by id.
 * Mirrors:
 *   GET /api/vote-submissions/{id}
 */
export async function fetchVoteSubmission(
  submissionId: string
): Promise<VoteSubmissionDto> {
  const res = await apiClient.get<VoteSubmissionDto>(
    `/vote-submissions/${submissionId}`
  );
  return res.data;
}

/**
 * Create a new vote submission.
 * Mirrors:
 *   POST /api/vote-submissions  (multipart/form-data)
 */
export async function createVoteSubmission(
  payload: VoteSubmissionCreatePayload,
  files?: File[]
): Promise<VoteSubmissionDto> {
  const formData = buildMultipartForm(payload, files);

  const res = await apiClient.post<VoteSubmissionDto>(
    "/vote-submissions",
    formData,
    {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    }
  );

  return res.data;
}

/**
 * Update an existing vote submission.
 * Mirrors:
 *   PUT /api/vote-submissions/{id}  (multipart/form-data)
 */
export async function updateVoteSubmission(
  submissionId: string,
  payload: VoteSubmissionUpdatePayload,
  files?: File[]
): Promise<VoteSubmissionDto> {
  const formData = buildMultipartForm(payload, files);

  const res = await apiClient.put<VoteSubmissionDto>(
    `/vote-submissions/${submissionId}`,
    formData,
    {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    }
  );

  return res.data;
}

/**
 * Delete a submission.
 * Mirrors:
 *   DELETE /api/vote-submissions/{id}
 */
export async function deleteVoteSubmission(
  submissionId: string
): Promise<void> {
  await apiClient.delete(`/vote-submissions/${submissionId}`);
}
