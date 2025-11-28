// src/shared/layout/TopBar.tsx
import React from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuthStore } from "../store/authStore";

const routeTitleMap: Record<string, string> = {
  "/dashboard": "Dashboard",
  "/geography": "Geography & Centers",
  "/vote-submissions": "Vote Submissions",
  "/observer-reports": "Observer Reports",
  "/results": "Results & Analytics",
  "/admin": "Administration",
  "/admin/users": "Users & Roles",
  "/admin/organizations": "Organizations",
  "/admin/elections": "Elections",
  "/admin/allocations": "Allocations",
};

const TopBar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const clearAuth = useAuthStore((s) => s.clearAuth);

  const basePath = "/" + location.pathname.split("/")[1];
  const title = routeTitleMap[basePath] ?? "Dashboard";

  const handleLogout = () => {
    clearAuth();
    navigate("/login");
  };

  return (
    <header className="h-14 px-6 flex items-center justify-between bg-white border-b border-slate-200">
      <div>
        <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
        <p className="text-xs text-slate-500">Tenant-scoped view</p>
      </div>

      <div className="flex items-center gap-4">
        <div className="text-right">
          <p className="text-sm font-medium text-slate-800">Logged in</p>
          <p className="text-xs text-slate-500">Authenticated user</p>
        </div>

        <button
          onClick={handleLogout}
          className="text-xs px-3 py-1.5 rounded-md border border-slate-300 text-slate-700 hover:bg-slate-100"
        >
          Logout
        </button>
      </div>
    </header>
  );
};

export default TopBar;
