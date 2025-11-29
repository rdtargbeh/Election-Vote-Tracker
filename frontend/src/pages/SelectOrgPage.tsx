// src/pages/SelectOrgPage.tsx
// ------------------------------------------------------------------
// Organization selection screen.
//
// Now wired to real backend data:
// - Calls useMyOrgMemberships() to load organizations
// - Shows loading + error states
// - On click, saves orgId into authStore and goes to /dashboard
// ------------------------------------------------------------------

import React from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../shared/store/authStore";
import { useMyOrgMemberships } from "../shared/api/useMyOrgMemberships";

const SelectOrgPage: React.FC = () => {
  const navigate = useNavigate();
  const setCurrentOrg = useAuthStore((state) => state.setCurrentOrg);

  const { data, isLoading, error } = useMyOrgMemberships();

  const handleSelect = (orgId: string) => {
    setCurrentOrg(orgId);
    navigate("/dashboard");
  };

  let content: React.ReactNode;

  if (isLoading) {
    content = (
      <p className="text-sm text-slate-600 text-center">
        Loading your organizations...
      </p>
    );
  } else if (error) {
    content = (
      <p className="text-sm text-red-600 text-center">
        Failed to load organizations. Please check your connection or backend.
      </p>
    );
  } else if (!data || data.length === 0) {
    content = (
      <p className="text-sm text-slate-600 text-center">
        No organizations found for your account.
      </p>
    );
  } else {
    content = (
      <div className="space-y-3">
        {data.map((org) => (
          <button
            key={org.orgId}
            disabled={!org.isEnabled}
            onClick={() => handleSelect(org.orgId)}
            className="w-full bg-blue-600 text-white py-2 rounded-md font-semibold hover:bg-blue-700 disabled:opacity-50 transition text-sm"
          >
            <div className="flex flex-col items-start">
              <span>{org.orgName}</span>
              <span className="text-xs text-blue-100">
                Role: {org.roleName}
                {!org.isEnabled ? " (disabled)" : ""}
              </span>
            </div>
          </button>
        ))}
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-100 flex items-center justify-center">
      <div className="w-full max-w-lg bg-white rounded-xl shadow-lg border border-slate-200 p-6">
        <h1 className="text-xl font-bold text-slate-900 mb-4 text-center">
          Select Your Organization
        </h1>

        <p className="text-sm text-slate-600 mb-6 text-center">
          Choose the organization you will act on behalf of.
        </p>

        {content}

        <p className="text-xs text-slate-400 mt-6 text-center">
          Data is loaded from the backend for the currently logged-in user.
        </p>
      </div>
    </div>
  );
};

export default SelectOrgPage;
