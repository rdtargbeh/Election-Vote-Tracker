// src/pages/SelectOrgPage.tsx
// ------------------------------------------------------------------
// Organization selection screen.
//
// Now wired to:
// - save selected organization in Zustand store
// - redirect user to the dashboard placeholder page
//
// Backend integration will be added later.
// ------------------------------------------------------------------

import React from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../shared/store/authStore";

const mockOrgs = [
  { id: "1", name: "Coalition for Democratic Change (CDC)" },
  { id: "2", name: "Unity Party (UP)" },
  { id: "3", name: "National Elections Commission (NEC)" },
];

const SelectOrgPage: React.FC = () => {
  const navigate = useNavigate();
  const setCurrentOrg = useAuthStore((state) => state.setCurrentOrg);

  const handleSelect = (orgId: string) => {
    setCurrentOrg(orgId);
    navigate("/dashboard"); // temporary route (we will create this next)
  };

  return (
    <div className="min-h-screen bg-slate-100 flex items-center justify-center">
      <div className="w-full max-w-lg bg-white rounded-xl shadow-lg border border-slate-200 p-6">
        <h1 className="text-xl font-bold text-slate-900 mb-4 text-center">
          Select Your Organization
        </h1>

        <p className="text-sm text-slate-600 mb-6 text-center">
          Choose the organization you will act on behalf of.
        </p>

        <div className="space-y-3">
          {mockOrgs.map((org) => (
            <button
              key={org.id}
              onClick={() => handleSelect(org.id)}
              className="w-full bg-blue-600 text-white py-2 rounded-md font-semibold hover:bg-blue-700 transition"
            >
              {org.name}
            </button>
          ))}
        </div>

        <p className="text-xs text-slate-400 mt-6 text-center">
          This data is currently mocked. Real backend data will be integrated
          later.
        </p>
      </div>
    </div>
  );
};

export default SelectOrgPage;
