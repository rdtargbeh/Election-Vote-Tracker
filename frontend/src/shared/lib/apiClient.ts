
// src/shared/lib/apiClient.ts
// ------------------------------------------------------------------
// Central Axios instance for the Election Vote Tracker frontend.
//
// Responsibilities:
// - Set the base URL for the backend API.
// - Attach Authorization header when a token exists.
// - Attach X-Org-Id header when an org is selected.
// - Provide a single reusable client for all features.
// ------------------------------------------------------------------

import axios, { type AxiosRequestHeaders } from "axios";
import { useAuthStore } from "../store/authStore";

// You can override this in a .env file later: VITE_API_BASE_URL
const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  // withCredentials: true, // enable if your backend uses cookies
});

// Attach token + org ID to every request
apiClient.interceptors.request.use((config) => {
  // Ensure headers is a proper AxiosRequestHeaders object
  const headers: AxiosRequestHeaders = (config.headers ??
    {}) as AxiosRequestHeaders;

  const { token, currentOrgId } = useAuthStore.getState();

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  if (currentOrgId) {
    headers["X-Org-Id"] = currentOrgId;
  }

  config.headers = headers;
  return config;
});
