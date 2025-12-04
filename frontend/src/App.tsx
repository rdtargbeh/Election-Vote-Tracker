// src/App.tsx
import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";

import { RequireAuth, RequireOrg } from "./shared/routes/RequireGuards";
import AppShell from "./shared/layout/AppShell";

import LoginPage from "./pages/loginPage";
import SelectOrgPage from "./pages/SelectOrgPage";
import DashboardPage from "./pages/DashboardPage";
import GeographyPage from "./pages/GeographyPage";
import HealthCheckPage from "./pages/HealthCheckPage";
import VoteSubmissionsPage from "./pages/VoteSubmissionsPage";
import ObserverReportsPage from "./pages/ObserverReportsPage";
import ResultsOverviewPage from "./pages/ResultsOverviewPage";
import CreateSubmissionForm from "./pages/CreateSubmissionForm";

const App: React.FC = () => {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/select-org" element={<SelectOrgPage />} />
      <Route path="/health-test" element={<HealthCheckPage />} />

      {/* Redirect root to dashboard */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />

      {/* Protected + shell-wrapped routes */}
      <Route
        path="/dashboard"
        element={
          <RequireAuth>
            <RequireOrg>
              <AppShell>
                <DashboardPage />
              </AppShell>
            </RequireOrg>
          </RequireAuth>
        }
      />

      <Route
        path="/geography"
        element={
          <RequireAuth>
            <RequireOrg>
              <AppShell>
                <GeographyPage />
              </AppShell>
            </RequireOrg>
          </RequireAuth>
        }
      />

      <Route
        path="/vote-submissions"
        element={
          <RequireAuth>
            <RequireOrg>
              <AppShell>
                <VoteSubmissionsPage />
              </AppShell>
            </RequireOrg>
          </RequireAuth>
        }
      />

      <Route
        path="/observer-reports"
        element={
          <RequireAuth>
            <RequireOrg>
              <AppShell>
                <ObserverReportsPage />
              </AppShell>
            </RequireOrg>
          </RequireAuth>
        }
      />

      <Route
        path="/results"
        element={
          <RequireAuth>
            <RequireOrg>
              <AppShell>
                <ResultsOverviewPage />
              </AppShell>
            </RequireOrg>
          </RequireAuth>
        }
      />

      {/* Create submission route */}
      <Route
        path="/vote-submissions/create"
        element={
          <RequireAuth>
            <RequireOrg>
              <AppShell>
                <CreateSubmissionForm />
              </AppShell>
            </RequireOrg>
          </RequireAuth>
        }
      />

      {/* convenience redirect for legacy path */}
      <Route
        path="/create-submission"
        element={<Navigate to="/vote-submissions/create" replace />}
      />

      {/* 404 fallback */}
      <Route path="*" element={<div>Not found</div>} />
    </Routes>
  );
};

export default App;
