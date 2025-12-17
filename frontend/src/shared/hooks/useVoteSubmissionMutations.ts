// src/shared/hooks/useVoteSubmissionMutations.ts
// ------------------------------------------------------
// Mutation hooks updated to always call multipart createVoteSubmission
// (the backend expects multipart @RequestPart("payload") for POST).
// - useCreateVoteSubmission now calls createVoteSubmission regardless of files presence.
// - The rest of the hooks reuse the service functions from services/voteSubmissionService.
// ------------------------------------------------------

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "../lib/apiClient";
import {
  createVoteSubmission,
  updateVoteSubmission,
  deleteVoteSubmission,
  verifyVoteSubmission,
} from "../services/voteSubmissionService";
import type {
  VoteSubmissionCreatePayload,
  VoteSubmissionUpdatePayload,
  VoteSubmissionDto,
  ApiError,
} from "../types/api";

export interface VoteSubmissionVerifyRequest {
  verifierUserId: string;
  accept: boolean;
  comment?: string | null;
}

/** Create submission (multipart) */
export function useCreateVoteSubmission() {
  const qc = useQueryClient();
  return useMutation<
    VoteSubmissionDto,
    ApiError,
    { payload: VoteSubmissionCreatePayload; files?: File[] }
  >({
    mutationFn: async ({ payload, files }) => {
      return await createVoteSubmission(payload, files);
    },
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ["vote-submissions"] });
      qc.invalidateQueries({
        queryKey: ["election-stats", data.orgId, data.electionId],
      });
    },
  });
}

/** Update submission */
export function useUpdateVoteSubmission() {
  const qc = useQueryClient();
  return useMutation<
    VoteSubmissionDto,
    ApiError,
    { id: string; payload: VoteSubmissionUpdatePayload; files?: File[] }
  >({
    mutationFn: async ({ id, payload, files }) =>
      updateVoteSubmission(id, payload, files),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ["vote-submissions"] });
      qc.invalidateQueries({
        queryKey: ["vote-submission", data.submissionId],
      });
      qc.invalidateQueries({
        queryKey: ["election-stats", data.orgId, data.electionId],
      });
    },
  });
}

/** Verify submission */
export function useVerifySubmission() {
  const qc = useQueryClient();
  return useMutation<
    VoteSubmissionDto,
    ApiError,
    { id: string; body: VoteSubmissionVerifyRequest }
  >({
    mutationFn: async ({ id, body }) => verifyVoteSubmission(id, body),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ["vote-submissions"] });
      qc.invalidateQueries({
        queryKey: ["vote-submission", data.submissionId],
      });
      qc.invalidateQueries({
        queryKey: ["election-stats", data.orgId, data.electionId],
      });
    },
  });
}

/** Delete submission */
export function useDeleteSubmission() {
  const qc = useQueryClient();
  return useMutation<
    void,
    ApiError,
    { id: string; orgId?: string; electionId?: string }
  >({
    mutationFn: async ({ id }) => {
      await deleteVoteSubmission(id);
    },
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ["vote-submissions"] });
      if (variables?.orgId && variables?.electionId) {
        qc.invalidateQueries({
          queryKey: ["election-stats", variables.orgId, variables.electionId],
        });
      }
    },
  });
}
