# API: Candidate election-level Party Stats

Endpoint
- GET /api/stats/party/candidates/elections

Description
- Returns paginated candidate-level aggregated statistics for an election from the view `v_candidate_election_stats_party`.
- Data is sourced from `vote_tally` joined with candidate/party and `v_election_stats_party`.
- Enforces tenant scoping (org) and uses DB RLS policies; server sets transaction-local GUCs before repository queries.

Query Parameters
- orgId (UUID) — optional if authenticated principal includes an organization; otherwise required.
- electionId (UUID) — required.
- candidateId (UUID) — optional.
- partyId (UUID) — optional.
- Pagination: page (>=0), size (1..500), sort (Spring Data style).

Response
- 200 OK: Page<CandidateElectionStatsPartyDto>
- 400 Bad Request: missing/invalid params
- 403 Forbidden: orgId mismatch
- 404 Not Found: electionId not found

CandidateElectionStatsPartyDto fields
- orgId, electionId, candidateId (UUIDs)
- candidateName, partyId, partyName, partyCode
- candidateVotes (integer)
- registeredVoters, ballotsCast, validVotes, invalidTotal (integers)
- voteSharePct (decimal in [0..1]; NULL if validVotes = 0)