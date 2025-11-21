

// src/main.tsx
// -----------------------------------------------------------
// Root entry for the Election Vote Tracker frontend.
//
// This file sets up global providers used throughout the app:
// - React Router (routing foundation)
// - React Query (server state management & caching)
// - React Query DevTools (debugging in development)
//
// Actual routes will be created in the next steps.
// -----------------------------------------------------------

import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";

import App from "./App";
import "./index.css";

// Create a single React Query client instance
const queryClient = new QueryClient();

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    {/* Global state for caching backend calls */}
    <QueryClientProvider client={queryClient}>
      {/* App-level router wrapper */}
      <BrowserRouter>
        <App />
      </BrowserRouter>

      {/* Only visible during development */}
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  </React.StrictMode>
);
