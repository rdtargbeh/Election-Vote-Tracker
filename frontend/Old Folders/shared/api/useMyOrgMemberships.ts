
// src/shared/api/useMyOrgMemberships.ts
// ------------------------------------------------------------------
// For now, we don't have a dedicated "my org memberships" endpoint,
// so we:
// - Call GET /api/orgs (via /orgs from apiClient baseURL /api)
// - Filter / request only active orgs
// - Adapt OrganizationDto -> OrgMembershipDto-like shape expected
//   by SelectOrgPage.
// ------------------------------------------------------------------


import { useQuery } from "@tanstack/react-query";
import { apiClient } from "../lib/apiClient";

export type OrgMembershipDto = {
  orgId: string;
  orgName: string;
  roleName: string;
  isEnabled: boolean;
};

type OrganizationDto = {
  orgId: string;
  orgName: string;
  orgType: "POLITICAL_PARTY" | "COALITION" | "NGO" | "MEDIA" | "NEC" | "OTHER";
  isActive?: boolean; // mark optional so TS knows it might be missing
};

type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

export function useMyOrgMemberships() {
  return useQuery({
    queryKey: ["my-org-memberships"],
    queryFn: async () => {
      const res = await apiClient.get<Page<OrganizationDto>>("/orgs", {
        params: {
          active: true,
        },
      });

      return res.data.content.map<OrgMembershipDto>((org) => ({
        orgId: org.orgId,
        orgName: org.orgName,
        roleName: org.orgType,
        isEnabled: org.isActive ?? true,
      }));
    },
  });
}
