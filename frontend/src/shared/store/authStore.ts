// src/shared/store/authStore.ts
// ------------------------------------------------------------------
// Global auth + organization store for the Election Vote Tracker.
// Enhanced: add currentElectionId + persistence to sessionStorage so
// the dashboard's selected election is remembered across reloads.
//
// Place this file at: src/shared/store/authStore.ts
// ------------------------------------------------------------------

// src/shared/store/authStore.ts
import { create } from "zustand";

export type RoleName =
  | "SYSTEM_ADMIN"
  | "NEC_ADMIN"
  | "ADMIN"
  | "PARTY_ADMIN"
  | "AGENT"
  | "OBSERVER"
  | "SUPERVISOR"
  | "COORDINATOR"
  | "DATA_ENTRY"
  | "AUDITOR";

export type OrgMembership = {
  orgId: string;
  orgName: string;
  roleName: RoleName; // tenant role
  isEnabled: boolean;
};

export type AuthUser = {
  userId: string;
  firstName: string;
  lastName: string;
  position: string;
  email: string;
  userName: string;

  // platform scope
  isSystemAdmin: boolean;
  globalRoleName: RoleName | null;

  // tenant scope
  orgMemberships: OrgMembership[];
};

type AuthState = {
  user: AuthUser | null;
  token: string | null;

  // tenant context (may be null for system admin in platform mode)
  currentOrgId: string | null;

  // election context (tenant feature)
  currentElectionId: string | null;

  // Actions
  setAuth: (payload: { user: AuthUser; token: string }) => void;
  setToken: (token: string | null) => void;
  clearAuth: () => void;
  setCurrentOrg: (orgId: string | null) => void;
  setCurrentElection: (electionId: string | null) => void;

  // Helpers
  isAuthenticated: () => boolean;
  isSystemAdmin: () => boolean;
  hasOrgContext: () => boolean;
  getEnabledOrgs: () => OrgMembership[];
};

const ELECTION_KEY = "evt.currentElectionId";
const ORG_KEY = "evt.currentOrgId";
const TOKEN_KEY = "evt.token";

function loadPersisted(): Partial<AuthState> {
  try {
    return {
      currentElectionId: sessionStorage.getItem(ELECTION_KEY) || null,
      currentOrgId: sessionStorage.getItem(ORG_KEY) || null,
      token: sessionStorage.getItem(TOKEN_KEY) || null,
    };
  } catch {
    return {};
  }
}

function persist(key: string, value: string | null) {
  try {
    if (value) sessionStorage.setItem(key, value);
    else sessionStorage.removeItem(key);
  } catch {}
}

export const useAuthStore = create<AuthState>((set, get) => {
  const persisted = loadPersisted();

  return {
    user: null,
    token: (persisted.token as string) ?? null,
    currentOrgId: (persisted.currentOrgId as string) ?? null,
    currentElectionId: (persisted.currentElectionId as string) ?? null,

    setAuth: ({ user, token }) =>
      set(() => {
        persist(TOKEN_KEY, token);

        // ✅ Real-world behavior:
        // - If user is NOT system admin, auto-pick a default enabled org if currentOrgId missing.
        // - If user is system admin, allow staying in platform mode (currentOrgId can remain null).
        let nextOrgId = get().currentOrgId;

        const enabledOrgs = (user.orgMemberships || []).filter(
          (m) => m.isEnabled
        );

        if (!user.isSystemAdmin) {
          if (!nextOrgId || !enabledOrgs.some((m) => m.orgId === nextOrgId)) {
            nextOrgId = enabledOrgs[0]?.orgId ?? null;
            persist(ORG_KEY, nextOrgId);
          }
        } else {
          // SYSTEM_ADMIN: keep org context only if already set; don't force it
          // but if it's set and not in their memberships, that's still ok (admin can act across tenants)
          persist(ORG_KEY, nextOrgId);
        }

        return { user, token, currentOrgId: nextOrgId };
      }),

    setToken: (token) =>
      set(() => {
        persist(TOKEN_KEY, token);
        return { token };
      }),

    clearAuth: () =>
      set(() => {
        persist(ELECTION_KEY, null);
        persist(ORG_KEY, null);
        persist(TOKEN_KEY, null);
        return {
          user: null,
          token: null,
          currentOrgId: null,
          currentElectionId: null,
        };
      }),

    setCurrentOrg: (orgId) =>
      set(() => {
        persist(ORG_KEY, orgId);
        return { currentOrgId: orgId };
      }),

    setCurrentElection: (electionId) =>
      set(() => {
        persist(ELECTION_KEY, electionId);
        return { currentElectionId: electionId };
      }),

    // Helpers
    isAuthenticated: () => !!get().token,
    isSystemAdmin: () =>
      !!get().user?.isSystemAdmin ||
      get().user?.globalRoleName === "SYSTEM_ADMIN",
    hasOrgContext: () => !!get().currentOrgId,
    getEnabledOrgs: () =>
      (get().user?.orgMemberships || []).filter((m) => m.isEnabled),
  };
});
