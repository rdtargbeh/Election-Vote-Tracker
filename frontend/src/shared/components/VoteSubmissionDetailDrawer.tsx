// src/shared/components/VoteSubmissionDetailDrawer.tsx
// ------------------------------------------------------
// Submission detail panel for a single vote submission.
// Renders as a card under the table, with larger candidate text.
// ------------------------------------------------------

import React, { useMemo } from "react";
import { useVoteSubmission } from "../hooks/useVoteSubmission";
import { useElectionCandidates } from "../hooks/useElectionCandidates";
import type { VoteSubmissionDto } from "../types/api";
// import type { VoteSubmissionDto } from "../services/voteSubmissionService";

interface Props {
  submissionId: string | null;
  onClose: () => void;
}

export const VoteSubmissionDetailDrawer: React.FC<Props> = ({
  submissionId,
  onClose,
}) => {
  const isOpen = !!submissionId;

  if (!isOpen) return null;

  const query = useVoteSubmission(submissionId);
  const submission = query.data as VoteSubmissionDto | undefined;

  const electionIdForCandidates = submission?.electionId ?? null;
  const {
    data: candidates,
    isLoading: candidatesLoading,
    isError: candidatesError,
  } = useElectionCandidates(electionIdForCandidates);

  const candidateLookup = useMemo(() => {
    const map: Record<
      string,
      { name: string; partyAbbrev?: string | undefined }
    > = {};
    if (candidates) {
      for (const c of candidates) {
        map[c.candidateId] = {
          name: c.fullName,
          partyAbbrev: undefined,
        };
      }
    }
    return map;
  }, [candidates]);

  return (
    <section className="mt-4 bg-white rounded-xl shadow-sm border border-slate-200">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-slate-200">
        <div>
          <h3 className="text-sm font-semibold text-slate-900">
            Submission Details
          </h3>
          {submission && (
            <p className="text-[11px] text-slate-500 font-mono">
              {submission.submissionId?.slice(0, 12)}…
            </p>
          )}
        </div>
        <button
          onClick={onClose}
          className="text-xs px-2 py-1 rounded-md border border-slate-300 text-slate-600 hover:bg-slate-100"
        >
          Close
        </button>
      </div>

      {/* Body */}
      <div className="max-h-[420px] overflow-y-auto p-4 space-y-4 text-xs text-slate-700">
        {query.isLoading && (
          <div className="text-slate-500">Loading submission…</div>
        )}

        {query.isError && (
          <div className="text-red-600">Error loading submission details.</div>
        )}

        {submission && (
          <>
            {/* Context */}
            <section className="space-y-1">
              <h4 className="text-[11px] font-semibold text-slate-500 uppercase">
                Context
              </h4>
              <div className="bg-slate-50 rounded-lg border border-slate-200 p-3 space-y-1">
                <p>
                  <span className="font-semibold">Election:</span>{" "}
                  {submission.electionName}{" "}
                  {submission.year ? `(${submission.year})` : ""}
                </p>
                <p>
                  <span className="font-semibold">Center:</span>{" "}
                  {submission.centerName || submission.centerCode || "—"}
                </p>
                <p>
                  <span className="font-semibold">Place:</span>{" "}
                  {submission.placeLabel ||
                    submission.placeCode ||
                    (submission.placeNumber != null
                      ? `Place ${submission.placeNumber}`
                      : "—")}
                </p>
                <p>
                  <span className="font-semibold">Agent:</span>{" "}
                  {submission.agentName || "—"}
                </p>
              </div>
            </section>

            {/* Ballots Summary */}
            <section className="space-y-1">
              <h4 className="text-[11px] font-semibold text-slate-500 uppercase">
                Ballots Summary
              </h4>
              <div className="grid grid-cols-2 gap-2">
                <StatBox
                  label="Ballots Cast"
                  value={submission.ballotsCast ?? 0}
                />
                <StatBox
                  label="Valid Votes"
                  value={submission.validVotes ?? 0}
                />
                <StatBox
                  label="Invalid Total"
                  value={submission.invalidTotal ?? 0}
                  helper="invalid + blank + rejected + spoiled"
                />
                <StatBox
                  label="Turnout %"
                  value={
                    submission.turnoutPct != null
                      ? submission.turnoutPct.toFixed(2)
                      : "—"
                  }
                />
                <StatBox
                  label="Invalid %"
                  value={
                    submission.invalidPct != null
                      ? submission.invalidPct.toFixed(2)
                      : "—"
                  }
                />
                <StatBox label="Status" value={submission.status} />
              </div>
            </section>

            {/* Candidate Votes – bigger fonts */}
            <section className="space-y-1">
              <h4 className="text-[11px] font-semibold text-slate-500 uppercase">
                Candidate Votes
              </h4>

              {candidatesLoading && (
                <p className="text-slate-500 text-xs">Loading candidates…</p>
              )}
              {candidatesError && (
                <p className="text-red-600 text-[11px]">
                  Could not load candidate details; showing IDs only.
                </p>
              )}

              {submission.candidateVotes &&
              Object.keys(submission.candidateVotes).length > 0 ? (
                <div className="border border-slate-200 rounded-lg overflow-hidden">
                  <table className="w-full text-sm">
                    <thead className="bg-slate-50 border-b border-slate-200">
                      <tr>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">
                          Candidate
                        </th>
                        <th className="px-3 py-2 text-right font-semibold text-slate-700">
                          Votes
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {Object.entries(submission.candidateVotes).map(
                        ([candidateId, votes]) => {
                          const meta = candidateLookup[candidateId];
                          const label = meta ? meta.name : candidateId;

                          return (
                            <tr
                              key={candidateId}
                              className="border-b border-slate-100 last:border-b-0"
                            >
                              <td className="px-3 py-2">
                                <div className="text-[13px] font-medium text-slate-900">
                                  {label}
                                </div>
                                {!meta && (
                                  <div className="text-[11px] text-slate-400 font-mono">
                                    {candidateId.slice(0, 8)}…
                                  </div>
                                )}
                              </td>
                              <td className="px-3 py-2 text-right text-sm font-semibold text-slate-900">
                                {votes}
                              </td>
                            </tr>
                          );
                        }
                      )}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="text-slate-500 text-xs">
                  No candidate breakdown available.
                </p>
              )}
            </section>

            {/* Metadata */}
            <section className="space-y-1">
              <h4 className="text-[11px] font-semibold text-slate-500 uppercase">
                Metadata
              </h4>
              <div className="bg-slate-50 rounded-lg border border-slate-200 p-3 space-y-1">
                <p>
                  <span className="font-semibold">Verified by:</span>{" "}
                  {submission.verifiedByName || "—"}
                </p>
                <p>
                  <span className="font-semibold">Date verified:</span>{" "}
                  {submission.dateVerified
                    ? new Date(submission.dateVerified).toLocaleString()
                    : "—"}
                </p>
                <p>
                  <span className="font-semibold">Client IP:</span>{" "}
                  {submission.clientIp || "—"}
                </p>
                <p>
                  <span className="font-semibold">User agent:</span>{" "}
                  {submission.userAgent || "—"}
                </p>
                <p>
                  <span className="font-semibold">GPS:</span>{" "}
                  {submission.latitude != null && submission.longitude != null
                    ? `${submission.latitude.toFixed(
                        5
                      )}, ${submission.longitude.toFixed(5)}`
                    : "—"}
                </p>
                <p>
                  <span className="font-semibold">Comments:</span>{" "}
                  {submission.comments || "—"}
                </p>
              </div>
            </section>
          </>
        )}
      </div>
    </section>
  );
};

const StatBox: React.FC<{
  label: string;
  value: React.ReactNode;
  helper?: string;
}> = ({ label, value, helper }) => (
  <div className="bg-slate-50 rounded-lg border border-slate-200 p-2">
    <div className="text-[10px] font-semibold text-slate-500 uppercase">
      {label}
    </div>
    <div className="mt-0.5 text-sm font-semibold text-slate-900">{value}</div>
    {helper && (
      <div className="mt-0.5 text-[10px] text-slate-500">{helper}</div>
    )}
  </div>
);
