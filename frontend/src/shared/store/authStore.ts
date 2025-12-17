// src/shared/store/authStore.ts
// ------------------------------------------------------------------
// Global auth + organization store for the Election Vote Tracker.
// Enhanced: add currentElectionId + persistence to sessionStorage so
// the dashboard's selected election is remembered across reloads.
//
// Place this file at: src/shared/store/authStore.ts
// ------------------------------------------------------------------

import { create } from "zustand";

// Types can be expanded later as we align with backend DTOs.
export type OrgMembership = {
  orgId: string;
  orgName: string;
  roleName: string; // ADMIN, PARTY_ADMIN, AGENT, etc.
  isEnabled: boolean;
};

export type AuthUser = {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  userName: string;
  isSystemAdmin: boolean;
  globalRoleName: string | null; // from user_role table (optional)
  orgMemberships: OrgMembership[];
};

type AuthState = {
  user: AuthUser | null;
  token: string | null;
  currentOrgId: string | null;
  currentElectionId: string | null;

  // Actions
  setAuth: (payload: { user: AuthUser; token: string }) => void;
  setToken: (token: string | null) => void;
  clearAuth: () => void;
  setCurrentOrg: (orgId: string | null) => void;
  setCurrentElection: (electionId: string | null) => void;
};

const ELECTION_KEY = "evt.currentElectionId";
const ORG_KEY = "evt.currentOrgId";
const TOKEN_KEY = "evt.token";

/**
 * Try to hydrate persisted small state from sessionStorage.
 * We only persist electionId and orgId + token (optional) to survive reloads.
 */
function loadPersisted(): Partial<AuthState> {
  try {
    const currentElectionId = sessionStorage.getItem(ELECTION_KEY);
    const currentOrgId = sessionStorage.getItem(ORG_KEY);
    const token = sessionStorage.getItem(TOKEN_KEY);
    return {
      currentElectionId: currentElectionId ? currentElectionId : null,
      currentOrgId: currentOrgId ? currentOrgId : null,
      token: token ? token : null,
    };
  } catch (e) {
    // sessionStorage may be unavailable in some environments; fail silently
    return {};
  }
}

export const useAuthStore = create<AuthState>((set) => {
  const persisted = loadPersisted();

  return {
    user: null,
    token: (persisted.token as string) ?? null,
    currentOrgId: (persisted.currentOrgId as string) ?? null,
    currentElectionId: (persisted.currentElectionId as string) ?? null,

    // For future flows where backend returns both user + token
    setAuth: ({ user, token }) =>
      set(() => {
        try {
          if (token) sessionStorage.setItem(TOKEN_KEY, token);
        } catch (_) {}
        return {
          user,
          token,
        };
      }),

    // For simple flows where we only have a token (current case)
    setToken: (token) =>
      set(() => {
        try {
          if (token) {
            sessionStorage.setItem(TOKEN_KEY, token);
          } else {
            sessionStorage.removeItem(TOKEN_KEY);
          }
        } catch (_) {}
        return {
          token,
        };
      }),

    clearAuth: () =>
      set(() => {
        try {
          sessionStorage.removeItem(ELECTION_KEY);
          sessionStorage.removeItem(ORG_KEY);
          sessionStorage.removeItem(TOKEN_KEY);
        } catch (_) {}
        return {
          user: null,
          token: null,
          currentOrgId: null,
          currentElectionId: null,
        };
      }),

    setCurrentOrg: (orgId) =>
      set(() => {
        try {
          if (orgId) sessionStorage.setItem(ORG_KEY, orgId);
          else sessionStorage.removeItem(ORG_KEY);
        } catch (_) {}
        return {
          currentOrgId: orgId,
        };
      }),

    setCurrentElection: (electionId) =>
      set(() => {
        try {
          if (electionId) sessionStorage.setItem(ELECTION_KEY, electionId);
          else sessionStorage.removeItem(ELECTION_KEY);
        } catch (_) {}
        return {
          currentElectionId: electionId,
        };
      }),
  };
});
