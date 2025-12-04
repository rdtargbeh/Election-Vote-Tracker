// src/pages/LoginPage.tsx
// ------------------------------------------------------
// Login screen wired to real backend controller:
//
// POST /api/auth/login
// Request body: { userName, password }
// Response: { accessToken, expiresIn, defaultOrgId, defaultOrgName }
//
// Frontend:
// - Calls /auth/login (base URL already has /api).
// - Stores accessToken in authStore.token.
// - Stores defaultOrgId in authStore.currentOrgId (if present).
// - Redirects directly to /dashboard on success.
// ------------------------------------------------------

import React, { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";

import { apiClient } from "../shared/lib/apiClient";
import { useAuthStore } from "../shared/store/authStore";

type LoginResponse = {
  accessToken: string;
  expiresIn: number;
  defaultOrgId: string | null;
  defaultOrgName?: string;
};

const LoginPage: React.FC = () => {
  const [userName, setUserName] = useState("");
  const [password, setPassword] = useState("");

  const setToken = useAuthStore((state) => state.setToken);
  const setCurrentOrg = useAuthStore((state) => state.setCurrentOrg);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const navigate = useNavigate();

  const loginMutation = useMutation({
    mutationFn: async () => {
      const res = await apiClient.post<LoginResponse>("/auth/login", {
        userName,
        password,
      });
      return res.data;
    },
    onSuccess: (data) => {
      // Store JWT from backend
      setToken(data.accessToken);

      // Store default org if backend provided one
      if (data.defaultOrgId) {
        setCurrentOrg(data.defaultOrgId);
      }

      // Go straight to dashboard (no org selection UI)
      navigate("/dashboard");
    },
    onError: () => {
      // clear any stale auth state on failure
      clearAuth();
    },
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    loginMutation.mutate();
  };

  const isLoading = loginMutation.isPending;

  return (
    <div className="min-h-screen bg-slate-100 flex items-center justify-center">
      <div className="w-full max-w-md bg-white rounded-xl shadow-lg border border-slate-200 p-6">
        <h1 className="text-xl font-bold text-slate-900 mb-4 text-center">
          Sign in to Election Vote Tracker
        </h1>

        <form className="space-y-4" onSubmit={handleSubmit}>
          <div>
            <label className="block text-sm font-medium text-slate-700">
              Username
            </label>
            <input
              type="text"
              value={userName}
              onChange={(e) => setUserName(e.target.value)}
              className="w-full mt-1 px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="Enter username (or email if your auth supports it)"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700">
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full mt-1 px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="Enter password"
              required
            />
          </div>

          {loginMutation.isError && (
            <p className="text-sm text-red-600">
              Login failed. Please check your credentials or backend connection.
            </p>
          )}

          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-blue-600 text-white py-2 rounded-md font-semibold hover:bg-blue-700 disabled:opacity-60 transition"
          >
            {isLoading ? "Logging in..." : "Login"}
          </button>
        </form>
      </div>
    </div>
  );
};

export default LoginPage;
