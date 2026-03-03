// // src/shared/services/fileUploadService.ts
// // Helper: request presigned URLs from the backend, upload files to those URLs, return storage keys.

// import { apiClient } from "../lib/apiClient";

// export interface PresignRequestItem {
//   fileName: string;
//   contentType: string;
//   contentLength: number;
// }

// export interface PresignResponseItem {
//   key: string;
//   url: string;
//   contentType: string;
//   contentLength: number;
// }

// export async function getPresignedUrls(
//   items: PresignRequestItem[]
// ): Promise<PresignResponseItem[]> {
//   const res = await apiClient.post<{ files: PresignResponseItem[] }>(
//     "/vote-submissions/presign",
//     { files: items }
//   );
//   return res.data.files;
// }

// /** Upload a file via PUT to the given presigned URL. */
// export async function uploadFileToUrl(
//   url: string,
//   file: File,
//   contentType?: string
// ): Promise<void> {
//   // Use fetch here; axios may strip/pre-flight headers unpredictably for presigned PUTs
//   const res = await fetch(url, {
//     method: "PUT",
//     headers: {
//       "Content-Type": contentType ?? (file.type || "application/octet-stream"),
//       // Do NOT send Authorization header; presigned URL includes auth
//     },
//     body: file,
//   });
//   if (!res.ok) {
//     throw new Error(`Upload failed: ${res.status} ${res.statusText}`);
//   }
// }

// /** Helper: take File[] and request presigned URLs then upload and return array of storage keys */
// export async function uploadFilesDirect(files: File[]): Promise<string[]> {
//   if (!files || files.length === 0) return [];
//   const items: PresignRequestItem[] = Array.from(files).map((f) => ({
//     fileName: f.name,
//     contentType: f.type || "application/octet-stream",
//     contentLength: f.size,
//   }));

//   const presigned = await getPresignedUrls(items);

//   // Upload in sequence or in parallel with concurrency control; here we do parallel
//   await Promise.all(
//     presigned.map((p, idx) => uploadFileToUrl(p.url, files[idx], p.contentType))
//   );

//   // Return storage keys so caller can include them in submission payload
//   return presigned.map((p) => p.key);
// }
