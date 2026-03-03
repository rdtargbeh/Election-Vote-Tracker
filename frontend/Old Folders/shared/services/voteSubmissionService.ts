// // src/shared/services/voteSubmissionService.ts
// // Create/update vote submissions with both JSON and multipart behavior.

// import { apiClient } from "../lib/apiClient";
// import { ensureIdempotencyFields } from "../lib/submissionHash";
// import type {
//   VoteSubmissionCreatePayload,
//   VoteSubmissionUpdatePayload,
//   VoteSubmissionDto,
//   PageResponse,
// } from "../types/api";

// /** Helper to build multipart FormData with a JSON "submission" part and optional files */
// function buildMultipartForm(
//   payload: VoteSubmissionCreatePayload | VoteSubmissionUpdatePayload,
//   files?: File[]
// ): FormData {
//   const formData = new FormData();
//   const json = JSON.stringify(payload);
//   const blob = new Blob([json], { type: "application/json" });
//   formData.append("submission", blob);
//   if (files && files.length > 0) {
//     for (const f of files) {
//       formData.append("files", f);
//     }
//   }
//   return formData;
// }

// /** Paged search for vote submissions */
// export async function fetchVoteSubmissions(
//   params: {
//     orgId?: string;
//     electionId?: string;
//     centerId?: string;
//     agentId?: string;
//     status?: string;
//     from?: string;
//     to?: string;
//     q?: string;
//     page?: number;
//     size?: number;
//   } = {}
// ): Promise<PageResponse<VoteSubmissionDto>> {
//   const res = await apiClient.get<PageResponse<VoteSubmissionDto>>(
//     "/vote-submissions",
//     {
//       params: {
//         ...params,
//         q: params.q && params.q.trim().length > 0 ? params.q.trim() : undefined,
//       },
//     }
//   );
//   return res.data;
// }

// /** Load single submission */
// export async function fetchVoteSubmission(
//   submissionId: string
// ): Promise<VoteSubmissionDto> {
//   const res = await apiClient.get<VoteSubmissionDto>(
//     `/vote-submissions/${submissionId}`
//   );
//   return res.data;
// }

// /** Create or update a vote submission (atomic flow for multipart) */
// export async function createVoteSubmission(
//   payload: VoteSubmissionCreatePayload,
//   files?: File[]
// ): Promise<VoteSubmissionDto> {
//   // Compute idempotency fields (required for all submissions)
//   const { clientGuid, submissionHash } = await ensureIdempotencyFields(payload);

//   const payloadWithIds: VoteSubmissionCreatePayload = {
//     ...payload,
//     clientGuid,
//     submissionHash,
//   };

//   // If no files are included, fallback to JSON submission
//   if (!files || files.length === 0) {
//     const res = await apiClient.post<VoteSubmissionDto>(
//       "/vote-submissions",
//       payloadWithIds
//     );
//     return res.data;
//   }

//   // Atomic submission with files
//   const form = buildMultipartForm(payloadWithIds, files);
//   const res = await apiClient.post<VoteSubmissionDto>(
//     "/vote-submissions/atomic-upload", // Updated endpoint for atomic operation
//     form
//   );

//   return res.data;
// }

// /** Update a vote submission (uses multipart for file uploads) */
// export async function updateVoteSubmission(
//   submissionId: string,
//   payload: VoteSubmissionUpdatePayload,
//   files?: File[]
// ): Promise<VoteSubmissionDto> {
//   const form = buildMultipartForm(payload, files);

//   const res = await apiClient.put<VoteSubmissionDto>(
//     `/vote-submissions/${submissionId}`,
//     form
//   );

//   return res.data;
// }

// /** Verify (accept/reject) a submission */
// export async function verifyVoteSubmission(
//   submissionId: string,
//   body: { verifierUserId: string; accept: boolean; comment?: string | null }
// ): Promise<VoteSubmissionDto> {
//   const res = await apiClient.post<VoteSubmissionDto>(
//     `/vote-submissions/${submissionId}/verify`,
//     body
//   );
//   return res.data;
// }

// /** Delete a vote submission */
// export async function deleteVoteSubmission(
//   submissionId: string
// ): Promise<void> {
//   await apiClient.delete(`/vote-submissions/${submissionId}`);
// }

// // // src/shared/services/voteSubmissionService.ts
// // // Create wrapper: chooses JSON when no files, multipart when files present.
// // // Computes idempotency fields before send and never sets Content-Type for FormData.

