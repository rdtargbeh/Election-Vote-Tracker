import React from "react";
import Sidebar from "./Sidebar";
import TopBar from "./TopBar";

interface AppShellProps {
  children: React.ReactNode;
}

/**
 * AppShell
 * - Restores the original desktop layout: persistent Sidebar on the left and TopBar above content.
 * - Keeps the exact Tailwind classes you were using so the visual layout and spacing remain unchanged.
 * - Lightweight and safe to drop in as a replacement for your current AppShell.
 */
const AppShell: React.FC<AppShellProps> = ({ children }) => {
  return (
    <div className="min-h-screen flex bg-slate-100">
      {/* Sidebar */}
      <Sidebar />

      {/* Main content area */}
      <div className="flex-1 flex flex-col">
        <TopBar />
        <main
          role="main"
          className="flex-1 p-6 space-y-6 overflow-y-auto"
          aria-live="polite"
        >
          {children}
        </main>
      </div>
    </div>
  );
};

export default AppShell;
