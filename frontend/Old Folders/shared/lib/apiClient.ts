// /**
//  * src/shared/lib/apiClient.ts
//  *
//  * Production-stable Axios client:
//  *  - attaches Authorization header if token exists
//  *  - attaches X-Org-Id header only if currentOrgId exists (system admin may not have org)
//  *  - normalizes errors to ApiError
//  *  - OPTIONAL: auto-clear auth on 401 (disabled by default here)
//  */

// import axios, {
//   type AxiosError,
//   type AxiosRequestConfig,
//   type InternalAxiosRequestConfig,
// } from "axios";
// import { useAuthStore } from "../../../shared/lib/store/authStore";
// import type { ApiError } from "../types/api";

// const API_BASE_URL =
//   import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";

// export const apiClient = axios.create({
//   baseURL: API_BASE_URL,
//   timeout: 30_000,
//   // withCredentials: true,
// });

// function normalizeAxiosError(err: unknown): ApiError {
//   if (!axios.isAxiosError(err)) {
//     return { message: (err as any)?.message ?? String(err) };
//   }

//   const aerr = err as AxiosError;
//   const status = aerr.response?.status;
//   const respData = aerr.response?.data;

//   // canceled requests
//   if (
//     aerr.code === "ERR_CANCELED" ||
//     aerr.message?.toLowerCase().includes("canceled")
//   ) {
//     return { message: "Request cancelled" };
//   }

//   if (respData && typeof respData === "object") {
//     const anyData = respData as any;
//     return {
//       status,
//       code: anyData.code ?? anyData.errorCode ?? undefined,
//       message:
//         anyData.message ??
//         anyData.error ??
//         aerr.message ??
//         "Server returned an error",
//       details: anyData.details ?? anyData.fieldErrors ?? undefined,
//     };
//   }

//   if (typeof respData === "string") {
//     return { status, message: respData };
//   }

//   return { status, message: aerr.message || "Network or server error" };
// }

// apiClient.interceptors.request.use(
//   (config: InternalAxiosRequestConfig) => {
//     // IMPORTANT: do not reject here; just attach headers
//     const headers: Record<string, string> = {
//       ...(config.headers as any),
//     };

//     try {
//       const { token, currentOrgId } = useAuthStore.getState();

//       if (token) headers.Authorization = `Bearer ${token}`;

//       // ✅ tenant header only if present (SYSTEM_ADMIN may not have orgId)
//       if (currentOrgId) headers["X-Org-Id"] = currentOrgId;
//     } catch (e) {
//       console.warn("apiClient: failed to read authStore", e);
//     }

//     (config.headers as any) = headers;
//     return config;
//   },
//   (err) => Promise.reject(normalizeAxiosError(err))
// );

// // src/shared/lib/apiClient.ts
// // ...keep everything else exactly the same...

// apiClient.interceptors.response.use(
//   (resp) => resp,
//   (err) => {
//     const apiErr = normalizeAxiosError(err);

//     // ✅ 401 handling (session expired / invalid token) => clear auth + go login
//     if (apiErr.status === 401) {
//       try {
//         // ✅ use the store method that actually exists
//         useAuthStore.getState().clearAuth();
//       } catch {}

//       if (window.location.pathname !== "/login") {
//         window.location.href = "/login";
//       }
//     }

//     return Promise.reject(apiErr);
//   }
// );

// export function getWithSignal<T = any>(
//   url: string,
//   config?: AxiosRequestConfig & { signal?: AbortSignal }
// ) {
//   return apiClient.get<T>(url, config);
// }

// export type { AxiosRequestConfig, AxiosError };
