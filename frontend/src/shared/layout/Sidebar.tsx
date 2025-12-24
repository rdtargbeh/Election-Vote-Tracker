// src/shared/layout/Sidebar.tsx
import React from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuthStore } from "../store/authStore";

interface NavItem {
  label: string;
  path: string;
}

const mainItems: NavItem[] = [
  { label: "My Profile", path: "/profile" },
  { label: "User Management", path: "/users" },
  { label: "Dashboard", path: "/dashboard" },
  { label: "Geography & Centers", path: "/geography" },
  { label: "Vote Submissions", path: "/vote-submissions" },
  { label: "Observer Reports", path: "/observer-reports" },
  { label: "Results & Analytics", path: "/results" },
];

const adminItems: NavItem[] = [
  { label: "Users & Roles", path: "/admin/users" },
  { label: "Organizations", path: "/admin/organizations" },
  { label: "Elections", path: "/admin/elections" },
  { label: "Allocations", path: "/admin/allocations" },
];

const Sidebar: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const currentOrgId = useAuthStore((s) => s.currentOrgId);
  // later we can use role from store to hide admin items

  const isActive = (path: string) =>
    location.pathname === path || location.pathname.startsWith(path + "/");

  return (
    <aside className="w-64 bg-slate-900 text-slate-50 flex flex-col">
      <div className="px-4 py-4 border-b border-slate-800">
        <h1 className="text-lg font-bold">Election Vote Tracker</h1>
        <p className="text-xs text-slate-400 mt-1">
          Multi-tenant, real-time vote tracking
        </p>
      </div>

      <nav
        className="flex-1 px-3 py-4 space-y-1 text-sm"
        aria-label="Main navigation"
      >
        <p className="px-2 text-xs font-semibold text-slate-500 uppercase mb-2">
          Main
        </p>

        {mainItems.map((item) => (
          <button
            key={item.path}
            onClick={() => navigate(item.path)}
            className={`w-full text-left px-3 py-2 rounded-md ${
              isActive(item.path)
                ? "bg-slate-800 text-slate-100"
                : "hover:bg-slate-800/60"
            }`}
          >
            {item.label}
          </button>
        ))}

        <p className="px-2 text-xs font-semibold text-slate-500 uppercase mt-4 mb-2">
          Admin
        </p>

        {adminItems.map((item) => (
          <button
            key={item.path}
            onClick={() => navigate(item.path)}
            className={`w-full text-left px-3 py-2 rounded-md ${
              isActive(item.path)
                ? "bg-slate-800 text-slate-100"
                : "hover:bg-slate-800/60"
            }`}
          >
            {item.label}
          </button>
        ))}
      </nav>

      <div className="px-4 py-3 border-t border-slate-800 text-xs text-slate-400">
        <p>Org ID:</p>
        <p className="font-mono break-all text-slate-300 text-[11px]">
          {currentOrgId ?? "N/A"}
        </p>
      </div>
    </aside>
  );
};

export default Sidebar;
