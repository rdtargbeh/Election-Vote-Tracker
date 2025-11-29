// src/shared/layout/AppShell.tsx
import React from "react";
import Sidebar from "./Sidebar";
import TopBar from "./TopBar";

interface AppShellProps {
  children: React.ReactNode;
}

const AppShell: React.FC<AppShellProps> = ({ children }) => {
  return (
    <div className="min-h-screen flex bg-slate-100">
      {/* Sidebar */}
      <Sidebar />

      {/* Main content area */}
      <div className="flex-1 flex flex-col">
        <TopBar />
        <main className="flex-1 p-6 space-y-6 overflow-y-auto">{children}</main>
      </div>
    </div>
  );
};

export default AppShell;
