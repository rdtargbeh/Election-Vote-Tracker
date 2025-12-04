/**
 * src/shared/lib/apiClient.ts
 *
 * Central Axios instance for the Election Vote Tracker frontend.
 * Fixes TypeScript typing error in request interceptor by using
 * InternalAxiosRequestConfig and casting headers safely.
 *
 * Responsibilities:
 *  - Base URL and default timeout
 *  - Attach Authorization + X-Org-Id headers from authStore
 *  - Normalize Axios errors into ApiError
 *  - Support passing AbortSignal via Axios `signal` in request config
 */

import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from "axios";
import { useAuthStore } from "../store/authStore";
import type { ApiError } from "../types/api";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30_000,
  // withCredentials: true, // enable if your backend uses cookies
});

/**
 * Normalize various Axios / network errors into a small, typed ApiError.
 */
function normalizeAxiosError(err: unknown): ApiError {
  const fallback: ApiError = { message: "An unknown network error occurred" };

  if (!axios.isAxiosError(err)) {
    return {
      message: (err && (err as any).message) || String(err),
    };
  }

  const aerr = err as AxiosError;
  if (
    aerr.code === "ERR_CANCELED" ||
    aerr.message?.toLowerCase().includes("canceled")
  ) {
    return { message: "Request cancelled" };
  }

  const status = aerr.response?.status;
  const respData = aerr.response?.data;

  if (respData && typeof respData === "object") {
    const anyData = respData as any;
    const apiErr: ApiError = {
      status,
      code: anyData.code ?? anyData.errorCode ?? undefined,
      message:
        anyData.message ??
        anyData.error ??
        (aerr.message || "Server returned an error"),
      details: anyData.details ?? anyData.fieldErrors ?? undefined,
    };
    return apiErr;
  }

  if (typeof respData === "string") {
    return { status, message: respData };
  }

  return { status, message: aerr.message || "Network or server error" };
}

/**
 * Request interceptor: attach Authorization + X-Org-Id headers automatically.
 * Use InternalAxiosRequestConfig to satisfy axios typings and cast headers where necessary.
 */
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Use a plain object for headers, then assign to config.headers with a cast.
    const headers: Record<string, string> = (config.headers as any) ?? {};

    try {
      const { token, currentOrgId } = useAuthStore.getState();

      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }

      if (currentOrgId) {
        headers["X-Org-Id"] = currentOrgId;
      }
    } catch (e) {
      // Accessing the store should be safe; if it fails, proceed without headers.
      // Do not block the request for store read errors.
      // eslint-disable-next-line no-console
      console.warn("apiClient: failed to read authStore", e);
    }

    // Assign back to config.headers. Cast to any because Axios types are strict.
    (config.headers as any) = headers;
    return config;
  },
  (err) => {
    return Promise.reject(normalizeAxiosError(err));
  }
);

/**
 * Response interceptor: pass through on success; normalize errors on failure.
 */
apiClient.interceptors.response.use(
  (resp) => resp,
  (err) => {
    return Promise.reject(normalizeAxiosError(err));
  }
);

/**
 * Convenience getter which supports passing an AbortSignal in config.
 * Hooks can call apiClient.get('/path', { signal }) directly as well.
 */
export function getWithSignal<T = any>(
  url: string,
  config?: AxiosRequestConfig & { signal?: AbortSignal }
) {
  return apiClient.get<T>(url, config);
}

export type { AxiosRequestConfig, AxiosError };

// // src/shared/lib/apiClient.ts
// // ------------------------------------------------------------------
// // Central Axios instance for the Election Vote Tracker frontend.
// //
// // Responsibilities:
// // - Set the base URL for the backend API.
// // - Attach Authorization header when a token exists.
// // - Attach X-Org-Id header when an org is selected.
// // - Provide a single reusable client for all features.
// // ------------------------------------------------------------------

// import axios, { type AxiosRequestHeaders } from "axios";
// import { useAuthStore } from "../store/authStore";

// // You can override this in a .env file later: VITE_API_BASE_URL
// const API_BASE_URL =
//   import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";

// export const apiClient = axios.create({
//   baseURL: API_BASE_URL,
//   // withCredentials: true, // enable if your backend uses cookies
// });

// // Attach token + org ID to every request
// apiClient.interceptors.request.use((config) => {
//   // Ensure headers is a proper AxiosRequestHeaders object
//   const headers: AxiosRequestHeaders = (config.headers ??
//     {}) as AxiosRequestHeaders;

//   const { token, currentOrgId } = useAuthStore.getState();

//   if (token) {
//     headers.Authorization = `Bearer ${token}`;
//   }

//   if (currentOrgId) {
//     headers["X-Org-Id"] = currentOrgId;
//   }

//   config.headers = headers;
//   return config;
// });
