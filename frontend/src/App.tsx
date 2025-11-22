

// src/App.tsx
// ------------------------------------------------------
// Main application routes for the Election Vote Tracker.
//
// Routing behavior:
// - "/"             = LandingPage (public)
// - "/login"        = LoginPage (public)
// - "/select-org"   = Requires login (auth only)
// - "/dashboard"    = Requires login + org context
// - "/geography"    = Requires login + org context
// - "/health-test"  = Public (optional)
// ------------------------------------------------------

import React from "react";
import { Routes, Route } from "react-router-dom";

import LoginPage from "./pages/loginPage";
import SelectOrgPage from "./pages/SelectOrgPage";
import DashboardPage from "./pages/DashboardPage";
import HealthCheckPage from "./pages/HealthCheckPage";
import GeographyPage from "./pages/GeographyPage";

import { RequireAuth, RequireOrg } from "./shared/routes/RequireGuards";

// Temporary landing page
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
      {/* Public */}
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/health-test" element={<HealthCheckPage />} />

      {/* Auth-only route (no org needed) */}
      <Route
        path="/select-org"
        element={
          <RequireAuth>
            <SelectOrgPage />
          </RequireAuth>
        }
      />

      {/* Full protection: must be logged in AND have org */}
      <Route
        path="/dashboard"
        element={
          <RequireAuth>
            <RequireOrg>
              <DashboardPage />
            </RequireOrg>
          </RequireAuth>
        }
      />

      <Route
        path="/geography"
        element={
          <RequireAuth>
            <RequireOrg>
              <GeographyPage />
            </RequireOrg>
          </RequireAuth>
        }
      />

      {/* Fallback */}
      <Route path="*" element={<LandingPage />} />
    </Routes>
  );
};

export default App;




// // src/App.tsx
// // ------------------------------------------------------
// // Main application routes for the Election Vote Tracker.
// //
// // Routing behavior:
// // - "/"             = LandingPage (public)
// // - "/login"        = LoginPage (public)
// // - "/select-org"   = Requires login (auth only)
// // - "/dashboard"    = Requires login + org context
// // - "/health-test"  = Public (optional)
// // ------------------------------------------------------

// import React from "react";
// import { Routes, Route } from "react-router-dom";

// import LoginPage from "./pages/loginPage";
// import SelectOrgPage from "./pages/SelectOrgPage";
// import DashboardPage from "./pages/DashboardPage";
// import HealthCheckPage from "./pages/HealthCheckPage";
// import GeographyPage from "./pages/GeographyPage";


// import { RequireAuth, RequireOrg } from "./shared/routes/RequireGuards";

// // Temporary landing page
// const LandingPage: React.FC = () => {
//   return (
//     <div className="min-h-screen bg-slate-100 flex items-center justify-center">
//       <div className="w-full max-w-xl bg-white rounded-2xl shadow-lg border border-slate-200 p-8">
//         <h1 className="text-2xl font-bold text-slate-900 mb-3">
//           Election Vote Tracker – Frontend
//         </h1>

//         <p className="text-sm text-slate-600 mb-4">
//           Tailwind CSS is working. This shell will host:
//         </p>

//         <ul className="list-disc list-inside text-sm text-slate-700 space-y-1">
//           <li>Authentication &amp; organization selection</li>
//           <li>Role-based dashboards (SYSTEM_ADMIN, ADMIN, PARTY_ADMIN, etc.)</li>
//           <li>Election, geography, and vote submission flows</li>
//           <li>Observer reports, incidents, analytics, and notifications</li>
//         </ul>

//         <p className="text-xs text-slate-400 mt-6">
//           Setup phase: base layout only. Next we&apos;ll add routing and
//           providers, one step at a time.
//         </p>
//       </div>
//     </div>
//   );
// };

// const App: React.FC = () => {
//   return (
//     <Routes>
//       {/* Public */}
//       <Route path="/" element={<LandingPage />} />
//       <Route path="/login" element={<LoginPage />} />
//       <Route path="/health-test" element={<HealthCheckPage />} />

//       {/* Auth-only route (no org needed) */}
//       <Route
//         path="/select-org"
//         element={
//           <RequireAuth>
//             <SelectOrgPage />
//           </RequireAuth>
//         }
//       />

//       {/* Full protection: must be logged in AND have org */}
//       <Route
//         path="/dashboard"
//         element={
//           <RequireAuth>
//             <RequireOrg>
//               <DashboardPage />
//             </RequireOrg>
//           </RequireAuth>
//         }
//       />

//       {/* Fallback */}
//       <Route path="*" element={<LandingPage />} />
//     </Routes>
//   );
// };

// export default App;
