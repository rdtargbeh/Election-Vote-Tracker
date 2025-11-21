// src/pages/LoginPage.tsx
// ------------------------------------------------------
// Login screen wired to:
// - call backend via apiClient
// - store { user, token } in authStore
// - redirect to /select-org on success
//
// Backend assumptions (adjust as needed):
// POST /auth/login  ->  { token, user }
// ------------------------------------------------------

import React, { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";

import { apiClient } from "../shared/lib/apiClient";
import { useAuthStore, type AuthUser } from "../shared/store/authStore";


type LoginResponse = {
  token: string;
  user: AuthUser;
};

const LoginPage: React.FC = () => {
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");

  const setAuth = useAuthStore((state) => state.setAuth);
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const navigate = useNavigate();

  const loginMutation = useMutation({
    mutationFn: async () => {
      const res = await apiClient.post<LoginResponse>("/auth/login", {
        identifier,
        password,
      });
      return res.data;
    },
    onSuccess: (data) => {
      setAuth({ user: data.user, token: data.token });
      navigate("/select-org");
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
              Email or Username
            </label>
            <input
              type="text"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              className="w-full mt-1 px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="Enter email or username"
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
