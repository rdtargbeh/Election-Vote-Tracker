
// src/shared/store/authStore.ts
// ------------------------------------------------------------------
// Global auth + organization store for the Election Vote Tracker.
//
// This store will:
// - Hold the authenticated user
// - Hold the auth token (e.g., JWT)
// - Track the currently selected organization (for X-Org-Id)
// - Provide actions to log in, log out, and select an org
//
// NOTE (current phase):
// - No real backend integration yet.
// - We'll wire real login + org APIs in later steps.
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
  // Actions
  setAuth: (payload: { user: AuthUser; token: string }) => void;
  clearAuth: () => void;
  setCurrentOrg: (orgId: string | null) => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: null,
  currentOrgId: null,

  setAuth: ({ user, token }) =>
    set({
      user,
      token,
      // Optionally auto-select the default org in a later step
      // currentOrgId: user.orgMemberships[0]?.orgId ?? null,
    }),

  clearAuth: () =>
    set({
      user: null,
      token: null,
      currentOrgId: null,
    }),

  setCurrentOrg: (orgId) =>
    set({
      currentOrgId: orgId,
    }),
}));