// // import { apiClient } from "../lib/apiClient";
// // import { ensureIdempotencyFields } from "../lib/submissionHash";
// // import type {
// //   VoteSubmissionCreatePayload,
// //   VoteSubmissionUpdatePayload,
// //   VoteSubmissionDto,
// //   PageResponse,
// // } from "../types/api";

// // /** Helper to build multipart FormData with a JSON "payload" part and optional files */
// // function buildMultipartForm(
// //   payload: VoteSubmissionCreatePayload | VoteSubmissionUpdatePayload,
// //   files?: File[]
// // ): FormData {
// //   const formData = new FormData();
// //   const json = JSON.stringify(payload);
// //   const blob = new Blob([json], { type: "application/json" });
// //   formData.append("payload", blob);
// //   if (files && files.length > 0) {
// //     for (const f of files) {
// //       formData.append("files", f);
// //     }
// //   }
// //   return formData;
// // }

// // /** Paged search for vote submissions. */
// // export async function fetchVoteSubmissions(
// //   params: {
// //     orgId?: string;
// //     electionId?: string;
// //     centerId?: string;
// //     agentId?: string;
// //     status?: string;
// //     from?: string;
// //     to?: string;
// //     q?: string;
// //     page?: number;
// //     size?: number;
// //   } = {}
// // ): Promise<PageResponse<VoteSubmissionDto>> {
// //   const res = await apiClient.get<PageResponse<VoteSubmissionDto>>(
// //     "/vote-submissions",
// //     {
// //       params: {
// //         ...params,
// //         q: params.q && params.q.trim().length > 0 ? params.q.trim() : undefined,
// //       },
// //     }
// //   );
// //   return res.data;
// // }

// // /** Load single submission */
// // export async function fetchVoteSubmission(
// //   submissionId: string
// // ): Promise<VoteSubmissionDto> {
// //   const res = await apiClient.get<VoteSubmissionDto>(
// //     `/vote-submissions/${submissionId}`
// //   );
// //   return res.data;
// // }

// // /** Create wrapper: JSON if no files, multipart if files present */
// // export async function createVoteSubmission(
// //   payload: VoteSubmissionCreatePayload,
// //   files?: File[]
// // ): Promise<VoteSubmissionDto> {
// //   // Compute idempotency fields if missing
// //   const { clientGuid, submissionHash } = await ensureIdempotencyFields(payload);
// //   const payloadWithIds: VoteSubmissionCreatePayload = {
// //     ...payload,
// //     clientGuid,
// //     submissionHash,
// //   };

// //   if (!files || files.length === 0) {
// //     // Send JSON body to /api/vote-submissions (server must support JSON endpoint)
// //     const res = await apiClient.post<VoteSubmissionDto>(
// //       "/vote-submissions",
// //       payloadWithIds
// //     );
// //     return res.data;
// //   } else {
// //     // Send multipart when files are present
// //     const form = buildMultipartForm(payloadWithIds, files);
// //     // Do NOT set Content-Type manually — axios will set it with boundary
// //     const res = await apiClient.post<VoteSubmissionDto>(
// //       "/vote-submissions",
// //       form
// //     );
// //     return res.data;
// //   }
// // }

// // /** Update an existing vote submission (multipart). */
// // export async function updateVoteSubmission(
// //   submissionId: string,
// //   payload: VoteSubmissionUpdatePayload,
// //   files?: File[]
// // ): Promise<VoteSubmissionDto> {
// //   const formData = buildMultipartForm(payload, files);
// //   const res = await apiClient.put<VoteSubmissionDto>(
// //     `/vote-submissions/${submissionId}`,
// //     formData
// //   );
// //   return res.data;
// // }

// // /** Verify (accept/reject) a submission via POST /{id}/verify */
// // export async function verifyVoteSubmission(
// //   submissionId: string,
// //   body: { verifierUserId: string; accept: boolean; comment?: string | null }
// // ): Promise<VoteSubmissionDto> {
// //   const res = await apiClient.post<VoteSubmissionDto>(
// //     `/vote-submissions/${submissionId}/verify`,
// //     body
// //   );
// //   return res.data;
// // }

// // /** Delete submission */
// // export async function deleteVoteSubmission(
// //   submissionId: string
// // ): Promise<void> {
// //   await apiClient.delete(`/vote-submissions/${submissionId}`);
// // }
