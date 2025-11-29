
// src/shared/services/statsService.ts
// ------------------------------------------------------
// Fetches election-level stats for the current organization.
// Backed by backend views:
//   v_election_stats_party
// ------------------------------------------------------

import { apiClient } from "../lib/apiClient";

export type ElectionStats = {
  electionId: string;
  registeredVoters: number;
  ballotsCast: number;
  validVotes: number;
  invalidTotal: number;
  turnoutPct: number;
  invalidPct: number;
};

export async function fetchElectionStats(electionId: string) {
  const res = await apiClient.get<ElectionStats>(
    `/stats/election-summary?electionId=${electionId}`
  );
  return res.data;
}
