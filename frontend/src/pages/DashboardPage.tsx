
// src/pages/DashboardPage.tsx
// ------------------------------------------------------------------
// Temporary placeholder dashboard.
// This will later be replaced with real role-based dashboards.
// ------------------------------------------------------------------

import React from "react";
import { useAuthStore } from "../shared/store/authStore";

const DashboardPage: React.FC = () => {
  const currentOrgId = useAuthStore((state) => state.currentOrgId);

  return (
    <div className="min-h-screen bg-slate-100 flex items-center justify-center">
      <div className="bg-white p-8 rounded-xl shadow-md w-full max-w-xl border border-slate-200">
        <h1 className="text-2xl font-bold mb-4 text-slate-900 text-center">
          Dashboard Placeholder
        </h1>

        <p className="text-slate-700 text-center">
          Selected Organization ID:
        </p>

        <p className="text-blue-600 font-semibold text-center mt-2">
          {currentOrgId ?? "None selected"}
        </p>

        <p className="text-xs text-slate-400 mt-6 text-center">
          Real dashboards will be added based on roles.
        </p>
      </div>
    </div>
  );
};

export default DashboardPage;
