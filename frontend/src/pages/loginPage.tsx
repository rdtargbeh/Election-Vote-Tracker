// src/pages/LoginPage.tsx
// ------------------------------------------------------
// Login screen wired to backend controller:
//
// POST /api/auth/login
// Request body: { userName, password }
// Success response: { accessToken, expiresIn, defaultOrgId, defaultOrgName }
//
// UX improvement:
// - Shows exact backend message when login is blocked (403)
//   e.g. user deactivated / organization deactivated
// ------------------------------------------------------

// src/pages/LoginPage.tsx

// src/pages/LoginPage.tsx
// ------------------------------------------------------
// POST /api/auth/login
// Request: { userName, password }
// Response: { accessToken, expiresIn, defaultOrgId, defaultOrgName }
//
// Shows backend error messages clearly:
// - "Your account has been deactivated..."
// - "Organization is deactivated..."
// - "No organization assigned..."
// ------------------------------------------------------

import React, { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import axios from "axios";

import { apiClient } from "../shared/lib/apiClient";
import { useAuthStore } from "../shared/store/authStore";

type LoginResponse = {
  accessToken: string;
  expiresIn: number;
  defaultOrgId: string | null;
  defaultOrgName?: string | null;
};

const LoginPage: React.FC = () => {
  const [userName, setUserName] = useState("");
  const [password, setPassword] = useState("");

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const setToken = useAuthStore((state) => state.setToken);
  const setCurrentOrg = useAuthStore((state) => state.setCurrentOrg);
  const clearAuth = useAuthStore((state) => state.clearAuth);

  const navigate = useNavigate();

  const loginMutation = useMutation({
    mutationFn: async () => {
      setErrorMessage(null);

      const res = await apiClient.post<LoginResponse>("/auth/login", {
        userName,
        password,
      });
      return res.data;
    },

    onSuccess: (data) => {
      setErrorMessage(null);

      setToken(data.accessToken);

      if (data.defaultOrgId) {
        setCurrentOrg(data.defaultOrgId);
      }

      navigate("/dashboard");
    },

    onError: (error: unknown) => {
      clearAuth();

      // ✅ If your apiClient interceptor throws a custom object:
      // { status, code, message, details }
      const anyErr = error as any;
      if (anyErr?.message && typeof anyErr.message === "string") {
        setErrorMessage(anyErr.message);
        return;
      }

      // ✅ Normal Axios error path
      if (axios.isAxiosError(error)) {
        const data: any = error.response?.data;

        // Sometimes backend returns string body
        if (typeof data === "string" && data.trim()) {
          setErrorMessage(data);
          return;
        }

        // Common Spring error shapes:
        // { message: "..."} or { error: "..."} or { detail: "..."}
        const msg =
          data?.message ||
          data?.detail ||
          data?.error ||
          error.message ||
          "Login failed. Please try again.";

        setErrorMessage(msg);
        return;
      }

      // ✅ Last resort
      setErrorMessage("Unexpected error occurred. Please try again.");
    },
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    loginMutation.mutate();
  };

  const isLoading = loginMutation.isPending;

  return (
    <div className="min-h-screen bg-slate-100 flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-lg border border-slate-200 p-7">
        <h1 className="text-xl font-bold text-slate-900 mb-2 text-center">
          Sign in to Election Management System (EMS)
        </h1>
        <p className="text-sm text-slate-600 text-center mb-6">
          Enter your credentials to continue.
        </p>

        {errorMessage && (
          <div className="mb-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3">
            <p className="text-sm font-medium text-red-800">{errorMessage}</p>
            <p className="mt-1 text-xs text-red-700">
              If you believe this is a mistake, please contact your organization
              administrator.
            </p>
          </div>
        )}

        <form className="space-y-4" onSubmit={handleSubmit}>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">
              Username (or Email)
            </label>
            <input
              type="text"
              value={userName}
              onChange={(e) => setUserName(e.target.value)}
              className="w-full px-3.5 py-2.5 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
              placeholder="Enter username or email"
              required
              autoComplete="username"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-3.5 py-2.5 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
              placeholder="Enter password"
              required
              autoComplete="current-password"
            />
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-blue-600 text-white py-2.5 rounded-lg font-semibold hover:bg-blue-700 disabled:opacity-60 transition"
          >
            {isLoading ? "Logging in..." : "Login"}
          </button>
        </form>
      </div>
    </div>
  );
};

export default LoginPage;

// // src/pages/LoginPage.tsx
// // ------------------------------------------------------
// // Login screen wired to real backend controller:
// //
// // POST /api/auth/login
// // Request body: { userName, password }
// // Response: { accessToken, expiresIn, defaultOrgId, defaultOrgName }
// //
// // Frontend:
// // - Calls /auth/login (base URL already has /api).
// // - Stores accessToken in authStore.token.
// // - Stores defaultOrgId in authStore.currentOrgId (if present).
// // - Redirects directly to /dashboard on success.
// // ------------------------------------------------------

// import React, { useState } from "react";
// import type { FormEvent } from "react";
// import { useNavigate } from "react-router-dom";
// import { useMutation } from "@tanstack/react-query";

// import { apiClient } from "../shared/lib/apiClient";
// import { useAuthStore } from "../shared/store/authStore";

// type LoginResponse = {
//   accessToken: string;
//   expiresIn: number;
//   defaultOrgId: string | null;
//   defaultOrgName?: string;
// };

// const LoginPage: React.FC = () => {
//   const [userName, setUserName] = useState("");
//   const [password, setPassword] = useState("");

//   const setToken = useAuthStore((state) => state.setToken);
//   const setCurrentOrg = useAuthStore((state) => state.setCurrentOrg);
//   const clearAuth = useAuthStore((state) => state.clearAuth);
//   const navigate = useNavigate();

//   const loginMutation = useMutation({
//     mutationFn: async () => {
//       const res = await apiClient.post<LoginResponse>("/auth/login", {
//         userName,
//         password,
//       });
//       return res.data;
//     },
//     onSuccess: (data) => {
//       // Store JWT from backend
//       setToken(data.accessToken);

//       // Store default org if backend provided one
//       if (data.defaultOrgId) {
//         setCurrentOrg(data.defaultOrgId);
//       }

//       // Go straight to dashboard (no org selection UI)
//       navigate("/dashboard");
//     },
//     onError: () => {
//       // clear any stale auth state on failure
//       clearAuth();
//     },
//   });

//   const handleSubmit = (e: FormEvent) => {
//     e.preventDefault();
//     loginMutation.mutate();
//   };

//   const isLoading = loginMutation.isPending;

//   return (
//     <div className="min-h-screen bg-slate-100 flex items-center justify-center">
//       <div className="w-full max-w-md bg-white rounded-xl shadow-lg border border-slate-200 p-6">
//         <h1 className="text-xl font-bold text-slate-900 mb-4 text-center">
//           Sign in to Election Management System (EMS)
//         </h1>

//         <form className="space-y-4" onSubmit={handleSubmit}>
//           <div>
//             <label className="block text-sm font-medium text-slate-700">
//               Username
//             </label>
//             <input
//               type="text"
//               value={userName}
//               onChange={(e) => setUserName(e.target.value)}
//               className="w-full mt-1 px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
//               placeholder="Enter username (or email if your auth supports it)"
//               required
//             />
//           </div>

//           <div>
//             <label className="block text-sm font-medium text-slate-700">
//               Password
//             </label>
//             <input
//               type="password"
//               value={password}
//               onChange={(e) => setPassword(e.target.value)}
//               className="w-full mt-1 px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
//               placeholder="Enter password"
//               required
//             />
//           </div>

//           {loginMutation.isError && (
//             <p className="text-sm text-red-600">
//               Login failed. Please check your credentials or backend connection.
//             </p>
//           )}

//           <button
//             type="submit"
//             disabled={isLoading}
//             className="w-full bg-blue-600 text-white py-2 rounded-md font-semibold hover:bg-blue-700 disabled:opacity-60 transition"
//           >
//             {isLoading ? "Logging in..." : "Login"}
//           </button>
//         </form>
//       </div>
//     </div>
//   );
// };

// export default LoginPage;
