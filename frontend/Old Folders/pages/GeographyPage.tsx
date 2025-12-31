// src/pages/GeographyPage.tsx
// ------------------------------------------------------
// Geography & Centers view:
//   County → District → Polling Centers → Polling Places
// ------------------------------------------------------

import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { useAuthStore } from "../../shared/lib/store/authStore";
import { useCounties } from "../shared/hooks/useCounties";
import { useDistricts } from "../shared/hooks/useDistricts";
import { usePollingCenters } from "../shared/hooks/usePollingCenters";
import { usePollingPlaces } from "../shared/hooks/usePollingPlaces";

const GeographyPage: React.FC = () => {
  const navigate = useNavigate();
  const currentOrgId = useAuthStore((s) => s.currentOrgId);
  const clearAuth = useAuthStore((s) => s.clearAuth);

  // ───────────────────────────────────────────────
  // County → District → Center → Places (state)
  // ───────────────────────────────────────────────
  const countiesQuery = useCounties();
  const [selectedCountyId, setSelectedCountyId] = useState<string | null>(null);

  const districtsQuery = useDistricts(selectedCountyId);
  const [selectedDistrictId, setSelectedDistrictId] = useState<string | null>(
    null
  );

  const centersQuery = usePollingCenters(selectedDistrictId);
  const [selectedCenterId, setSelectedCenterId] = useState<string | null>(null);

  const placesQuery = usePollingPlaces(selectedCenterId);

  // ───────────────────────────────────────────────
  // Auto-select the first available items
  // ───────────────────────────────────────────────

  // Counties
  useEffect(() => {
    if (
      countiesQuery.data &&
      countiesQuery.data.length > 0 &&
      !selectedCountyId
    ) {
      setSelectedCountyId(countiesQuery.data[0].countyId);
    }
  }, [countiesQuery.data, selectedCountyId]);

  // Districts
  useEffect(() => {
    if (districtsQuery.data && districtsQuery.data.length > 0) {
      const exists = districtsQuery.data.some(
        (d) => d.districtId === selectedDistrictId
      );
      if (!exists) {
        setSelectedDistrictId(districtsQuery.data[0].districtId);
      }
    } else {
      setSelectedDistrictId(null);
    }
  }, [districtsQuery.data, selectedDistrictId]);

  // Centers
  useEffect(() => {
    if (centersQuery.data && centersQuery.data.length > 0) {
      const exists = centersQuery.data.some(
        (c) => c.centerId === selectedCenterId
      );
      if (!exists) {
        setSelectedCenterId(centersQuery.data[0].centerId);
      }
    } else {
      setSelectedCenterId(null);
    }
  }, [centersQuery.data, selectedCenterId]);

  const handleLogout = () => {
    clearAuth();
    navigate("/login");
  };

  // ───────────────────────────────────────────────
  // UI layout
  // ───────────────────────────────────────────────
  return (
    <div className="min-h-screen flex bg-slate-100">
      {/* Sidebar */}
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
        <header className="h-14 px-6 flex items-center justify-between bg-white border-b border-slate-200">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">
              Geography &amp; Centers
            </h2>
            <p className="text-xs text-slate-500">
              Drill down from counties → districts → centers → places.
            </p>
          </div>

          <button
            onClick={handleLogout}
            className="text-xs px-3 py-1.5 rounded-md border border-slate-300 text-slate-700 hover:bg-slate-100"
          >
            Logout
          </button>
        </header>

        {/* 4-column drill-down */}
        <main className="flex-1 p-6">
          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Counties */}
            <section className="bg-white rounded-xl shadow-sm border p-4">
              <h3 className="text-sm font-semibold mb-2">Counties</h3>

              {countiesQuery.isLoading && (
                <p className="text-xs text-slate-500">Loading counties…</p>
              )}
              {countiesQuery.isError && (
                <p className="text-xs text-red-600">
                  Error loading counties. Check /api/counties.
                </p>
              )}

              {countiesQuery.data?.map((county) => (
                <button
                  key={county.countyId}
                  onClick={() => setSelectedCountyId(county.countyId)}
                  className={`block w-full text-left px-3 py-2 rounded-md text-sm ${
                    county.countyId === selectedCountyId
                      ? "bg-blue-600 text-white"
                      : "hover:bg-slate-100"
                  }`}
                >
                  {county.countyName}
                </button>
              ))}
            </section>

            {/* Districts */}
            <section className="bg-white rounded-xl shadow-sm border p-4">
              <h3 className="text-sm font-semibold mb-2">Districts</h3>

              {!selectedCountyId && (
                <p className="text-xs text-slate-500">
                  Select a county to view districts.
                </p>
              )}
              {selectedCountyId && districtsQuery.isLoading && (
                <p className="text-xs text-slate-500">Loading districts…</p>
              )}
              {selectedCountyId && districtsQuery.isError && (
                <p className="text-xs text-red-600">
                  Error loading districts. Check /api/districts.
                </p>
              )}

              {districtsQuery.data?.map((d) => (
                <button
                  key={d.districtId}
                  onClick={() => setSelectedDistrictId(d.districtId)}
                  className={`block w-full text-left px-3 py-2 rounded-md text-sm ${
                    d.districtId === selectedDistrictId
                      ? "bg-blue-600 text-white"
                      : "hover:bg-slate-100"
                  }`}
                >
                  {d.districtName}
                </button>
              ))}
            </section>

            {/* Polling Centers */}
            <section className="bg-white rounded-xl shadow-sm border p-4">
              <h3 className="text-sm font-semibold mb-2">Polling Centers</h3>

              {!selectedDistrictId && (
                <p className="text-xs text-slate-500">
                  Select a district to view polling centers.
                </p>
              )}
              {selectedDistrictId && centersQuery.isLoading && (
                <p className="text-xs text-slate-500">
                  Loading polling centers…
                </p>
              )}
              {selectedDistrictId && centersQuery.isError && (
                <p className="text-xs text-red-600">
                  Error loading polling centers. Check /api/polling-centers.
                </p>
              )}

              {centersQuery.data?.map((center) => (
                <button
                  key={center.centerId}
                  onClick={() => setSelectedCenterId(center.centerId)}
                  className={`block w-full text-left px-3 py-2 rounded-md text-sm ${
                    center.centerId === selectedCenterId
                      ? "bg-blue-600 text-white"
                      : "hover:bg-slate-100"
                  }`}
                >
                  {center.centerName}
                </button>
              ))}
            </section>

            {/* Polling Places */}
            <section className="bg-white rounded-xl shadow-sm border p-4">
              <h3 className="text-sm font-semibold mb-2">Polling Places</h3>

              {!selectedCenterId && (
                <p className="text-xs text-slate-500">
                  Select a polling center to view its places.
                </p>
              )}

              {selectedCenterId && placesQuery.isLoading && (
                <p className="text-xs text-slate-500">Loading places…</p>
              )}

              {selectedCenterId && placesQuery.isError && (
                <p className="text-xs text-red-600">
                  Error loading polling places. Check /api/polling-places.
                </p>
              )}

              {selectedCenterId &&
                !placesQuery.isLoading &&
                !placesQuery.isError &&
                placesQuery.data &&
                placesQuery.data.length === 0 && (
                  <p className="text-xs text-slate-500">
                    No polling places defined for this center.
                  </p>
                )}

              {placesQuery.data?.map((place) => (
                <div
                  key={place.placeId}
                  className="border-b last:border-b-0 py-1.5 text-sm"
                >
                  {place.placeNumber} — {place.label ?? place.code}
                </div>
              ))}
            </section>
          </div>
        </main>
      </div>
    </div>
  );
};

export default GeographyPage;
