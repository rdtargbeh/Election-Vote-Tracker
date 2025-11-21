
// src/App.tsx
// ------------------------------------------------------
// Application route container for the Election Vote Tracker.
//
// For now, we only define the root ("/") route that shows
// the base shell we already built.
//
// Later we will add routes like:
// - /login
// - /select-org
// - /org/:orgId/dashboard
// - /org/:orgId/elections
// etc.
//
// React Router's <BrowserRouter> is defined in main.tsx;
// here we only use <Routes> and <Route>.
// ------------------------------------------------------

import React from "react";
import { Routes, Route } from "react-router-dom";
import LoginPage from "./pages/loginPage";
import SelectOrgPage from "./pages/SelectOrgPage";
import DashboardPage from "./pages/DashboardPage";
import HealthCheckPage from "./pages/HealthCheckPage";



// Temporary landing page component
const LandingPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-slate-100 flex items-center justify-center">
      <div className="w-full max-w-xl bg-white rounded-2xl shadow-lg border border-slate-200 p-8">
        <h1 className="text-2xl font-bold text-slate-900 mb-3">
          Election Vote Tracker – Frontend
        </h1>

        <p className="text-sm text-slate-600 mb-4">
          Tailwind CSS is working. This shell will host:
        </p>

        <ul className="list-disc list-inside text-sm text-slate-700 space-y-1">
          <li>Authentication &amp; organization selection</li>
          <li>Role-based dashboards (SYSTEM_ADMIN, ADMIN, PARTY_ADMIN, etc.)</li>
          <li>Election, geography, and vote submission flows</li>
          <li>Observer reports, incidents, analytics, and notifications</li>
        </ul>

        <p className="text-xs text-slate-400 mt-6">
          Setup phase: base layout only. Next we&apos;ll add routing and
          providers, one step at a time.
        </p>
      </div>
    </div>
  );
};

const App: React.FC = () => {
  return (
    <Routes>
      {/* Root route – initial landing */}
      <Route path="/" element={<LandingPage />} />
      {/* Future routes will be added here */}
     <Route path="/login" element={<LoginPage />} />
     <Route path="/select-org" element={<SelectOrgPage />} />
     <Route path="/dashboard" element={<DashboardPage />} />
     <Route path="/health-test" element={<HealthCheckPage />} />

    </Routes>
  );
};

export default App;
