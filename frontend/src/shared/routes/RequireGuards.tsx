// src/shared/routes/RequireGuards.tsx
// ------------------------------------------------------
// Route guards for the Election Vote Tracker frontend.
//
// - RequireAuth: user must be logged in (token present)
// - RequireOrg:  user must have an organization in context
//
// Multi-tenant nuance:
// - Most users require orgId (tenant context)
// - SYSTEM_ADMIN is a platform user and may NOT have orgId
//   -> RequireOrg must allow SYSTEM_ADMIN to pass without orgId.
// ------------------------------------------------------

import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuthStore } from "../store/authStore";

type GuardProps = { children: React.ReactNode };

function decodeIsSystemAdmin(token: string | null): boolean {
  try {
    if (!token) return false;
    const parts = token.split(".");
    if (parts.length < 2) return false;

    const b64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(b64)
        .split("")
        .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
        .join("")
    );

    const payload = JSON.parse(json) as any;

    const role =
      payload.globalRoleName ??
      payload.roleName ??
      payload.role ??
      payload.authority ??
      payload.authorities?.[0];

    const isSysAdminFlag =
      payload.isSystemAdmin ?? payload.is_system_admin ?? payload.sysAdmin;

    return (
      isSysAdminFlag === true ||
      role === "SYSTEM_ADMIN" ||
      payload?.roles?.includes?.("SYSTEM_ADMIN")
    );
  } catch {
    return false;
  }
}

export const RequireAuth: React.FC<GuardProps> = ({ children }) => {
  const token = useAuthStore((s) => s.token);
  const location = useLocation();

  if (!token)
    return <Navigate to="/login" state={{ from: location }} replace />;
  return <>{children}</>;
};

export const RequireOrg: React.FC<GuardProps> = ({ children }) => {
  const currentOrgId = useAuthStore((s) => s.currentOrgId);
  const user = useAuthStore((s) => s.user);
  const token = useAuthStore((s) => s.token);

  const isSystemAdmin =
    !!user?.isSystemAdmin ||
    user?.globalRoleName === "SYSTEM_ADMIN" ||
    decodeIsSystemAdmin(token);

  if (isSystemAdmin) return <>{children}</>;

  if (!currentOrgId) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-100">
        <div className="bg-white p-6 rounded-xl shadow border border-slate-200 max-w-md">
          <h1 className="text-lg font-semibold text-slate-900 mb-2">
            Organization Not Configured
          </h1>
          <p className="text-sm text-slate-700">
            Your account is not associated with any active organization. Please
            contact your system administrator.
          </p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
};

/**
 * ✅ NEW: RequireSystemAdmin
 * - Use for platform-only pages like Organization Management.
 * - SYSTEM_ADMIN can pass even without orgId.
 * - Everyone else is redirected away.
 */
export const RequireSystemAdmin: React.FC<GuardProps> = ({ children }) => {
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);
  const location = useLocation();

  // must be logged in
  if (!token)
    return <Navigate to="/login" state={{ from: location }} replace />;

  const isSystemAdmin =
    !!user?.isSystemAdmin ||
    user?.globalRoleName === "SYSTEM_ADMIN" ||
    decodeIsSystemAdmin(token);

  if (!isSystemAdmin) {
    // send non-system users back to dashboard (adjust if your home route differs)
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
};

// // src/shared/routes/RequireGuards.tsx
// // ------------------------------------------------------
// // Route guards for the Election Vote Tracker frontend.
// //
// // - RequireAuth: user must be logged in (token present)
// // - RequireOrg:  user must have an organization in context
// //
// // Multi-tenant nuance:
// // - Most users require orgId (tenant context)
// // - SYSTEM_ADMIN is a platform user and may NOT have orgId
// //   -> RequireOrg must allow SYSTEM_ADMIN to pass without orgId.
// // ------------------------------------------------------

// // src/shared/routes/RequireGuards.tsx
// import React from "react";
// import { Navigate, useLocation } from "react-router-dom";
// import { useAuthStore } from "../store/authStore";

// type GuardProps = { children: React.ReactNode };

// function decodeIsSystemAdmin(token: string | null): boolean {
//   try {
//     if (!token) return false;
//     const parts = token.split(".");
//     if (parts.length < 2) return false;

//     const b64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
//     const json = decodeURIComponent(
//       atob(b64)
//         .split("")
//         .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
//         .join("")
//     );

//     const payload = JSON.parse(json) as any;

//     const role =
//       payload.globalRoleName ??
//       payload.roleName ??
//       payload.role ??
//       payload.authority ??
//       payload.authorities?.[0];

//     const isSysAdminFlag =
//       payload.isSystemAdmin ?? payload.is_system_admin ?? payload.sysAdmin;

//     return (
//       isSysAdminFlag === true ||
//       role === "SYSTEM_ADMIN" ||
//       payload?.roles?.includes?.("SYSTEM_ADMIN")
//     );
//   } catch {
//     return false;
//   }
// }

// export const RequireAuth: React.FC<GuardProps> = ({ children }) => {
//   const token = useAuthStore((s) => s.token);
//   const location = useLocation();

//   if (!token)
//     return <Navigate to="/login" state={{ from: location }} replace />;
//   return <>{children}</>;
// };

// export const RequireOrg: React.FC<GuardProps> = ({ children }) => {
//   const currentOrgId = useAuthStore((s) => s.currentOrgId);
//   const user = useAuthStore((s) => s.user);
//   const token = useAuthStore((s) => s.token);

//   const isSystemAdmin =
//     !!user?.isSystemAdmin ||
//     user?.globalRoleName === "SYSTEM_ADMIN" ||
//     decodeIsSystemAdmin(token);

//   if (isSystemAdmin) return <>{children}</>;

//   if (!currentOrgId) {
//     return (
//       <div className="min-h-screen flex items-center justify-center bg-slate-100">
//         <div className="bg-white p-6 rounded-xl shadow border border-slate-200 max-w-md">
//           <h1 className="text-lg font-semibold text-slate-900 mb-2">
//             Organization Not Configured
//           </h1>
//           <p className="text-sm text-slate-700">
//             Your account is not associated with any active organization. Please
//             contact your system administrator.
//           </p>
//         </div>
//       </div>
//     );
//   }

//   return <>{children}</>;
// };
