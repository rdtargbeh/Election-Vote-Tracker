
// src/pages/HealthCheckPage.tsx

import { useHealthCheck } from "../shared/api/useHealthCheck";

export default function HealthCheckPage() {
  const { data, isLoading, error } = useHealthCheck();

  if (isLoading) return <p className="text-white">Checking API...</p>;
  if (error)
    return (
      <p className="text-red-400">❌ API error: {(error as any).message}</p>
    );

  return (
    <div className="text-white p-4">
      <h1 className="text-2xl font-semibold">API Health Check</h1>
      <pre className="mt-4 bg-black/40 p-4 rounded-lg">
        {JSON.stringify(data, null, 2)}
      </pre>
    </div>
  );
}
