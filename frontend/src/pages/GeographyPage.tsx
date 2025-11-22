
// src/pages/GeographyPage.tsx
// ------------------------------------------------------
// Geography & Centers view (first step: County → District).
//
// Uses:
//   useCounties()  -> /api/counties
//   useDistricts() -> /api/districts?countyId=...
// ------------------------------------------------------

import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../shared/store/authStore";
import { useCounties } from "../shared/hook/useCounties";
import { useDistricts } from "../shared/hook/useDistricts";

const GeographyPage: React.FC = () => {
  const navigate = useNavigate();
  const currentOrgId = useAuthStore((state) => state.currentOrgId);
  const clearAuth = useAuthStore((state) => state.clearAuth);

  const countiesQuery = useCounties();

  const [selectedCountyId, setSelectedCountyId] = useState<string | null>(null);

  const districtsQuery = useDistricts(selectedCountyId);

  // Auto-select first county when data is loaded
  useEffect(() => {
    if (
      countiesQuery.data &&
      countiesQuery.data.length > 0 &&
      !selectedCountyId
    ) {
      setSelectedCountyId(countiesQuery.data[0].countyId);
    }
  }, [countiesQuery.data, selectedCountyId]);

  const handleLogout = () => {
    clearAuth();
    navigate("/login");
  };

  return (
    <div className="min-h-screen flex bg-slate-100">
      {/* Sidebar: re-use same style as Dashboard for consistency */}
      <aside className="w-64 bg-slate-900 text-slate-50 flex flex-col">
        <div className="px-4 py-4 border-b border-slate-800">
          <h1 className="text-lg font-bold">Election Vote Tracker</h1>
          <p className="text-xs text-slate-400 mt-1">Geography &amp; Centers</p>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1 text-sm">
          <button
            className="w-full text-left px-3 py-2 rounded-md hover:bg-slate-800/60"
            onClick={() => navigate("/dashboard")}
          >
            ← Back to Dashboard
          </button>
        </nav>

        <div className="px-4 py-3 border-t border-slate-800 text-xs text-slate-400">
          <p>Org ID:</p>
          <p className="font-mono break-all text-slate-300 text-[11px]">
            {currentOrgId ?? "N/A"}
          </p>
        </div>
      </aside>

      {/* Main content */}
      <div className="flex-1 flex flex-col">
        {/* Top bar */}
        <header className="h-14 px-6 flex items-center justify-between bg-white border-b border-slate-200">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">
              Geography &amp; Centers
            </h2>
            <p className="text-xs text-slate-500">
              Explore counties and districts. Later this will drive center-level
              stats and allocations.
            </p>
          </div>

          <button
            onClick={handleLogout}
            className="text-xs px-3 py-1.5 rounded-md border border-slate-300 text-slate-700 hover:bg-slate-100"
          >
            Logout
          </button>
        </header>

        {/* Body */}
        <main className="flex-1 p-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* County selector */}
            <section className="lg:col-span-1 bg-white rounded-xl shadow-sm border border-slate-200 p-4">
              <h3 className="text-sm font-semibold text-slate-900 mb-2">
                Counties
              </h3>

              {countiesQuery.isLoading ? (
                <p className="text-xs text-slate-500">Loading counties…</p>
              ) : countiesQuery.isError ? (
                <p className="text-xs text-red-600">
                  Error loading counties. Check API /api/counties.
                </p>
              ) : countiesQuery.data && countiesQuery.data.length > 0 ? (
                <ul className="space-y-1">
                  {countiesQuery.data.map((county) => {
                    const isActive = county.countyId === selectedCountyId;
                    return (
                      <li key={county.countyId}>
                        <button
                          type="button"
                          onClick={() => setSelectedCountyId(county.countyId)}
                          className={`w-full text-left px-3 py-2 rounded-md text-sm ${
                            isActive
                              ? "bg-blue-600 text-white"
                              : "hover:bg-slate-100"
                          }`}
                        >
                          {county.countyName}
                        </button>
                      </li>
                    );
                  })}
                </ul>
              ) : (
                <p className="text-xs text-slate-500">No counties defined.</p>
              )}
            </section>

            {/* District list */}
            <section className="lg:col-span-2 bg-white rounded-xl shadow-sm border border-slate-200 p-4">
              <h3 className="text-sm font-semibold text-slate-900 mb-2">
                Districts
              </h3>

              {!selectedCountyId && (
                <p className="text-xs text-slate-500">
                  Select a county on the left to view districts.
                </p>
              )}

              {selectedCountyId && districtsQuery.isLoading && (
                <p className="text-xs text-slate-500">Loading districts…</p>
              )}

              {selectedCountyId && districtsQuery.isError && (
                <p className="text-xs text-red-600">
                  Error loading districts. Check API /api/districts.
                </p>
              )}

              {selectedCountyId &&
                districtsQuery.data &&
                districtsQuery.data.length === 0 && (
                  <p className="text-xs text-slate-500">
                    No districts found for this county.
                  </p>
                )}

              {selectedCountyId &&
                districtsQuery.data &&
                districtsQuery.data.length > 0 && (
                  <table className="w-full text-xs border-collapse">
                    <thead>
                      <tr className="text-left border-b border-slate-200">
                        <th className="py-2 pr-2 font-medium text-slate-600">
                          District Name
                        </th>
                        <th className="py-2 pr-2 font-medium text-slate-600">
                          County
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {districtsQuery.data.map((d) => (
                        <tr
                          key={d.districtId}
                          className="border-b border-slate-100 last:border-0"
                        >
                          <td className="py-1.5 pr-2 text-slate-800">
                            {d.districtName}
                          </td>
                          <td className="py-1.5 pr-2 text-slate-500">
                            {d.countyName}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
            </section>
          </div>
        </main>
      </div>
    </div>
  );
};

export default GeographyPage;
