// src/shared/routes/RequireGuards.tsx
// ------------------------------------------------------
// Route guards for the Election Vote Tracker frontend.
//
// - RequireAuth: user must be logged in (token present)
// - RequireOrg:  user must have an organization in context
//
// In this design, org selection is invisible to the user:
// backend sends defaultOrgId at login, and the frontend
// stores it as currentOrgId in authStore.
// ------------------------------------------------------

import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuthStore } from "../store/authStore";

type GuardProps = {
  children: React.ReactNode;
};

export const RequireAuth: React.FC<GuardProps> = ({ children }) => {
  const token = useAuthStore((state) => state.token);
  const location = useLocation();

  if (!token) {
    // Not logged in -> send to /login and remember where they came from
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
};

export const RequireOrg: React.FC<GuardProps> = ({ children }) => {
  const currentOrgId = useAuthStore((state) => state.currentOrgId);

  if (!currentOrgId) {
    // This should not happen in normal flow (backend should send defaultOrgId).
    // If it does, show a clear message rather than exposing any org list.
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-100">
        <div className="bg-white p-6 rounded-xl shadow border border-slate-200 max-w-md">
          <h1 className="text-lg font-semibold text-slate-900 mb-2">
            Organization Not Configured
          </h1>
          <p className="text-sm text-slate-700">
            Your account is not associated with any active organization.
            Please contact your system administrator.
          </p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
};
